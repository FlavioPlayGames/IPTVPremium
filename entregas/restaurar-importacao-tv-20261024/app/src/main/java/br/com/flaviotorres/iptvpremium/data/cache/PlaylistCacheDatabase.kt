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
    version = 6,
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

        // Version 4 already has exactly the restored indices; no table/data changes.
        private val MIGRATION_4_6 = object : Migration(4,6) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }
        private val MIGRATION_5_6 = object : Migration(5,6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_movies_playlistId_generationId_name ON cached_movies(playlistId,generationId,name)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_movies_playlistId_generationId_addedAtEpochSeconds ON cached_movies(playlistId,generationId,addedAtEpochSeconds)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_series_playlistId_generationId_name ON cached_series(playlistId,generationId,name)")
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
                            .addCallback(object : RoomDatabase.Callback() {
                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onOpen(db)
                                    // Never treat the abandoned category experiment as a full catalogue.
                                    // Keep the rows for normal later cleanup; preserve favourites/history.
                                    db.execSQL("""
                                        UPDATE playlist_cache_generations SET status='STALE'
                                        WHERE status IN ('ACTIVE','READY') AND EXISTS (
                                            SELECT 1 FROM cached_categories c
                                            WHERE c.playlistId=playlist_cache_generations.playlistId
                                            AND c.generationId=playlist_cache_generations.generationId
                                            AND c.type IN ('__LAZY_MOVIES__','__LAZY_SERIES__')
                                        )
                                    """.trimIndent())
                                }
                            })
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_6,
                                MIGRATION_5_6
                            ), ImportTuning.load(context))
                            .build()
                            .also { database ->
                                INSTANCE = database
                            }
                }
        }
    }
}
