package br.com.flaviotorres.iptvpremium.data.repository

import kotlinx.coroutines.CancellationException

/** Somente fotos normais. A UI reutiliza o fundo da serie quando nenhuma foto funciona. */
internal suspend fun <T : Any> loadEpisodeThumbnail(
    images: List<String>,
    image: suspend (String) -> T?,
    show: (T) -> Unit
) {
    for (url in images.distinct()) {
        val result = try { image(url) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        if (result != null) { show(result); return }
    }
}
