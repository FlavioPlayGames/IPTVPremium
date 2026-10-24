package br.com.flaviotorres.iptvpremium.data.repository

/** Uma lista importada com episodios reais nao precisa esperar uma chamada remota. */
internal suspend fun loadSeriesEpisodeContent(
    local: suspend () -> SeriesEpisodesResult?,
    remote: suspend () -> SeriesEpisodesResult?
): SeriesEpisodesResult? {
    local()?.takeIf { it.episodes.isNotEmpty() }?.let { return it }
    return remote()?.takeIf { it.episodes.isNotEmpty() }
}

/** The official still wins once available; the provider remains the immediate fallback. */
internal fun episodeReferenceImages(tmdb: String?, provider: String?, secondary: String? = null): List<String> =
    listOfNotNull(tmdb, provider, secondary).map(String::trim)
        .filter { it.startsWith("https://", true) || it.startsWith("http://", true) }.distinct()

/** Horizontal artwork must not reuse a vertical poster, including images returned from RAM. */
internal fun seriesLandscapeImage(width: Int, height: Int): Boolean =
    width > 0 && height > 0 && width >= height

internal fun seriesBackdropReferences(official: String?, provider: String?, secondary: String?,
                                     failed: Set<String>): List<String> =
    episodeReferenceImages(official, provider, secondary).filterNot { it in failed }
