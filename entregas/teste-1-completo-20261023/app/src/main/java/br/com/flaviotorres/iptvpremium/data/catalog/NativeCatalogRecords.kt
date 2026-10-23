package br.com.flaviotorres.iptvpremium.data.catalog

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ForeignKey
import br.com.flaviotorres.iptvpremium.data.cache.entity.*

/** A stable catalog has independent completion state, not a replacement snapshot per refresh. */
@Entity(tableName="native_catalog_state",primaryKeys=["playlistId","generationId","kind"],
    foreignKeys=[ForeignKey(entity=PlaylistCacheGenerationEntity::class,parentColumns=["playlistId","generationId"],
        childColumns=["playlistId","generationId"],onDelete=ForeignKey.CASCADE)])
data class NativeCatalogState(val playlistId:String,val generationId:String,val kind:String,
    val runId:String,val status:String,val itemCount:Long,val updatedAt:Long)

/** Compact visitation ledger: payload rows need not be rewritten to mark them seen. */
@Entity(tableName="native_catalog_seen",primaryKeys=["playlistId","generationId","kind","itemId"],
    indices=[Index(value=["playlistId","generationId","kind","runId"])],
    foreignKeys=[ForeignKey(entity=PlaylistCacheGenerationEntity::class,parentColumns=["playlistId","generationId"],
        childColumns=["playlistId","generationId"],onDelete=ForeignKey.CASCADE)])
data class NativeCatalogSeen(val playlistId:String,val generationId:String,val kind:String,
    val itemId:String,val runId:String)

internal enum class NativeKind(val wire:String,val categoryAction:String,val itemAction:String) {
    LIVE("LIVE_TV","get_live_categories","get_live_streams"),
    MOVIES("MOVIES","get_vod_categories","get_vod_streams"),
    SERIES("SERIES","get_series_categories","get_series")
}
internal data class NativeBatch(val playlist:String,val generation:String,val kind:String,val run:String,
    val channels:List<CachedChannelEntity> = emptyList(), val movies:List<CachedMovieEntity> = emptyList(),
    val series:List<CachedSeriesEntity> = emptyList(),val episodes:List<CachedEpisodeEntity> = emptyList()) {
    val ids:List<String> get()=when(kind){"LIVE_TV"->channels.map{it.channelId};"MOVIES"->movies.map{it.movieId};"EPISODES"->episodes.map{it.episodeId};else->series.map{it.seriesId}}
}
internal data class NativeWriteStats(val inserted:Int=0,val changed:Int=0,val moved:Int=0,val unchanged:Int=0,val duplicate:Int=0) {
    operator fun plus(other:NativeWriteStats)=NativeWriteStats(inserted+other.inserted,changed+other.changed,moved+other.moved,unchanged+other.unchanged,duplicate+other.duplicate)
    val accepted:Int get()=inserted+changed+moved+unchanged
}
