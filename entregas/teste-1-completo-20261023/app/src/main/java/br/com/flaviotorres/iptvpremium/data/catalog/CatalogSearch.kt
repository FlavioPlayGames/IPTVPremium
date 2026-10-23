package br.com.flaviotorres.iptvpremium.data.catalog

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase

@Fts4(tokenizer="unicode61") @Entity(tableName="catalog_movie_fts")
data class MovieSearch(@PrimaryKey @ColumnInfo(name="rowid")val rowid:Long,val name:String)
@Fts4(tokenizer="unicode61") @Entity(tableName="catalog_series_fts")
data class SeriesSearch(@PrimaryKey @ColumnInfo(name="rowid")val rowid:Long,val name:String)
@Fts4(tokenizer="unicode61") @Entity(tableName="catalog_channel_fts")
data class ChannelSearch(@PrimaryKey @ColumnInfo(name="rowid")val rowid:Long,val name:String)

internal object CatalogSearch {
    private val words=Regex("[\\p{L}\\p{N}]+")
    fun expression(query:String):String=words.findAll(query).take(12).map{ "\""+it.value.take(80)+"*\"" }.joinToString(" AND ")
    fun install(db:SupportSQLiteDatabase,rebuild:Boolean=false) {
        for((table,fts)in listOf("cached_movies" to "catalog_movie_fts","cached_series" to "catalog_series_fts","cached_channels" to "catalog_channel_fts")) {
            val id=when(table){"cached_movies"->"movieId";"cached_series"->"seriesId";else->"channelId"}
            db.execSQL("CREATE TRIGGER IF NOT EXISTS ${fts}_bi BEFORE INSERT ON $table BEGIN DELETE FROM $fts WHERE rowid IN (SELECT rowid FROM $table WHERE playlistId=new.playlistId AND generationId=new.generationId AND $id=new.$id); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS ${fts}_ai AFTER INSERT ON $table BEGIN INSERT OR REPLACE INTO $fts(rowid,name) VALUES(new.rowid,new.name); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS ${fts}_ad AFTER DELETE ON $table BEGIN DELETE FROM $fts WHERE rowid=old.rowid; END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS ${fts}_au AFTER UPDATE OF name ON $table WHEN old.name != new.name BEGIN DELETE FROM $fts WHERE rowid=old.rowid; INSERT OR REPLACE INTO $fts(rowid,name) VALUES(new.rowid,new.name); END")
            if(rebuild){db.execSQL("DELETE FROM $fts");db.execSQL("INSERT INTO $fts(rowid,name) SELECT rowid,name FROM $table")}
        }
    }
}
