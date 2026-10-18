package br.com.flaviotorres.iptvpremium.core.player
import android.content.Context
import android.view.SurfaceHolder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
open class FakeAdapter(override val engine:PlaybackEngine):PlaybackEngineAdapter {
 override val state=MutableStateFlow(PlaybackUiState(engine=engine))
 override val capabilities=MutableStateFlow(PlaybackEngineCapabilities())
 override val events=MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity=10)
 var released=false;var opened:PlaybackOpenRequest?=null
 override fun prepare(request:PlaybackOpenRequest):PlaybackCommandResult {opened=request;state.value=PlaybackUiState(media=request.media,engine=engine,status=PlaybackStatus.READY,positionMs=request.startPositionMs);return PlaybackCommandResult.ACCEPTED}
 override fun release(){released=true}
 override fun attachVideoSurface(holder:SurfaceHolder)=PlaybackCommandResult.ACCEPTED
 override fun detachVideoSurface(holder:SurfaceHolder?){}
 override fun play()=PlaybackCommandResult.ACCEPTED
 override fun pause()=PlaybackCommandResult.ACCEPTED
 override fun stop(clearMedia:Boolean)=PlaybackCommandResult.ACCEPTED
 override fun seekTo(positionMs:Long)=PlaybackCommandResult.ACCEPTED
 override fun seekBy(deltaMs:Long)=PlaybackCommandResult.ACCEPTED
 override fun goToLiveEdge()=PlaybackCommandResult.ACCEPTED
 override fun selectTrack(type:PlaybackTrackType,trackId:String?)=PlaybackCommandResult.ACCEPTED
 override fun selectVideoVariant(variantId:String?)=PlaybackCommandResult.ACCEPTED
 override fun setPlaybackSpeed(speed:Float)=PlaybackCommandResult.ACCEPTED
 override fun setVolume(volume:Float)=PlaybackCommandResult.ACCEPTED
 override fun setVideoLayout(layout:PlaybackVideoLayout)=PlaybackCommandResult.ACCEPTED
 override fun setSubtitleStyle(style:PlaybackSubtitleStyle)=PlaybackCommandResult.ACCEPTED
}
class Media3PlaybackAdapter(context:Context):FakeAdapter(PlaybackEngine.MEDIA3)
class LibVlcPlaybackAdapter(context:Context):FakeAdapter(PlaybackEngine.VLC)
@OptIn(ExperimentalCoroutinesApi::class)
fun main()=runTest {
 Dispatchers.setMain(StandardTestDispatcher(testScheduler))
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val media=PlaybackMedia("1",PlaybackContentType.MOVIE,"Teste","https://example.invalid/video")
 fun fixture():Pair<PlaybackController,MutableList<FakeAdapter>> {
  val created=mutableListOf<FakeAdapter>()
  val c=PlaybackController(Context(),factory=PlaybackEngineAdapterFactory{e->check(created.lastOrNull()?.released!=false);FakeAdapter(e).also{created.add(it)}})
  c.open(PlaybackOpenRequest(media,startPositionMs=120000));return c to created
 }
 try {
  val(c,list)=fixture();runCurrent()
  list[0].events.emit(PlaybackEngineEvent.Failure(PlaybackFailure(PlaybackFailureType.DECODER,"erro",true)))
  runCurrent();advanceTimeBy(300);runCurrent()
  pass("evento de erro sem estado ERROR aciona motor alternativo",list.size==2 && list[1].engine==PlaybackEngine.VLC)
  pass("troca preserva posicao e libera motor anterior",list[1].opened?.startPositionMs==120000L && list[0].released)
  val failure=PlaybackFailure(PlaybackFailureType.DECODER,"erro",true)
  list[1].state.value=list[1].state.value.copy(status=PlaybackStatus.ERROR,failure=failure);list[1].events.emit(PlaybackEngineEvent.Failure(failure));runCurrent();advanceTimeBy(2000);runCurrent()
  pass("falha nos dois motores encerra sem loop",list.size==2 && c.state.value.status==PlaybackStatus.ERROR);c.release()
  for(type in listOf(PlaybackFailureType.AUTHENTICATION,PlaybackFailureType.SOURCE_UNAVAILABLE)){
   val(controller,adapters)=fixture();runCurrent();val f=PlaybackFailure(type,"erro",true)
   adapters[0].state.value=adapters[0].state.value.copy(status=PlaybackStatus.ERROR,failure=f);adapters[0].events.emit(PlaybackEngineEvent.Failure(f));runCurrent()
   adapters[0].state.value=adapters[0].state.value.copy(positionMs=120001);runCurrent()
   pass("ticker nao exibe erro definitivo durante recuperacao $type",controller.state.value.status==PlaybackStatus.BUFFERING)
   advanceTimeBy(300);runCurrent();pass("estado e evento duplicados fazem uma troca $type",adapters.size==2);controller.release()
  }
  val(drm,drmAdapters)=fixture();runCurrent();drmAdapters[0].events.emit(PlaybackEngineEvent.Failure(PlaybackFailure(PlaybackFailureType.DRM,"protegido",false)));runCurrent();advanceTimeBy(1000);runCurrent();pass("DRM nao entra em tentativas inuteis",drmAdapters.size==1 && drm.state.value.status==PlaybackStatus.ERROR);drm.release()
  val(paused,ps)=fixture();runCurrent();paused.pause();ps[0].events.emit(PlaybackEngineEvent.Failure(failure));runCurrent();advanceTimeBy(300);runCurrent()
  pass("troca preserva escolha de pausa",ps[1].opened?.playWhenReady==false);paused.release()
  val(network,ns)=fixture();runCurrent()
  repeat(3) { ns.last().events.emit(PlaybackEngineEvent.Failure(PlaybackFailure(PlaybackFailureType.NETWORK,"rede",true)));runCurrent();advanceTimeBy(2000);runCurrent() }
  pass("rede tenta duas reconexoes e depois motor alternativo",ns.map{it.engine}==listOf(PlaybackEngine.MEDIA3,PlaybackEngine.MEDIA3,PlaybackEngine.MEDIA3,PlaybackEngine.VLC));network.release()
  val(cancel,cs)=fixture();runCurrent();cs[0].events.emit(PlaybackEngineEvent.Failure(failure));runCurrent();cancel.release();advanceTimeBy(1000);runCurrent();pass("sair cancela troca pendente",cs.size==1 && cs[0].released)
 } finally {Dispatchers.resetMain()}
}
