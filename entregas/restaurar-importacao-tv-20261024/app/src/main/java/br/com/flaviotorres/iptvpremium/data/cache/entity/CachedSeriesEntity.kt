package br.com.flaviotorres.iptvpremium.data.cache.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_series",
    primaryKeys = [
        "playlistId",
        "generationId",
        "seriesId"
    ],
    indices = [
        Index(
            value = [
                "playlistId",
                "generationId",
                "categoryId"
            ]
        ),
        Index(
            value = [
                "playlistId",
                "generationId",
                "name"
            ]
        ),
        Index(
            value = [
                "playlistId",
                "generationId",
                "providerOrder"
            ]
        )
    ]
)
data class CachedSeriesEntity(
    val playlistId: String,
    val generationId: String,
    val seriesId: String,
    val name: String,
    val categoryId: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val year: Int?,
    val rating: Double?,
    val plot: String?,
    val titleLogoUrl: String?,
    val imdbId: String?,
    val tmdbId: String?,
    val ratingSource: String?,
    val durationSeconds: Int?,
    val genres: String?,
    val trailerUrl: String?,
    // Ordem original de chegada do provedor, igual ao catalogo de Filmes.
    val providerOrder: Int? = null
)
