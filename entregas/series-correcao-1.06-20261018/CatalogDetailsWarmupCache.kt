package br.com.flaviotorres.iptvpremium.data.cache

import android.os.SystemClock
import android.util.Log
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodesResult
import br.com.flaviotorres.iptvpremium.domain.model.MovieDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.LinkedHashMap

/**
 * Cache em memoria de INFORMACOES de Filmes e Series.
 *
 * Foi separado do cache de imagens (MoviePosterLoader e
 * MovieDetailsImageLoader). Nenhum Bitmap e guardado aqui.
 *
 * A integracao posterior no AppNavigation aquece este cache ao iniciar:
 * - Ate 10 titulos por categoria, apos iniciar o download das capas.
 * - Novos titulos do Paging entram na fila somente quando aparecem.
 * - A abertura de detalhes consulta este cache ANTES da rede.
 *
 * O trabalho em segundo plano tem, no maximo, duas consultas concorrentes.
 * A abertura solicitada pelo usuario NAO usa esse limitador: assim nao fica
 * bloqueada atras de outras categorias. Consultas simultaneas ao MESMO
 * titulo compartilham um lock, evitando repetir a requisicao.
 *
 * Os dados ficam somente na memoria do processo e nunca incluem senhas.
 * A chave leva playlist + geracao ativa + ID: dados de uma playlist antiga
 * nao sao reutilizados indevidamente apos a importacao ou troca de lista.
 *
 * O limite de 64 MiB combinado e exclusivo das capas em disco.
 * Para metadados, usamos caches pequenos e independentes, com LRU e TTL.
 */
internal object CatalogDetailsWarmupCache {

    private const val TAG = "CatalogDetailsWarmup"
    private const val MAX_MOVIES = 180
    private const val MAX_SERIES = 40
    private const val MAX_SERIES_EPISODES = 8_000
    private const val MAX_EPISODES_PER_SERIES = 3_000
    private const val MAX_SERIES_ESTIMATED_BYTES = 16L * 1024 * 1024
    private const val EXPIRATION_MS = 6L * 60L * 60L * 1_000L
    private const val BACKGROUND_TIMEOUT_MS = 12_000L

    private class CacheEntry<T : Any>(
        val data: T,
        val createdAt: Long,
        val estimatedBytes: Long = 0L
    )

    private val movieEntries =
        LinkedHashMap<String, CacheEntry<MovieDetails>>(16, 0.75f, true)
    private val seriesEntries =
        LinkedHashMap<String, CacheEntry<SeriesEpisodesResult>>(16, 0.75f, true)

    // Fila limitada SOMENTE para o aquecimento em segundo plano.
    private val backgroundSlots = Semaphore(permits = 2)

    // Locks fixos: nao criamos um Mutex permanente para cada titulo.
    private val movieLocks = Array(64) { Mutex() }
    // Apenas chaves em uso ficam aqui. Titulos diferentes nunca dividem fila.
    private class SeriesLock(
        val mutex: Mutex = Mutex(),
        var users: Int = 0,
        var completed: Result<SeriesEpisodesResult?>? = null
    )
    private val seriesLocks = HashMap<String, SeriesLock>()

    private suspend fun withSeriesLock(
        key: String,
        block: suspend () -> SeriesEpisodesResult?
    ): SeriesEpisodesResult? {
        val entry = synchronized(seriesLocks) {
            seriesLocks.getOrPut(key) { SeriesLock() }.also { it.users++ }
        }
        try {
            return entry.mutex.withLock {
                // Foco e abertura que se sobrepoem recebem o mesmo desfecho,
                // inclusive vazio/erro. Nao repetem toda a recuperacao em fila.
                // A ultima saida remove a entrada, permitindo uma nova tentativa
                // explicita depois, sem guardar falhas no cache permanente.
                entry.completed?.let { return@withLock it.getOrThrow() }
                try {
                    block().also { entry.completed = Result.success(it) }
                } catch (cancelled: CancellationException) {
                    // Cancelar o dono nao cancela quem ainda esta esperando.
                    throw cancelled
                } catch (error: Exception) {
                    entry.completed = Result.failure(error)
                    throw error
                }
            }
        } finally {
            synchronized(seriesLocks) {
                entry.users--
                if (entry.users == 0) seriesLocks.remove(key)
            }
        }
    }

    private fun cacheKey(playlistId: String, generationId: String, itemId: String): String =
        "$playlistId\u0000$generationId\u0000$itemId"

    private fun lockIndex(key: String): Int = (key.hashCode() and Int.MAX_VALUE) % 64

    private fun isFresh(entryTime: Long): Boolean =
        SystemClock.elapsedRealtime() - entryTime in 0..EXPIRATION_MS

    /** Consulta instantanea; nao inicia rede, Room ou decodificacao de imagem. */
    fun getMovie(
        playlistId: String,
        generationId: String,
        movieId: String
    ): MovieDetails? {
        if (playlistId.isBlank() || generationId.isBlank() || movieId.isBlank()) return null
        val key = cacheKey(playlistId, generationId, movieId)
        return synchronized(movieEntries) {
            val entry = movieEntries[key] ?: return@synchronized null
            if (isFresh(entry.createdAt)) entry.data else {
                movieEntries.remove(key)
                null
            }
        }
    }

    /** Consulta instantanea; o resultado pode conter episodios reais do provedor. */
    fun getSeries(
        playlistId: String,
        generationId: String,
        seriesId: String
    ): SeriesEpisodesResult? {
        if (playlistId.isBlank() || generationId.isBlank() || seriesId.isBlank()) return null
        val key = cacheKey(playlistId, generationId, seriesId)
        return synchronized(seriesEntries) {
            val entry = seriesEntries[key] ?: return@synchronized null
            if (isFresh(entry.createdAt) && entry.data.episodes.isNotEmpty()) entry.data else {
                seriesEntries.remove(key)
                null
            }
        }
    }

    /**
     * Para a abertura de um filme: usa imediatamente o objeto preparado;
     * so executa o carregador caso nao tenha resultado valido no cache.
     * Nao espera a fila das outras categorias.
     */
    suspend fun movieOrLoad(
        playlistId: String,
        generationId: String,
        movieId: String,
        loader: suspend () -> MovieDetails?
    ): MovieDetails? = loadMovie(playlistId, generationId, movieId, loader)

    /**
     * Para a abertura de uma serie: reutiliza a lista REAL de episodios,
     * quando ja tiver sido preparada. Nunca inventa temporadas ou episodios.
     */
    suspend fun seriesOrLoad(
        playlistId: String,
        generationId: String,
        seriesId: String,
        loader: suspend () -> SeriesEpisodesResult?
    ): SeriesEpisodesResult? = loadSeries(playlistId, generationId, seriesId, loader)

    /** Aquecimento silencioso para os primeiros titulos e paginas subsequentes. */
    suspend fun prefetchMovie(
        playlistId: String,
        generationId: String,
        movieId: String,
        loader: suspend () -> MovieDetails?
    ) {
        if (getMovie(playlistId, generationId, movieId) != null) return
        backgroundSlots.withPermit {
            withTimeoutOrNull(BACKGROUND_TIMEOUT_MS) {
                try {
                    loadMovie(playlistId, generationId, movieId, loader)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.d(TAG, "Filme nao antecipado: ${error.javaClass.simpleName}")
                }
            }
        }
    }

    /** Nao impede a Home de aparecer nem segura a navegacao pelo controle. */
    suspend fun prefetchSeries(
        playlistId: String,
        generationId: String,
        seriesId: String,
        loader: suspend () -> SeriesEpisodesResult?
    ) {
        if (getSeries(playlistId, generationId, seriesId) != null) return
        backgroundSlots.withPermit {
            // O dono (foco/tela) cancela a tarefa; o clique pode preserva-la.
            // Um prazo separado de 12s cancelaria a consulta que o clique espera.
            try {
                loadSeries(playlistId, generationId, seriesId, loader)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.d(TAG, "Serie nao antecipada: ${error.javaClass.simpleName}")
            }
        }
    }

    private suspend fun loadMovie(
        playlistId: String,
        generationId: String,
        movieId: String,
        loader: suspend () -> MovieDetails?
    ): MovieDetails? {
        if (playlistId.isBlank() || generationId.isBlank() || movieId.isBlank()) return null
        getMovie(playlistId, generationId, movieId)?.let { return it }
        val key = cacheKey(playlistId, generationId, movieId)

        return movieLocks[lockIndex(key)].withLock {
            getMovie(playlistId, generationId, movieId)?.let { return@withLock it }
            val result = withContext(Dispatchers.IO) { loader() }
            if (result != null) {
                synchronized(movieEntries) {
                    movieEntries[key] = CacheEntry(result, SystemClock.elapsedRealtime())
                    while (movieEntries.size > MAX_MOVIES) {
                        val iterator = movieEntries.entries.iterator()
                        if (!iterator.hasNext()) break
                        iterator.next()
                        iterator.remove()
                    }
                }
            }
            result
        }
    }

    private suspend fun loadSeries(
        playlistId: String,
        generationId: String,
        seriesId: String,
        loader: suspend () -> SeriesEpisodesResult?
    ): SeriesEpisodesResult? {
        if (playlistId.isBlank() || generationId.isBlank() || seriesId.isBlank()) return null
        getSeries(playlistId, generationId, seriesId)?.let { return it }
        val key = cacheKey(playlistId, generationId, seriesId)

        return withSeriesLock(key) {
            getSeries(playlistId, generationId, seriesId)?.let { return@withSeriesLock it }
            val result = withContext(Dispatchers.IO) { loader() }
            val estimatedBytes = result?.let(::estimateSeriesBytes) ?: 0L
            if (result != null && result.episodes.isNotEmpty() &&
                result.episodes.size <= MAX_EPISODES_PER_SERIES && estimatedBytes <= MAX_SERIES_ESTIMATED_BYTES) {
                synchronized(seriesEntries) {
                    seriesEntries[key] = CacheEntry(result, SystemClock.elapsedRealtime(), estimatedBytes)
                    // Uma serie longa nao pode ocupar a RAM disponivel para a TV.
                    while (seriesEntries.isNotEmpty() && (
                                seriesEntries.size > MAX_SERIES ||
                                        seriesEntries.values.sumOf { it.data.episodes.size } > MAX_SERIES_EPISODES ||
                                        seriesEntries.values.sumOf { it.estimatedBytes } > MAX_SERIES_ESTIMATED_BYTES
                                )) {
                        val iterator = seriesEntries.entries.iterator()
                        iterator.next()
                        iterator.remove()
                    }
                }
            }
            result
        }
    }

    // Estimativa conservadora de texto/objetos; nao e uma medicao do heap.
    private fun estimateSeriesBytes(data: SeriesEpisodesResult): Long {
        fun text(s: String?) = (s?.length?.toLong() ?: 0L) * 2L + 40L
        return 256L + text(data.seriesId) + text(data.title) + text(data.plot) +
            text(data.backdropUrl) + text(data.posterUrl) + text(data.trailerUrl) +
            data.seasons.sumOf { 96L + text(it.title) + text(it.coverUrl) } +
            data.episodes.sumOf { 160L + text(it.id) + text(it.title) + text(it.thumbnailUrl) + text(it.synopsis) + text(it.containerExtension) }
    }

    /** Libera dados associados a uma lista removida, sem tocar nas outras. */
    fun clearPlaylist(playlistId: String) {
        if (playlistId.isBlank()) return
        val prefix = "$playlistId\u0000"
        synchronized(movieEntries) {
            movieEntries.keys.removeAll { it.startsWith(prefix) }
        }
        synchronized(seriesEntries) {
            seriesEntries.keys.removeAll { it.startsWith(prefix) }
        }
    }
}
