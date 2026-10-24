package br.com.flaviotorres.iptvpremium.data.repository

/** A failure of an earlier candidate set must not condemn newly available official stills. */
internal fun seriesEpisodeNeedsFallback(primary: List<String>, failedCandidates: List<String>?,
                                        secondary: String? = null): Boolean =
    episodeReferenceImages(null, null, secondary).isEmpty() &&
        (primary.isEmpty() || primary == failedCandidates)

/** A known working or not-yet-tested candidate gets its turn before another service is queried. */
internal fun seriesBackdropNeedsFallback(official: String?, provider: String?, secondary: String?,
                                        failedCandidates: Set<String>): Boolean =
    seriesBackdropReferences(official, provider, secondary, failedCandidates).isEmpty()
