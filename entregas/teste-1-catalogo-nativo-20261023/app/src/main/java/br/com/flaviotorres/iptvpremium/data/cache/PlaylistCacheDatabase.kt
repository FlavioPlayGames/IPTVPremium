package br.com.flaviotorres.iptvpremium.data.cache

import br.com.flaviotorres.iptvpremium.data.catalog.*
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
        CachedEpisodeEntity::class,
        NativeCatalogState::class,
        NativeCatalogSeen::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PlaylistCacheDatabase : RoomDatabase() {

    abstract fun playlistCacheDao(): PlaylistCacheDao
    internal abstract fun nativeCatalogDao(): NativeCatalogDao

    companion object {
        const val FILE_NAME = "playlist_cache_native_test1.db"

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

        private val MIGRATION_4_5 = object : Migration(4,5) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS native_catalog_state (playlistId TEXT NOT NULL,generationId TEXT NOT NULL,kind TEXT NOT NULL,runId TEXT NOT NULL,status TEXT NOT NULL,itemCount INTEGER NOT NULL,updatedAt INTEGER NOT NULL,PRIMARY KEY(playlistId,generationId,kind),FOREIGN KEY(playlistId,generationId) REFERENCES playlist_cache_generations(playlistId,generationId) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS native_catalog_seen (playlistId TEXT NOT NULL,generationId TEXT NOT NULL,kind TEXT NOT NULL,itemId TEXT NOT NULL,runId TEXT NOT NULL,PRIMARY KEY(playlistId,generationId,kind,itemId),FOREIGN KEY(playlistId,generationId) REFERENCES playlist_cache_generations(playlistId,generationId) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_native_catalog_seen_playlistId_generationId_kind_runId ON native_catalog_seen(playlistId,generationId,kind,runId)")
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
                            FILE_NAME
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5
                            ), ImportTuning.load(context))
                            .build()
                            .also { database ->
                                INSTANCE = database
                            }
                }
        }
    }
}
