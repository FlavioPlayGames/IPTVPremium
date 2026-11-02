package br.com.flaviotorres.iptvpremium.core.player

/** Season artwork first; failed downloads continue through to the series cover. */
internal fun playerSeasonPosterCandidates(officialSeason:String?,providerSeason:String?,officialSeries:String?,providerSeries:String?):List<String> =
    listOfNotNull(officialSeason,providerSeason,officialSeries,providerSeries).map(String::trim)
        .filter{it.startsWith("https://",true)||it.startsWith("http://",true)}.distinct()
