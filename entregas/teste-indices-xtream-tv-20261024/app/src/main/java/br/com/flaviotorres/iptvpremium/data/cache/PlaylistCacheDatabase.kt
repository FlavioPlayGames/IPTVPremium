package br.com.flaviotorres.iptvpremium.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedCategoryEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedChannelEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedEpisodeEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedMovieEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedSeriesEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.PlaylistCacheGenerationEntity

@Database(
    entities = [
        PlaylistCacheGenerationEntity::class,
        CachedCategoryEntity::class,
        CachedChannelEntity::class,
        CachedMovieEntity::class,
        CachedSeriesEntity::class,
        CachedEpisodeEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PlaylistCacheDatabase : RoomDatabase() {

    abstract fun playlistCacheDao(): PlaylistCacheDao

    companion object {

        @Volatile
        private var INSTANCE: PlaylistCacheDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(
                startVersion = 1,
                endVersion = 2
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        ALTER TABLE cached_movies
                        ADD COLUMN addedAtEpochSeconds INTEGER
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_cached_movies_playlistId_generationId_addedAtEpochSeconds
                        ON cached_movies(
                            playlistId,
                            generationId,
                            addedAtEpochSeconds
                        )
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(
                startVersion = 2,
                endVersion = 3
            ) {
                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {
                    db.execSQL(
                        """
                        ALTER TABLE cached_movies
                        ADD COLUMN providerOrder INTEGER
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_cached_movies_playlistId_generationId_providerOrder
                        ON cached_movies(
                            playlistId,
                            generationId,
                            providerOrder
                        )
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_3_4 =
            object : Migration(
                startVersion = 3,
                endVersion = 4
            ) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Nao apaga historico, cache, favoritos ou episodios.
                    db.execSQL(
                        "ALTER TABLE cached_series ADD COLUMN providerOrder INTEGER"
                    )
                    // Aproveita a sequencia existente de insercao para
                    // preservar uma ordem estavel sem exigir novo download.
                    db.execSQL(
                        "UPDATE cached_series SET providerOrder = rowid"
                    )
                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_cached_series_playlistId_generationId_providerOrder
                        ON cached_series(playlistId, generationId, providerOrder)
                        """.trimIndent()
                    )
                }
            }

        fun getInstance(
            context: Context
        ): PlaylistCacheDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: ImportTuning.configure(Room.databaseBuilder(
                            context.applicationContext,
                            PlaylistCacheDatabase::class.java,
                            "playlist_cache.db"
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4
                            ), ImportTuning.load(context))
                            .addCallback(object : RoomDatabase.Callback() {
                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onOpen(db)
                                    // Also repairs an interrupted bulk import before exposing the database.
                                    XtreamBulkIndexPlan.repair(db)
                                }
                            })
                            .build()
                            .also { database ->
                                XtreamBulkIndexes.attach(database)
                                INSTANCE = database
                            }
                }
        }
    }
}
