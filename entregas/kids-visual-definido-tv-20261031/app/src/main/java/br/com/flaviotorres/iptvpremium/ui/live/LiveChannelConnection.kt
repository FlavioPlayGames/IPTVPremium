package br.com.flaviotorres.iptvpremium.ui.live

import android.net.Uri
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import br.com.flaviotorres.iptvpremium.domain.model.Channel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel as Events
import kotlinx.coroutines.flow.*
import java.util.Locale

/** The logical channel always belongs to the selected list; only the working transport may change. */
data class LiveStreamCandidate(val playlistId:String,val channel:Channel)
internal data class LiveConnectionState(val buffering:Boolean=false,val searching:Boolean=false,
    val unavailable:Boolean=false,val playing:Boolean=false,val source:LiveStreamCandidate?=null)

/** All Media3 operations stay on Main; cancellation tears down the old attempt before the next request. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class LiveChannelConnection(private val player:ExoPlayer) {
    private val timeoutNanos=7_000_000_000L
    private val mutableState=MutableStateFlow(LiveConnectionState())
    val state=mutableState.asStateFlow()
    private enum class Event { BUFFERING, READY, FAILED }

    suspend fun connect(origin:LiveStreamCandidate,alternatives:()->Flow<LiveStreamCandidate>,onPlaying:()->Unit) {
        val visited=hashSetOf<String>()
        var searching=false
        suspend fun attempt(source:LiveStreamCandidate) {
            // Same URL may be registered under several IDs/categories/lists; never retry it in this search.
            if(!visited.add(source.channel.streamUrl.trim()))return
            if(source.channel.streamUrl.isBlank())return
            mutableState.value=LiveConnectionState(buffering=true,searching=searching,source=source)
            val releasePriority=br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.acquire()
            val startedAt=android.os.SystemClock.elapsedRealtime()
            fun trace(phase:String) {
                android.util.Log.i("LiveLoadPerf", "phase=$phase elapsedMs=${android.os.SystemClock.elapsedRealtime()-startedAt} bufferedMs=${player.totalBufferedDuration} positionMs=${player.currentPosition} isPlaying=${player.isPlaying}")
            }
            val events=Events<Event>(Events.UNLIMITED)
            val listener=object:Player.Listener {
                override fun onRenderedFirstFrame(){trace("first_frame")}
                override fun onPlaybackStateChanged(state:Int) {
                    trace("state_$state")
                    if(state==Player.STATE_BUFFERING)events.trySend(Event.BUFFERING)
                    if(state==Player.STATE_ENDED)events.trySend(Event.FAILED)
                }
                override fun onIsPlayingChanged(isPlaying:Boolean) {if(isPlaying)events.trySend(Event.READY)}
                override fun onPlayerError(error:PlaybackException) {events.trySend(Event.FAILED)}
            }
            var samples:Job?=null
            try {
                trace("prepare")
                player.stop();player.clearMediaItems()
                player.addListener(listener)
                val url=source.channel.streamUrl.trim()
                val path=Uri.parse(url).path.orEmpty().lowercase(Locale.ROOT)
                val item=MediaItem.Builder().setMediaId(source.playlistId+":"+source.channel.id).setUri(url).apply {
                    when {path.endsWith(".m3u8")->setMimeType(MimeTypes.APPLICATION_M3U8)
                        path.endsWith(".mpd")->setMimeType(MimeTypes.APPLICATION_MPD)}
                }.build()
                var deadline=System.nanoTime()+timeoutNanos
                player.setMediaItem(item);player.prepare();player.play()
                var started=false
                var buffering=true
                var announced=false
                while(currentCoroutineContext().isActive) {
                    val event=if(buffering||!started) {
                        val remaining=deadline-System.nanoTime()
                        if(remaining<=0L)null else withTimeoutOrNull((remaining/1_000_000L).coerceAtLeast(1L)){events.receive()}
                    }else events.receive()
                    when(event) {
                        null,Event.FAILED->return
                        Event.BUFFERING->{
                            if(!buffering)deadline=System.nanoTime()+timeoutNanos
                            buffering=true;mutableState.value=mutableState.value.copy(buffering=true)
                        }
                        Event.READY->{
                            buffering=false;started=true
                            mutableState.value=LiveConnectionState(playing=true,source=source)
                            // Background downloads remain deferred during the first two seconds of playback.
                            // Video is not delayed; only this startup lease is released, never another owner.
                            trace("playing")
                            if(!announced){
                                announced=true;onPlaying()
                                samples=CoroutineScope(currentCoroutineContext()).launch {
                                    repeat(6){sample ->
                                        delay(500)
                                        if(sample==3)releasePriority()
                                        trace("startup_sample")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){/* Never log provider URLs or authentication errors. */}
            finally {samples?.cancel();releasePriority();player.removeListener(listener);events.close()}
        }
        try {
            attempt(origin)
            // A successful attempt stays suspended until it fails or the selection is cancelled.
            searching=true
            mutableState.value=LiveConnectionState(searching=true,buffering=true)
            alternatives().collect {candidate->attempt(candidate)}
            player.stop();player.clearMediaItems()
            mutableState.value=LiveConnectionState(unavailable=true)
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){player.stop();mutableState.value=LiveConnectionState(unavailable=true)}
        finally {if(!currentCoroutineContext().isActive){runCatching{player.stop();player.clearMediaItems()};mutableState.value=LiveConnectionState()}}
    }
}
