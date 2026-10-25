package br.com.flaviotorres.iptvpremium.data.cache.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_movies",
    primaryKeys = [
        "playlistId",
        "generationId",
        "movieId"
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
                "addedAtEpochSeconds"
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
data class CachedMovieEntity(
    val playlistId: String,
    val generationId: String,
    val movieId: String,
    val name: String,
    val categoryId: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val streamUrl: String,
    val year: Int?,
    val rating: Double?,
    val durationSeconds: Long?,
    val addedAtEpochSeconds: Long? = null,
    val providerOrder: Int? = null
)
