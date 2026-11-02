package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import br.com.flaviotorres.iptvpremium.domain.model.Series
import br.com.flaviotorres.iptvpremium.data.repository.*
import br.com.flaviotorres.iptvpremium.data.remote.ArtworkHttpCache
import br.com.flaviotorres.iptvpremium.data.remote.TmdbClientProvider
import br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

internal class PlayerSeriesImages {
    var root by mutableStateOf<JSONObject?>(null)
    var stills by mutableStateOf<Map<Int,String>>(emptyMap())
    var supplemental by mutableStateOf<SupplementalSeriesArt>(SupplementalSeriesArt())
    var backdrop by mutableStateOf<ImageBitmap?>(null)
    var failures = mutableStateMapOf<String,List<String>>()
    val posters:Map<Int,String> get()=root?.let(::catalogSeasonPosterPaths).orEmpty()
}

/** Metadata is fetched once per visited work/season; each visible episode loads only its own photo. */
@Composable
internal fun rememberPlayerSeriesImages(series:Series?,season:Int,episodes:List<SeriesEpisodeInfo>):PlayerSeriesImages {
    val context=LocalContext.current.applicationContext
    val images=remember(series?.id){PlayerSeriesImages()}
    val memo=remember(series?.id){SeriesArtworkRequestMemo()}
    var metadataReady by remember(series?.id){mutableStateOf(false)}
    var seasonReady by remember(series?.id,season){mutableStateOf(false)}
    LaunchedEffect(series) {
        if(series==null)return@LaunchedEffect
        try {
            images.root=withTimeoutOrNull(8_000){resolveSeriesArtworkIdentity(series.name,series.year,series.tmdbId,series.imdbId,
                TmdbClientProvider.client::get,referenceImages=listOfNotNull(series.posterUrl,series.backdropUrl))}
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){}finally{metadataReady=true}
    }
    val resolvedId=images.root?.optInt("id",-1)?.takeIf{it>0}
    LaunchedEffect(series?.id,season,resolvedId,metadataReady) {
        seasonReady=false;images.stills=emptyMap();images.supplemental=SupplementalSeriesArt();images.failures.clear()
        try {
            if(resolvedId!=null)images.stills=memo.load(listOf("season",resolvedId,season)) {
                withTimeoutOrNull(4_000){loadSeriesSeasonArtwork(resolvedId,season)}
            }?.episodeStills.orEmpty()
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){}finally{seasonReady=true}
    }
    val rows=episodes.filter{it.seasonNumber==season}
    val missing=rows.filter { row->seriesEpisodeNeedsFallback(episodeReferenceImages(images.stills[row.episodeNumber],row.thumbnailUrl),images.failures[row.id],images.supplemental.episodeStills[row.episodeNumber]) }
        .map{it.episodeNumber}.filter{it>0}.toSet()
    val backdropUrl=seriesArtworkImage("w780",images.root?.optString("backdrop_path"))?:webImage(series?.backdropUrl)
    LaunchedEffect(series?.id,season,metadataReady,seasonReady,missing,backdropUrl) {
        if(series==null || !metadataReady || !seasonReady || (missing.isEmpty() && backdropUrl!=null))return@LaunchedEffect
        val identity=ArtworkIdentity((images.root?.let(::preferredSeriesSearchNames).orEmpty()+series.name).distinct(),
            series.year?:images.root?.optString("first_air_date")?.take(4)?.toIntOrNull(),series.imdbId?:images.root?.optJSONObject("external_ids")?.optString("imdb_id"))
        try {
            val extra=memo.load(listOf("supplemental",identity,season,missing,backdropUrl==null)) {
                withTimeoutOrNull(8_000){SeriesArtworkFallback(ArtworkHttpCache.getInstance(context)::get).load(identity,season,missing,backdropUrl==null)}
            }
            if(extra!=null)images.supplemental=images.supplemental.copy(backdropUrl=extra.backdropUrl?:images.supplemental.backdropUrl,episodeStills=images.supplemental.episodeStills+extra.episodeStills)
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){}
    }
    val backdrops=listOfNotNull(backdropUrl,webImage(series?.backdropUrl),images.supplemental.backdropUrl).distinct()
    LaunchedEffect(series?.id,backdrops) {
        images.backdrop=null
        loadEpisodeThumbnail(backdrops,{url->MovieDetailsImageLoader.load(context,url,780,440,"series_backdrop")?.takeIf{seriesLandscapeImage(it.width,it.height)}},{images.backdrop=it})
    }
    return images
}
