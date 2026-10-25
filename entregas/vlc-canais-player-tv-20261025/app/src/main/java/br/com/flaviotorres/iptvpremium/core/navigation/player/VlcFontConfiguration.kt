package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import android.os.SystemClock
import android.system.Os
import android.util.AtomicFile
import android.util.Log
import java.io.File

/** A real Fontconfig configuration with an app-writable cache, installed on IO before LibVLC. */
internal object VlcFontConfiguration {
    private var configured = false

    @Synchronized fun prepare(context: Context) {
        if (configured) return
        val started = SystemClock.elapsedRealtime()
        try {
            val fontDirectories = listOf("/system/fonts", "/product/fonts", "/system_ext/fonts")
                .map(::File).filter { it.isDirectory && it.canRead() }.map { it.absolutePath }
            if (fontDirectories.isEmpty()) {
                Log.w("VlcFontPerf", "phase=config_unavailable reason=no_system_font_directory")
                return
            }
            val directory = File(context.filesDir, "vlc-fontconfig-v1")
            val cache = File(context.cacheDir, "vlc-fontconfig-v1")
            check((directory.isDirectory || directory.mkdirs()) && (cache.isDirectory || cache.mkdirs()))
            val file = File(directory, "fonts.conf")
            val xml = vlcFontConfigurationXml(fontDirectories, cache.absolutePath)
            if (!file.isFile || file.length() > 16_384 || file.readText() != xml) {
                val atomic = AtomicFile(file)
                val stream = atomic.startWrite()
                try {
                    stream.write(xml.toByteArray(Charsets.UTF_8))
                    atomic.finishWrite(stream)
                } catch (failure: Exception) {
                    atomic.failWrite(stream)
                    throw failure
                }
            }
            Os.setenv("FONTCONFIG_FILE", file.absolutePath, true)
            configured = true
            Log.i("VlcFontPerf", "phase=config_ready fontDirectories=${fontDirectories.size} durationMs=${SystemClock.elapsedRealtime()-started}")
        } catch (_: Exception) {
            // A font-cache issue must never prevent the user from opening a player.
            Log.w("VlcFontPerf", "phase=config_unavailable reason=setup_failed")
        }
    }
}

/** Keep every system font available for subtitles, including non-Latin alphabets. */
internal fun vlcFontConfigurationXml(fontDirectories: List<String>, cacheDirectory: String): String {
    fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
    return buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<fontconfig>\n")
        fontDirectories.forEach { append("  <dir>${escape(it)}</dir>\n") }
        append("  <cachedir>${escape(cacheDirectory)}</cachedir>\n")
        append("  <alias><family>sans-serif</family><prefer><family>Roboto</family><family>Noto Sans</family></prefer></alias>\n")
        append("</fontconfig>\n")
    }
}
