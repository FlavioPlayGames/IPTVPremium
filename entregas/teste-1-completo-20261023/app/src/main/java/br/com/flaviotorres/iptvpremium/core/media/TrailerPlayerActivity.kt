package br.com.flaviotorres.iptvpremium.core.media

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*

/** Official YouTube embed for YouTube trailers; Media3 for direct provider videos. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class TrailerPlayerActivity : ComponentActivity() {
    private lateinit var frame: FrameLayout
    private lateinit var status: TextView
    private lateinit var retry: Button
    private lateinit var loading: ProgressBar
    private var resolverJob: Job? = null
    private var startupJob: Job? = null
    private var choices = emptyList<TrailerChoice>()
    private var choiceIndex = 0
    private var player: ExoPlayer? = null
    private var web: WebView? = null
    private var videoView: View? = null
    private var videoCallback: WebChromeClient.CustomViewCallback? = null
    private var position = 0L
    private var resumePlaying = true
    private var generation = 0
    private var loadingTrailer = false
    private var youtubeApiReady = false
    private var waitingForGesture = false
    private val request by lazy {
        TrailerRequest(intent.getStringExtra("title").orEmpty(), intent.getStringExtra("raw"),
            intent.getStringExtra("kind") ?: "movie", intent.getStringExtra("tmdbId"),
            intent.getIntExtra("year", 0).takeIf { it > 0 })
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = trailerSearchTitle(request.title).ifBlank { "Trailer" }; setTextColor(Color.WHITE); textSize = 18f
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(12), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        retry = Button(this).apply { text = "Tentar outro"; visibility = View.GONE; setOnClickListener {
            if (choiceIndex + 1 < choices.size) { choiceIndex++; position = 0; resumePlaying = true; showChoice() }
            else resolve()
        } }
        root.addView(header)
        status = TextView(this).apply { setTextColor(Color.LTGRAY); textSize = 13f; setPadding(dp(12), 0, dp(12), dp(4)) }
        frame = FrameLayout(this)
        root.addView(frame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        resolve()
    }
    private fun resolve() {
        resolverJob?.cancel(); releaseViews()
        choices = emptyList(); choiceIndex = 0; position = 0; resumePlaying = true
        status.visibility = View.VISIBLE
        status.text = "Procurando trailer…"
        retry.visibility = View.GONE
        loading = ProgressBar(this)
        frame.addView(loading, FrameLayout.LayoutParams(dp(48), dp(48), android.view.Gravity.CENTER))
        resolverJob = lifecycleScope.launch {
            val result = TrailerResolver().resolve(request)
            if (isFinishing || isDestroyed) return@launch
            choices = result
            if (result.isEmpty()) showError("Não encontrei um trailer disponível para este título.")
            else showChoice()
        }
    }
    private fun releaseViews() {
        generation++; startupJob?.cancel()
        hideCustomVideo()
        player?.release(); player = null
        web?.let { view ->
            view.clearCache(true)
            view.stopLoading(); view.onPause(); view.removeJavascriptInterface("TrailerHost")
            (view.parent as? ViewGroup)?.removeView(view); view.destroy()
        }
        web = null; frame.removeAllViews()
    }
    private fun showChoice() {
        releaseViews()
        val choice = choices.getOrNull(choiceIndex) ?: return
        retry.visibility = View.GONE
        status.text = ""
        status.visibility = View.GONE
        try {
            if (choice.youtubeId != null) showYouTube(choice.youtubeId) else showDirect(choice.url)
        } catch (_: Exception) {
            showError("O player deste trailer não está disponível no aparelho. Tente outra versão.")
        }
    }
    private fun showDirect(url: String) {
        val view = PlayerView(this).apply { useController = true }
        frame.addView(view, FrameLayout.LayoutParams(-1, -1))
        player = ExoPlayer.Builder(this).build().also { native ->
            view.player = native
            native.setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, true)
            native.setHandleAudioBecomingNoisy(true)
            native.trackSelectionParameters = native.trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguages("pt-BR", "pt")
                .setPreferredTextLanguages("pt-BR", "pt").build()
            native.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    showError("Este trailer não pôde ser reproduzido. Tente outra versão.")
                }
            })
            native.setMediaItem(MediaItem.fromUri(url), position)
            native.playWhenReady = resumePlaying; native.prepare()
        }
        view.requestFocus(); view.showController()
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun showYouTube(id: String) {
        val token = generation
        loadingTrailer = true
        youtubeApiReady = false
        waitingForGesture = false
        status.visibility = View.VISIBLE
        status.text = "Carregando trailer…"
        val identity = "https://${packageName.lowercase()}/"
        val view = WebView(this)
        view.clearCache(true)
        web = view
        view.setBackgroundColor(Color.TRANSPARENT)
        // Keep the Activity's hardware acceleration, without an extra offscreen texture.
        // A forced hardware layer can hide video surfaces on TV/emulator GPU drivers.
        view.setLayerType(View.LAYER_TYPE_NONE, null)
        view.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false; allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
        }
        view.addJavascriptInterface(object {
            @JavascriptInterface fun ready() { runOnUiThread {
                if (!isDestroyed && generation == token) {
                    youtubeApiReady = true
                    status.text = "Iniciando trailer…"
                    view.requestFocus()
                }
            } }
            @JavascriptInterface fun playing() { runOnUiThread {
                if (!isDestroyed && generation == token) {
                    loadingTrailer = false; waitingForGesture = false
                    startupJob?.cancel(); loading.visibility = View.GONE
                    status.text = ""
                    status.visibility = View.GONE
                }
            } }
            @JavascriptInterface fun manual() { runOnUiThread {
                if (!isDestroyed && generation == token) {
                    waitingForGesture = true; loadingTrailer = false
                    startupJob?.cancel(); loading.visibility = View.GONE
                    status.visibility = View.VISIBLE
                    status.text = "Pressione OK ou toque em Reproduzir."
                    view.requestFocus()
                }
            } }
            @JavascriptInterface fun failed(code: Int) { runOnUiThread {
                if (!isDestroyed && generation == token) showError(
                    if (code == 101 || code == 150 || code == 100)
                        "Este trailer não permite reprodução incorporada ou está indisponível. Tente outro."
                    else "Não foi possível iniciar este trailer no player do YouTube. Tente outro.")
            } }
        }, "TrailerHost")
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val host = request.url.host.orEmpty().lowercase()
                return request.url.scheme != "https" ||
                    !(host == packageName.lowercase() || host in setOf("www.youtube.com", "www.youtube-nocookie.com"))
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if ((request.isForMainFrame || request.url.path.orEmpty().startsWith("/embed/")) && generation == token)
                    showError("Não foi possível carregar o trailer. Verifique a conexão.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (generation == token && (request.isForMainFrame || request.url.path.orEmpty().startsWith("/embed/")))
                    showError("O YouTube não conseguiu carregar este trailer. Tente outro.")
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
                if (generation == token) showError("Não foi possível estabelecer uma conexão segura com o trailer.")
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (generation == token) showError("O player de vídeo foi interrompido pelo aparelho. Tente novamente.")
                return true
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onShowCustomView(customView: View, callback: CustomViewCallback) {
                if (generation != token || videoView != null) { callback.onCustomViewHidden(); return }
                showCustomVideo(customView, callback)
            }
            override fun onHideCustomView() { hideCustomVideo() }
        }
        view.setOnKeyListener { _, key, event ->
            if (!youtubeApiReady || waitingForGesture) return@setOnKeyListener false
            val script = when (key) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "toggleTrailer()"
                KeyEvent.KEYCODE_DPAD_LEFT -> "seekTrailer(-10)"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "seekTrailer(10)"
                else -> null
            }
            if (script != null && event.action == KeyEvent.ACTION_DOWN) view.evaluateJavascript(script, null)
            script != null
        }
        frame.addView(view, FrameLayout.LayoutParams(-1, -1))
        loading = ProgressBar(this)
        frame.addView(loading, FrameLayout.LayoutParams(dp(48), dp(48), android.view.Gravity.CENTER))
        view.loadDataWithBaseURL(identity, youtubeTrailerHtml(id, identity), "text/html", "UTF-8", null)
        startupJob = lifecycleScope.launch {
            delay(20_000)
            if (generation == token && loadingTrailer) showError("O trailer demorou para responder. Tente outra versão.")
        }
    }
    private fun showCustomVideo(customView: View, callback: WebChromeClient.CustomViewCallback) {
        videoView = customView; videoCallback = callback
        (customView.parent as? ViewGroup)?.removeView(customView)
        frame.addView(customView, FrameLayout.LayoutParams(-1, -1))
        customView.bringToFront()
        // TV WebViews may deliver HTML5 video as a separate native surface.
        web?.visibility = View.INVISIBLE
        if (::loading.isInitialized) loading.visibility = View.GONE
        customView.requestFocus()
    }
    private fun hideCustomVideo() {
        val custom = videoView ?: return
        videoView = null
        (custom.parent as? ViewGroup)?.removeView(custom)
        web?.visibility = View.VISIBLE
        val callback = videoCallback; videoCallback = null
        callback?.onCustomViewHidden()
    }
    private fun showError(message: String) {
        loadingTrailer = false; startupJob?.cancel()
        status.visibility = View.VISIBLE
        status.text = message; retry.visibility = View.VISIBLE
        if (status.parent == null) frame.addView(status, FrameLayout.LayoutParams(-1, -2, android.view.Gravity.BOTTOM).apply { bottomMargin = dp(64) })
        if (retry.parent == null) frame.addView(retry, FrameLayout.LayoutParams(dp(160), dp(52), android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(8) })
        if (::loading.isInitialized) loading.visibility = View.GONE
        retry.requestFocus()
    }
    // Android Activity public input callback; AndroidX inherited annotation is overly restrictive.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onPause() {
        player?.let { resumePlaying = it.playWhenReady; position = it.currentPosition; it.pause() }
        web?.evaluateJavascript("if(window.player&&player.pauseVideo)player.pauseVideo()", null)
        web?.onPause()
        super.onPause()
    }
    override fun onStop() { player?.release(); player = null; super.onStop() }
    override fun onResume() {
        super.onResume(); web?.onResume()
        if (choices.getOrNull(choiceIndex)?.youtubeId == null && choices.isNotEmpty() && player == null) showChoice()
    }
    override fun onDestroy() { resolverJob?.cancel(); releaseViews(); super.onDestroy() }
}

internal fun youtubeTrailerHtml(id: String, identity: String): String {
    require(id.matches(Regex("[A-Za-z0-9_-]{11}")))
    require(identity.matches(Regex("https://[a-z0-9.]+/")))
    val origin = java.net.URLEncoder.encode(identity.trimEnd('/'), "UTF-8")
    return """<!doctype html><html lang="pt-BR"><head><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="referrer" content="strict-origin-when-cross-origin"><style>
html,body{width:100%;height:100%;margin:0;background:#000;overflow:hidden}
#player{position:absolute;inset:0;width:100%;height:100%;border:0}
#play{display:none;position:absolute;z-index:2;left:50%;top:50%;transform:translate(-50%,-50%);padding:18px 32px;font:20px sans-serif;color:#fff;background:#202020;border:2px solid #d4af37;border-radius:8px}
</style></head><body>
<iframe id="player" title="Trailer" allow="autoplay; encrypted-media; fullscreen; picture-in-picture" referrerpolicy="strict-origin-when-cross-origin"
src="https://www.youtube.com/embed/$id?enablejsapi=1&amp;origin=$origin&amp;autoplay=1&amp;playsinline=1&amp;controls=1&amp;fs=0&amp;rel=0&amp;hl=pt-BR&amp;cc_lang_pref=pt&amp;cc_load_policy=1"></iframe>
<button id="play" onclick="startTrailer()">Reproduzir</button>
<script>
var player,started=false;
function askForPlay(){if(started)return;var button=document.getElementById('play');button.style.display='block';button.focus();TrailerHost.manual()}
function startTrailer(){if(player&&player.playVideo)player.playVideo();document.getElementById('play').style.display='none';setTimeout(function(){if(!started)askForPlay()},4000)}
function onYouTubeIframeAPIReady(){player=new YT.Player('player',{events:{
onReady:function(){TrailerHost.ready();player.playVideo();setTimeout(function(){if(!started)askForPlay()},4000)},
onStateChange:function(e){if(e.data===1){started=true;document.getElementById('play').style.display='none';TrailerHost.playing()}},
onAutoplayBlocked:askForPlay,onError:function(e){TrailerHost.failed(e.data)}}})}
function toggleTrailer(){if(player&&player.getPlayerState){if(player.getPlayerState()==1)player.pauseVideo();else player.playVideo()}}
function seekTrailer(seconds){if(player&&player.getCurrentTime)player.seekTo(Math.max(0,player.getCurrentTime()+seconds),true)}
// The iframe is already visible. API callbacks are defined before the API is requested.
var api=document.createElement('script');api.src='https://www.youtube.com/iframe_api';api.async=true;
api.onerror=function(){document.getElementById('player').focus();TrailerHost.manual()};document.head.appendChild(api);
</script></body></html>"""
}
