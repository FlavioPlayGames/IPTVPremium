package br.com.flaviotorres.iptvpremium.ui.live

import android.content.Context
import android.view.SurfaceView
import android.view.View
import android.view.ViewOutlineProvider
import android.graphics.Outline
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout

internal enum class LivePictureMode(val title:String) { FIT("Encaixar"), CROP("Preencher"), STRETCH("Esticar") }

/** One SurfaceView stays mounted; expand changes only its bounds, never the media or decoder. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private class LiveSurfaceHost(context:Context,val player:ExoPlayer):FrameLayout(context) {
    private val surface=SurfaceView(context)
    private val aspect=AspectRatioFrameLayout(context)
    private var pictureMode=LivePictureMode.FIT
    private var videoAspect=0f
    private var cornerRadius=4f*resources.displayMetrics.density
    private val roundedOutline=object:ViewOutlineProvider(){
        override fun getOutline(view:View,outline:Outline) {
            outline.setRoundRect(0,0,view.width,view.height,cornerRadius)
        }
    }
    private val listener=object:Player.Listener {
        override fun onVideoSizeChanged(videoSize:VideoSize){updateAspect(videoSize)}
    }
    init {
        isFocusable=false;isClickable=false;clipChildren=true;clipToPadding=true
        outlineProvider=roundedOutline;clipToOutline=true
        surface.outlineProvider=roundedOutline;surface.clipToOutline=true
        aspect.addView(surface,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT))
        addView(aspect,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT,android.view.Gravity.CENTER))
        player.addListener(listener);player.setVideoSurfaceView(surface);updateAspect(player.videoSize)
    }
    private fun updateAspect(size:VideoSize) {
        if(size.width>0&&size.height>0)videoAspect=size.width.toFloat()*size.pixelWidthHeightRatio/size.height
        applyPicture()
    }
    private fun applyPicture() {
        // Native SurfaceView bounds never grow outside the host. Crop is done by the codec.
        aspect.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT
        aspect.setAspectRatio(if(pictureMode==LivePictureMode.FIT)videoAspect else 0f)
        player.videoScalingMode=if(pictureMode==LivePictureMode.CROP)C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            else C.VIDEO_SCALING_MODE_SCALE_TO_FIT
    }
    fun resize(fullScreen:Boolean,mode:LivePictureMode){
        if(pictureMode!=mode){pictureMode=mode;applyPicture()}
        val radius=if(fullScreen)0f else 4f*resources.displayMetrics.density
        if(cornerRadius!=radius){cornerRadius=radius;invalidateOutline();surface.invalidateOutline()}
    }
    fun detach(){runCatching{player.removeListener(listener);player.clearVideoSurfaceView(surface)}}
}
@Composable
internal fun StableLiveVideo(player:ExoPlayer,fullScreen:Boolean,pictureMode:LivePictureMode,modifier:Modifier) {
    AndroidView(modifier=modifier,factory={LiveSurfaceHost(it,player).also{host->host.resize(fullScreen,pictureMode)}},
        update={it.resize(fullScreen,pictureMode)},onRelease={it.detach()})
}
