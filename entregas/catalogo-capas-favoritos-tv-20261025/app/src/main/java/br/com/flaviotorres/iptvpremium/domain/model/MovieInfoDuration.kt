package br.com.flaviotorres.iptvpremium.domain.model

internal object MovieInfoDuration {
    /** Caller has already confirmed the TMDB movie identity. Runtime is minutes. */
    fun resolve(providerSeconds:Int?,confirmedTmdbMinutes:Int?):Int? =
        confirmedTmdbMinutes?.takeIf { it in 1..Int.MAX_VALUE/60 }?.times(60)
            ?: providerSeconds?.takeIf { it>0 }
}
