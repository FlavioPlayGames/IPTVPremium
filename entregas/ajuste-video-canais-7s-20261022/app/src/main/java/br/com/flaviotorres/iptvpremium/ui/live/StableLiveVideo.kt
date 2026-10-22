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
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout

/** One SurfaceView stays mounted; expand changes only its bounds, never the media or decoder. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private class LiveSurfaceHost(context:Context,val player:Player):FrameLayout(context) {
    private val surface=SurfaceView(context)
    private val aspect=AspectRatioFrameLayout(context)
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
        if(size.width>0&&size.height>0)aspect.setAspectRatio(size.width.toFloat()*size.pixelWidthHeightRatio/size.height)
    }
    fun resize(fullScreen:Boolean){
        // SurfaceView is a compositor surface: parent clipping cannot contain a ZOOM-sized child.
        // FIT keeps the native surface inside the reserved rectangle at every video aspect ratio.
        aspect.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT
        val radius=if(fullScreen)0f else 4f*resources.displayMetrics.density
        if(cornerRadius!=radius){cornerRadius=radius;invalidateOutline();surface.invalidateOutline()}
    }
    fun detach(){runCatching{player.removeListener(listener);player.clearVideoSurfaceView(surface)}}
}
@Composable
internal fun StableLiveVideo(player:Player,fullScreen:Boolean,modifier:Modifier) {
    AndroidView(modifier=modifier,factory={LiveSurfaceHost(it,player).also{host->host.resize(fullScreen)}},
        update={it.resize(fullScreen)},onRelease={it.detach()})
}
