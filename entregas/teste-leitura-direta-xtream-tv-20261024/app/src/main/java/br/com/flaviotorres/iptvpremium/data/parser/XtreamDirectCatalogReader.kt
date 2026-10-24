package br.com.flaviotorres.iptvpremium.data.parser

import android.util.JsonReader
import android.util.JsonToken
import android.util.MalformedJsonException
import br.com.flaviotorres.iptvpremium.domain.model.Movie
import br.com.flaviotorres.iptvpremium.domain.model.Series
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import br.com.flaviotorres.iptvpremium.data.cache.ImportTuningState
import java.io.EOFException
import java.io.Reader
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class XtreamJsonCompatibilityException(endpoint: String, cause: Exception) : IllegalStateException(
    "Resposta Xtream inválida em $endpoint: resposta requer parser compatível.", cause)

/** Reads selected fields once. Existing model conversion is shared with the legacy parser. */
internal object XtreamDirectCatalogReader {
    data class Stats(val records: Long, val characters: Long, val recoveredClosingBracket: Boolean,
                     val decodeMs: Long, val readerMs: Long)
    private val movieKeys = setOf("stream_id", "name", "category_id", "stream_icon", "backdrop_path",
        "direct_source", "year", "releaseDate", "release_date", "rating", "duration_secs", "duration", "added")
    private val seriesKeys = setOf("series_id", "name", "category_id", "cover", "backdrop_path", "year",
        "releaseDate", "release_date", "rating", "duration_secs", "duration", "plot", "genre", "imdb_id",
        "tmdb_id", "tmdb", "youtube_trailer", "title_logo", "title_logo_url", "clearlogo", "clear_logo", "movie_logo", "logo")

    suspend fun read(source: Reader, endpoint: String, mapper: DefaultXtreamParser,
                     emit: suspend (PlaylistContent) -> Unit): Stats {
        require(endpoint == "get_vod_streams" || endpoint == "get_series")
        val context = currentCoroutineContext()
        val input = RecoveringReader(source) { context.ensureActive() }
        val json = JsonReader(input).apply { isLenient = true }
        val movies = ArrayList<Movie>()
        val series = ArrayList<Series>()
        val keys = if (endpoint == "get_vod_streams") movieKeys else seriesKeys
        val profile = ImportTuningState.profile
        var records = 0L
        var inBatch = 0
        var batchStart = 0L
        var consumerNanos = 0L
        var consumerFailure: Throwable? = null
        val started = System.nanoTime()
        suspend fun flush() {
            if (inBatch == 0) return
            context.ensureActive()
            val batch = PlaylistContent(movies = movies.toList(), series = series.toList())
            val start = System.nanoTime()
            try { emit(batch) } catch (failure: Throwable) { consumerFailure = failure; throw failure } finally { consumerNanos += System.nanoTime() - start }
            context.ensureActive()
            movies.clear(); series.clear(); inBatch = 0; batchStart = input.characters
        }
        try {
            val first = try { json.peek() } catch (empty: EOFException) {
                if (input.onlyTrivia) return Stats(0, input.characters, false, 0, input.readNanos / 1_000_000)
                throw empty
            }
            if (first != JsonToken.BEGIN_ARRAY) error("Expected catalog array")
            json.beginArray()
            while (json.hasNext()) {
                context.ensureActive()
                if (json.peek() == JsonToken.BEGIN_OBJECT) {
                    val item = JSONObject()
                    json.beginObject()
                    while (json.hasNext()) {
                        context.ensureActive()
                        val key = json.nextName()
                        if (key in keys) item.put(key, value(json, 0)) else json.skipValue()
                    }
                    json.endObject()
                    if (endpoint == "get_vod_streams") mapper.movieFromCatalogItem(item)?.let(movies::add)
                    else mapper.seriesFromCatalogItem(item)?.let(series::add)
                } else json.skipValue()
                records++; inBatch++
                if (inBatch >= profile.batchSize || input.characters - batchStart >= profile.batchCharacters) flush()
            }
            json.endArray()
            // Buffered trailing input is deliberately ignored just as before; late HTTP errors still abort.
            val drain = CharArray(8192)
            while (input.read(drain, 0, drain.size) >= 0) context.ensureActive()
            flush()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: MalformedJsonException) { if (consumerFailure === error || input.failure === error) throw error; throw invalid(endpoint, error) }
        catch (error: EOFException) { if (consumerFailure === error || input.failure === error) throw error; throw invalid(endpoint, error) }
        catch (error: org.json.JSONException) { if (consumerFailure === error) throw error; throw invalid(endpoint, error) }
        catch (error: IllegalStateException) { if (consumerFailure === error) throw error; throw invalid(endpoint, error) }
        val elapsed = System.nanoTime() - started
        return Stats(records, input.characters, input.recovered,
            (elapsed - input.readNanos - consumerNanos).coerceAtLeast(0) / 1_000_000, input.readNanos / 1_000_000)
    }

    private fun invalid(endpoint: String, cause: Exception) = XtreamJsonCompatibilityException(endpoint, cause)

    private fun value(json: JsonReader, depth: Int): Any {
        check(depth < 256) { "JSON nesting limit" }
        return when (json.peek()) {
            JsonToken.NULL -> { json.nextNull(); JSONObject.NULL }
            JsonToken.BOOLEAN -> json.nextBoolean()
            JsonToken.NUMBER -> {
                val raw = json.nextString()
                raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: error("Invalid JSON number")
            }
            JsonToken.STRING -> json.nextString().also { raw ->
                // JsonReader exposes quoted and unquoted literals alike as STRING. For
                // nonstandard numeric forms, retry with org.json to preserve exact IDs/types.
                val unusual = raw.length > 1 && (raw[0] == '+' || raw[0] == '.' || raw.startsWith("-.") ||
                    (raw[0] == '0' && (raw[1].isDigit() || raw[1] == 'x' || raw[1] == 'X')) || raw.endsWith('.'))
                if (unusual && (raw.toDoubleOrNull() != null || raw.startsWith("0x", true))) {
                    error("Nonstandard numeric literal")
                }
            }
            JsonToken.BEGIN_ARRAY -> JSONArray().apply {
                json.beginArray(); while (json.hasNext()) put(value(json, depth + 1)); json.endArray()
            }
            JsonToken.BEGIN_OBJECT -> JSONObject().apply {
                json.beginObject(); while (json.hasNext()) put(json.nextName(), value(json, depth + 1)); json.endObject()
            }
            else -> error("Invalid JSON value")
        }
    }

    /** Preserves the old, bounded missing-final-bracket recovery test, without buffering the payload. */
    private class RecoveringReader(private val source: Reader, private val active: () -> Unit) : Reader() {
        var characters = 0L; private set
        var recovered = false; private set
        var readNanos = 0L; private set
        var onlyTrivia = true; private set
        var failure: Throwable? = null; private set
        private var triviaState = 0
        private var arrays = 0
        private var objects = 0
        private var quote = false
        private var escape = false
        private var invalid = false
        private var last = ' '
        override fun read(buffer: CharArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            active()
            val start = System.nanoTime()
            val n = try { source.read(buffer, offset, length) } catch (error: Throwable) { failure = error; throw error } finally { readNanos += System.nanoTime() - start }
            if (n < 0) {
                if (triviaState == 1 || triviaState == 3 || triviaState == 4) onlyTrivia = false
                if (!recovered && characters <= 8_000_000 && !invalid && !quote && !escape && arrays == 1 && objects == 0 && last == '}') {
                    buffer[offset] = ']'; recovered = true; return 1
                }
                return -1
            }
            for (i in offset until offset + n) {
                val c = buffer[i]; characters++
                if (onlyTrivia && triviaState == 0 && c == '\ufeff') buffer[i] = ' '
                if (onlyTrivia) when (triviaState) {
                    0 -> when { c.isWhitespace() || c == '\ufeff' -> Unit; c == '/' -> triviaState = 1; c == '#' -> triviaState = 2; else -> onlyTrivia = false }
                    1 -> when(c) { '/' -> triviaState = 2; '*' -> triviaState = 3; else -> onlyTrivia = false }
                    2 -> if (c == '\n' || c == '\r') triviaState = 0
                    3 -> if (c == '*') triviaState = 4
                    4 -> when(c) { '/' -> triviaState = 0; '*' -> Unit; else -> triviaState = 3 }
                }
                if (characters > 8_000_000) continue
                if (!c.isWhitespace()) last = c
                if (quote) {
                    if (escape) escape = false
                    else when(c) { '\\' -> escape = true; '"' -> quote = false; else -> if (c.code < 0x20) invalid = true }
                } else when(c) {
                    '"' -> quote = true
                    '[' -> arrays++
                    ']' -> { arrays--; if (arrays < 1) invalid = true }
                    '{' -> objects++
                    '}' -> { objects--; if (objects < 0) invalid = true }
                }
            }
            return n
        }
        override fun close() = source.close()
    }
}
