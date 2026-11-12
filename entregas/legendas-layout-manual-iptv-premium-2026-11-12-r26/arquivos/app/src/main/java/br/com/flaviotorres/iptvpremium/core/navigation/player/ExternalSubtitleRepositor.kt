package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import br.com.flaviotorres.iptvpremium.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.SSLException
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

/**
 * Ordem do menu: portugues brasileiro primeiro, portugues generico / Portugal depois.
 * "PT" sem regiao NAO e rotulado como brasileiro.
 */
private fun portugueseSubtitleRank(language: String, releaseName: String): Int =
    when(subtitleLanguageKey(language,releaseName)){"pt-BR"->0;"pt","pt-PT"->1;else->2}

/** Bounded choices: Brazilian Portuguese first, generic Portuguese afterwards. */
private fun portugueseFirst(options: List<ExternalSubtitleOption>): List<ExternalSubtitleOption> =
    options.filter { portugueseSubtitleRank(it.language,it.releaseName)<2 }
        .groupBy { subtitleProviderLanguageKey(it.provider,it.language,it.releaseName) }
        .let { groups -> listOf("pt-BR","pt-PT","pt").flatMap { groups[it].orEmpty().take(30) } }

/** Legendas externas: nenhuma busca fora da tela de reprodução. Sem segundo player. */
data class ExternalSubtitleOption(
    val provider: String,
    val providerId: String,
    val language: String,
    val releaseName: String,
    val downloadUrl: String? = null,
    // Campos opcionais mantêm compatibilidade com chamadas antigas de 5 argumentos.
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val menuIndex: Int = 0,
    val accountScope: String? = null,
    val verifyArchiveEpisode: Boolean = false
) {
    val displayName: String get() {
        if(provider=="Manual")return "Legenda manual — $releaseName"
        val languageName = when(language.lowercase(Locale.ROOT)) {
            "en", "eng" -> "Inglês"
            "es", "spa" -> "Espanhol"
            else -> subtitleLanguageLabel(subtitleProviderLanguageKey(provider,language,releaseName))
        }
        return if (menuIndex > 0) "$languageName — $provider — Opção $menuIndex" else "$languageName — $provider"
    }
}

data class ExternalSubtitleCue(val startMs: Long, val endMs: Long, val text: String)

/** Busca SubDL com chave individual armazenada no dispositivo. Iniciada apenas no player. */
class ExternalSubtitleRepository(private val context: Context,private val playlistId:String?=null) {
    private val manual=ManualSubtitleStore(context)
    private companion object {
        private const val SUBTITLE_CACHE_BYTES = 5L * 1024L * 1024L
        private const val MAX_ONE_SUBTITLE_BYTES = 1024 * 1024L
        private const val SAVED_MEDIA_PREFIX = "downloaded_subtitles_v1_"

        // Uma só LRU de RAM compartilhada entre instâncias do repositório.
        private val subtitleCacheLock = Any()
        private val memoryLru = LinkedHashMap<String, ByteArray>(16, 0.75f, true)
        private var memoryTotalBytes = 0L
    }

    private val savedDirectory = File(context.filesDir, "iptv_subtitles_lru_5mb")
    private val legacyDirectory = File(context.cacheDir, "iptv_external_subtitles")

    // Liga as opções apresentadas no menu ao filme/episódio desta abertura.
    // Só é persistida a associação após o download ser validado.
    private val mediaAssociations = HashMap<String, MutableSet<String>>()
    private val associationsLock = Any()

    private val credentials = SubdlCredentialStore(context)
    private val subsourceCredentials=SubsourceCredentialStore(context)
    private val subsource=SubsourceClient({subsourceCredentials.readKey()},trace={Log.i("SubsourcePerf",it)})
    private val openSubtitles = openSubtitlesClient(context)
    private val tmdbToken = BuildConfig.TMDB_READ_TOKEN.trim()
    private val prefs = context.getSharedPreferences("iptv_external_subtitle_search_subdl_personal_v2", Context.MODE_PRIVATE)

    val configured: Boolean get() = credentials.isConfigured() || openSubtitles.connected() || subsource.scope()!=null || manual.hasAny()

    /** Mensagem de diagnostico. Nunca inclui a chave ou a URL privada do filme. */
    @Volatile var lastSearchStatus: String = "Ainda não pesquisado"
        private set

    fun clearCaches() {
        synchronized(subtitleCacheLock) {
            prefs.edit().clear().apply()
            savedDirectory.deleteRecursively()
            legacyDirectory.deleteRecursively()
            memoryLru.clear()
            memoryTotalBytes = 0L
        }
        synchronized(associationsLock) { mediaAssociations.clear() }
    }
    private fun availableProvider(provider: String): Boolean =
        System.currentTimeMillis() >= prefs.getLong("blocked_until_$provider", 0L)

    private val userAgent = "IPTVPremium/1.0 (subtitle-integration)"

    /** Diagnostico apenas em builds de desenvolvimento: sem chave, token ou URL de video. */
    private fun trace(message: String) {
        if (BuildConfig.DEBUG) Log.d("SubDLTrace", message)
    }

    private fun traceTitle(title: String): String = title.trim()
        .replace(Regex("[\r\n\t]"), " ").take(85)

    /** Search both providers on menu demand. Downloads require explicit selection. */
    suspend fun search(media: PlaybackMedia): List<ExternalSubtitleOption> = withContext(Dispatchers.IO) {
        val manualOptions=manual.options(media,playlistId,br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value)
        if(media.type==PlaybackContentType.LIVE_CHANNEL) {
            lastSearchStatus="Legendas externas indisponíveis em canais ao vivo";return@withContext emptyList()
        }
        val title=(media.subtitleSearchTitle?:media.title).trim()
        if(title.length<2 || (media.type==PlaybackContentType.EPISODE &&
                    (media.seasonNumber==null || media.episodeNumber==null))) {
            lastSearchStatus="Identificação incompleta do conteúdo";return@withContext manualOptions
        }
        val subdlReady=credentials.isConfigured();val openScope=openSubtitles.scope();val sourceScope=subsource.scope()
        if(!subdlReady && openScope==null && sourceScope==null) {
            lastSearchStatus=if(manualOptions.isNotEmpty())"${manualOptions.size} legenda(s) manual(is)"else"Configure um provedor ou envie uma legenda manual";return@withContext manualOptions
        }
        // Independent providers start together; a slow SubDL request no longer delays OpenSubtitles.
        val openRequest=async {
            var status:String?=null
            val options=if(openScope!=null) {
                try {searchOpenSubtitles(media,openScope)}
                catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
                catch(error:OpenSubtitleException){status=error.problem.text;emptyList()}
                catch(_:Exception){status=OpenSubtitleProblem.UNAVAILABLE.text;emptyList()}
            }else emptyList()
            options to status
        }
        val sourceRequest=async {
            var status:String?=null
            val options=if(sourceScope!=null) {
                try{searchSubsource(media,sourceScope)}
                catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
                catch(error:SubsourceException){status=error.text;emptyList()}
                catch(_:Exception){status="SubSource indisponível. Tente novamente.";emptyList()}
            }else emptyList()
            options to status
        }
        val subdlOptions=if(subdlReady) searchSubdlOnly(media) else emptyList()
        val subdlStatus=lastSearchStatus
        val (openOptions,openStatus)=openRequest.await()
        val (sourceOptions,sourceStatus)=sourceRequest.await()
        val accountKey=credentials.readKey()?:openScope?:sourceScope.orEmpty()
        val mediaKey=downloadedMediaKey(media,accountKey,title)
        val ownOpenMediaKey=openScope?.let{downloadedMediaKey(media,it,title)}
        val ownSourceKey=sourceScope?.let{downloadedMediaKey(media,"SubSource:$it",title)}
        val downloaded=savedOptionsForMedia(mediaKey)+ownOpenMediaKey?.takeIf{it!=mediaKey}
            ?.let{savedOptionsForMedia(it)}.orEmpty()+ownSourceKey?.let{savedOptionsForMedia(it)}.orEmpty()
        val currentScope=openSubtitles.scope()
        val currentSourceScope=subsource.scope()
        val merged=(manualOptions+subdlOptions+openOptions+sourceOptions+downloaded)
            .map { it.copy(language=subtitleProviderLanguageKey(it.provider,it.language,it.releaseName)) }
            .filter { option->
                (option.provider=="Manual") ||
                (option.provider=="SubDL" && portugueseSubtitleRank(option.language,option.releaseName)<2) ||
                (option.provider=="OpenSubtitles" && currentScope!=null && option.accountScope==currentScope &&
                    option.language in setOf("pt-BR","pt-PT","en","es")) ||
                (option.provider=="SubSource"&&sourceScope!=null&&currentSourceScope==sourceScope&&option.accountScope==sourceScope&&option.language in setOf("pt-BR","pt-PT","en","es"))
            }
            .distinctBy{"${it.provider}|${it.providerId}|${it.accountScope}"}
            .sortedWith(compareBy<ExternalSubtitleOption>{subtitleLanguagePriority(subtitleLanguageKey(it.language,it.releaseName))}
                .thenBy{subtitleProviderPriority(it.provider)})
            .mapIndexed{index,option->option.copy(menuIndex=index+1)}
        rememberOptionsForMedia(mediaKey,merged)
        if(ownOpenMediaKey!=null && ownOpenMediaKey!=mediaKey)
            rememberOptionsForMedia(ownOpenMediaKey,merged.filter{it.provider=="OpenSubtitles"})
        if(ownSourceKey!=null)rememberOptionsForMedia(ownSourceKey,merged.filter{it.provider=="SubSource"})
        val problems=listOfNotNull(openStatus,sourceStatus).joinToString(" · ")
        lastSearchStatus=when {
            merged.isNotEmpty()->"${merged.size} legenda(s) disponíveis"+(if(problems.isNotBlank())" · $problems"else "")
            problems.isNotBlank()->problems
            openScope!=null||sourceScope!=null->"Nenhuma legenda encontrada nos idiomas consultados"+
                (if(subdlReady)" · $subdlStatus" else "")
            else->subdlStatus
        }
        merged
    }

    /** Uma pesquisa por abertura do player; falhas HTTP/rede nunca viram cache vazio. */
    private suspend fun searchSubdlOnly(media: PlaybackMedia): List<ExternalSubtitleOption> = withContext(Dispatchers.IO) {
        val subdlKey = credentials.readKey()
        if (subdlKey == null) {
            lastSearchStatus = "SubDL não conectado"
            return@withContext emptyList()
        }
        if (media.type == PlaybackContentType.LIVE_CHANNEL) {
            lastSearchStatus = "Indisponível em canais ao vivo"
            return@withContext emptyList()
        }
        val title = (media.subtitleSearchTitle ?: media.title).trim()
        if (title.length < 2) {
            lastSearchStatus = "Título insuficiente"
            return@withContext emptyList()
        }
        if (media.type == PlaybackContentType.EPISODE &&
            (media.seasonNumber == null || media.episodeNumber == null)) {
            lastSearchStatus = "Temporada/episódio ausente"
            return@withContext emptyList()
        }
        trace("start type=${media.type} title='${traceTitle(title)}' " +
                "year=${media.year ?: "-"} tmdbInput=${media.tmdbId?.takeIf { it.matches(Regex("\\d+")) } ?: "none"} " +
                "season=${media.seasonNumber ?: "-"} episode=${media.episodeNumber ?: "-"}")
        // Só encerrar na coleção local quando os dois grupos regionais estiverem disponíveis.
        // Uma legenda BR salva não deve impedir a descoberta de opções de Portugal.
        val mediaKey = downloadedMediaKey(media, subdlKey, title)
        val offline = portugueseFirst(savedOptionsForMedia(mediaKey).filter { it.provider == "SubDL" })
        if (offline.any{subtitleProviderLanguageKey(it.provider,it.language,it.releaseName)=="pt-BR"} &&
            offline.any{subtitleProviderLanguageKey(it.provider,it.language,it.releaseName)=="pt-PT"}) {
            rememberOptionsForMedia(mediaKey, offline)
            trace("cache source=downloaded-5mb options=${offline.size} apiRequests=0")
            lastSearchStatus = "${offline.size} legenda(s) salva(s) · cache local"
            return@withContext offline
        }

        if (!availableProvider("subdl")) {
            trace("skip reason=provider-temporarily-blocked")
            lastSearchStatus = "SubDL: limite temporário"
            return@withContext emptyList()
        }

        // v4 invalida SOMENTE cache de PESQUISA anterior, sem apagar cache SRT.
        val cacheKey = digest("smart_ptbr_title_v5|${digest(subdlKey)}|${media.type}|${media.tmdbId}|$title|${media.year}|${media.seasonNumber}|${media.episodeNumber}")
        val cached = prefs.getString(cacheKey, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val now = System.currentTimeMillis()
        if (cached != null && cached.optLong("until") > now) {
            val options = portugueseFirst(decodeOptions(cached.optJSONArray("options") ?: JSONArray()))
            rememberOptionsForMedia(mediaKey, options)
            trace("cache source=search options=${options.size} apiRequests=0")
            lastSearchStatus = if (options.isEmpty()) "Sem resultados (cache)" else "${options.size} legenda(s) (cache)"
            return@withContext options
        }

        // Guardar titulos originais apenas na UI; consultar APIs com nome limpo.
        val cleanTitle = cleanIptvTitle(title)
        val discoveryName = discoveryTitle(cleanTitle)
        val wantedYear = media.year ?: yearFromIptvTitle(title)
        trace("identity cleanedTitle='${traceTitle(cleanTitle)}' discovery='${traceTitle(discoveryName)}' " +
                "wantedYear=${wantedYear ?: "-"}")
        var lookupLabel = "nome"
        var uncertainResponse = false
        val options = try {
            val playerTmdb = media.tmdbId?.trim()?.takeIf { it.matches(Regex("\\d+")) }
            val tmdbFromSearch = if (playerTmdb == null)
                resolveTmdb(media, cleanTitle, wantedYear) else null
            // Quando a playlist nao traz TMDB ou o TMDB nao identifica a obra,
            // o indice de titulos do SubDL pode fornecer sd_id e tmdb_id verificados.
            val catalog = if (playerTmdb == null && tmdbFromSearch == null)
                findIdentityBySubdlTitle(media, cleanTitle, wantedYear, subdlKey) else null
            val tmdb = playerTmdb ?: tmdbFromSearch ?: catalog?.tmdbId
            val identitySource = when {
                playerTmdb != null -> "player"
                tmdbFromSearch != null -> "TMDB-title"
                catalog != null -> "SubDL-catalog"
                else -> "unresolved"
            }
            lookupLabel = when {
                tmdb != null -> "TMDB $tmdb"
                catalog != null -> "SubDL sd_id"
                else -> "título limpo"
            }
            trace("identity resolvedTmdb=${tmdb ?: "none"} source=$identitySource " +
                    "sdIdVerified=${catalog != null}")

            val primary = try {
                searchSubdl(media, catalog?.englishTitle ?: discoveryName, tmdb, subdlKey,
                    sdId = if (tmdb == null) catalog?.sdId else null,
                    allowUndocumentedEmptyResponse = tmdb == null && catalog == null)
            } catch (_: SubdlTitleNotFoundException) {
                uncertainResponse = true
                trace("response titleLookupRejected=true fallback=enabled")
                emptyList()
            }
            if (primary.isNotEmpty()) {
                primary
            } else if (!availableProvider("subdl")) {
                emptyList()
            } else {
                val english = if (catalog != null) EnglishIdentity(
                    catalog.englishTitle, catalog.originalTitle, catalog.year
                ) else loadEnglishIdentity(media, tmdb, discoveryName)
                lookupLabel = "TMDB + inglês/ano"
                trace("fallback englishTitle='${traceTitle(english.englishTitle)}' " +
                        "year=${english.year ?: "-"} tmdb=${tmdb ?: "none"}")
                // Evitar buscar o MESMO sd_id usado na consulta anterior.
                val sdId = if (catalog != null && tmdb == null) null
                else findVerifiedSubdlId(media, tmdb, english, subdlKey)
                if (sdId != null && availableProvider("subdl") && sdId != catalog?.sdId) {
                    lookupLabel = "inglês/ano + SubDL"
                    trace("fallback sdIdVerified=true")
                    searchSubdl(media, english.englishTitle, null, subdlKey, sdId)
                } else {
                    trace("fallback sdIdVerified=false blocked=${!availableProvider("subdl")}")
                    emptyList()
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: SubdlHttpException) {
            trace("error kind=http status=${error.status} stage=$lookupLabel")
            lastSearchStatus = when (error.status) {
                400 -> "SubDL: parâmetros inválidos (HTTP 400)"
                401 -> "SubDL: chave recusada (HTTP 401)"
                402, 403 -> "SubDL: acesso negado (HTTP ${error.status})"
                408 -> "SubDL: tempo esgotado (HTTP 408)"
                413 -> "SubDL: resposta excede o limite (413)"
                429 -> "SubDL: limite atingido (HTTP 429)"
                in 500..599 -> "SubDL: servidor indisponível (HTTP ${error.status})"
                else -> "SubDL: erro HTTP ${error.status}"
            }
            return@withContext emptyList()
        } catch (error: SubdlResponseException) {
            trace("error kind=response stage=$lookupLabel reason=${error.safeReason}")
            lastSearchStatus = "SubDL ($lookupLabel): ${error.safeReason}"
            return@withContext emptyList()
        } catch (_: JSONException) {
            trace("error kind=invalid-json stage=$lookupLabel")
            lastSearchStatus = "SubDL: resposta JSON inválida"
            return@withContext emptyList()
        } catch (_: SocketTimeoutException) {
            trace("error kind=timeout stage=$lookupLabel")
            lastSearchStatus = "SubDL: conexão ou leitura expirou"
            return@withContext emptyList()
        } catch (_: UnknownHostException) {
            trace("error kind=dns stage=$lookupLabel")
            lastSearchStatus = "SubDL: DNS ou internet indisponível"
            return@withContext emptyList()
        } catch (_: SSLException) {
            trace("error kind=ssl stage=$lookupLabel")
            lastSearchStatus = "SubDL: erro de certificado HTTPS"
            return@withContext emptyList()
        } catch (_: IOException) {
            trace("error kind=io stage=$lookupLabel")
            lastSearchStatus = "SubDL: falha de conexão ou leitura"
            return@withContext emptyList()
        } catch (_: SecurityException) {
            trace("error kind=security stage=$lookupLabel")
            lastSearchStatus = "SubDL: acesso de rede bloqueado"
            return@withContext emptyList()
        } catch (error: Exception) {
            trace("error kind=${error.javaClass.simpleName} stage=$lookupLabel")
            // Nunca expõe tokens, URLs ou dados privados na mensagem exibida.
            lastSearchStatus = "SubDL: erro interno (${error.javaClass.simpleName})"
            return@withContext emptyList()
        }
        val preferredOptions = portugueseFirst(options)
        trace("complete stage=$lookupLabel raw=${options.size} accepted=${preferredOptions.size} " +
                "ptBR=${preferredOptions.count { portugueseSubtitleRank(it.language, it.releaseName) == 0 }} " +
                "pt=${preferredOptions.count { portugueseSubtitleRank(it.language, it.releaseName) == 1 }}")
        rememberOptionsForMedia(mediaKey, preferredOptions)
        lastSearchStatus = if (preferredOptions.isEmpty()) {
            "SubDL ($lookupLabel): sem legendas"
        } else {
            "${preferredOptions.size} legenda(s) em português · $lookupLabel"
        }
        val entries = JSONArray()
        preferredOptions.forEach { option ->
            entries.put(JSONObject().put("p", option.provider).put("id", option.providerId)
                .put("lang", option.language).put("name", option.releaseName)
                .put("url", option.downloadUrl))
        }
        // Uma consulta HTTP 200 rejeitada sem confirmacao de "sem resultados"
        // nao pode gerar cache NEGATIVO. Assim a correcao futura funciona ao reabrir.
        if (preferredOptions.isNotEmpty() || !uncertainResponse) {
            val expires = now + if (preferredOptions.isEmpty()) 30 * 60_000L else 12 * 60 * 60_000L
            prefs.edit().putString(cacheKey, JSONObject().put("until", expires)
                .put("options", entries).toString()).apply()
        } else {
            trace("cache negative=skipped reason=unconfirmed-rejection")
        }
        preferredOptions
    }

    private suspend fun searchOpenSubtitles(media:PlaybackMedia,scope:String):List<ExternalSubtitleOption> {
        val title=(media.subtitleSearchTitle?:media.title).trim()
        val clean=cleanIptvTitle(title).replace(Regex("^\\d{1,2}\\s*[-.)]\\s+(?=\\p{L})"),"")
        val year=(media.year?:yearFromIptvTitle(title))?.takeIf{it in 1888..2100}
        val key=digest("open_search_v4|$scope|${media.type}|$clean|$year|${media.tmdbId}|"+
            "${media.seasonNumber}|${media.episodeNumber}|pt-br,pt-pt,en,es")
        val cached=prefs.getString(key,null)?.let{runCatching{JSONObject(it)}.getOrNull()}
        if(cached!=null && cached.optLong("until")>System.currentTimeMillis()) {
            openSubtitles.checkScope(scope)
            return decodeOptions(cached.optJSONArray("options")?:JSONArray()).filter{
                it.provider=="OpenSubtitles" && it.accountScope==scope}
        }
        val tmdb=media.tmdbId?.takeIf{it.toLongOrNull()?.let{id->id>0}==true}
            ?:resolveTmdb(media,clean,year)
        // The exact ID is sufficient; avoid another metadata request before subtitle search.
        val query=OpenSubtitleQuery(if(tmdb!=null)clean else discoveryTitle(clean),year,tmdb,
            if(media.type==PlaybackContentType.EPISODE)media.seasonNumber else null,
            if(media.type==PlaybackContentType.EPISODE)media.episodeNumber else null)
        val (responseScope,hits)=openSubtitles.search(query)
        if(responseScope!=scope)throw OpenSubtitleException(OpenSubtitleProblem.SESSION_EXPIRED)
        val options=hits.map{hit->ExternalSubtitleOption("OpenSubtitles",hit.fileId,hit.language,hit.release,
            seasonNumber=media.seasonNumber,episodeNumber=media.episodeNumber,accountScope=scope)}
        openSubtitles.checkScope(scope)
        prefs.edit().putString(key,JSONObject().put("until",System.currentTimeMillis()+
            if(options.isEmpty())600_000L else 1_800_000L)
            .put("options",JSONArray().apply{options.forEach{put(encodeOption(it))}}).toString()).apply()
        return options
    }

    private suspend fun searchSubsource(media:PlaybackMedia,scope:String):List<ExternalSubtitleOption> {
        val title=cleanIptvTitle(media.subtitleSearchTitle?:media.title)
        val year=(media.year?:yearFromIptvTitle(media.title))?.takeIf{it in 1888..2100}
        val key="subsource_search_v4_"+digest("subsource_search_v4|$scope|${media.type}|$title|$year|${media.tmdbId}|${media.seasonNumber}|${media.episodeNumber}")
        val cached=prefs.getString(key,null)?.let{runCatching{JSONObject(it)}.getOrNull()}
        if(cached!=null&&cached.optLong("until")>System.currentTimeMillis()) {
            subsource.checkScope(scope)
            return decodeOptions(cached.optJSONArray("options")?:JSONArray()).filter{it.provider=="SubSource"&&it.accountScope==scope}
        }
        val tmdb=media.tmdbId?.takeIf{it.toLongOrNull()?.let{id->id>0}==true}?:resolveTmdb(media,title,year)
        val kind=if(media.type==PlaybackContentType.EPISODE)"tv"else"movie"
        val imdb=if(tmdb!=null&&tmdbToken.isNotBlank()) {
            val cacheKey="subsource_imdb_${kind}_$tmdb"
            prefs.getString(cacheKey,null)?.takeIf{it.matches(Regex("tt[0-9]{5,12}"))}?:run {
                val response=try{fetch("https://api.themoviedb.org/3/$kind/$tmdb/external_ids",mapOf("Authorization" to "Bearer $tmdbToken"),400_000)}
                    catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(_:Exception){null}
                if(response?.code!=200)null else runCatching{JSONObject(String(response.data,Charsets.UTF_8))}.getOrNull()
                    ?.takeIf{it.optString("id")==tmdb}?.optString("imdb_id")?.takeIf{it.matches(Regex("tt[0-9]{5,12}"))}
                    ?.also{prefs.edit().putString(cacheKey,it).apply()}
            }
        }else null
        val english=if(imdb==null)loadEnglishIdentity(media,tmdb,discoveryTitle(title))else null
        val query=SubsourceQuery(english?.englishTitle?.ifBlank{title}?:title,english?.year?:year,imdb,
            if(media.type==PlaybackContentType.EPISODE)media.seasonNumber else null,
            if(media.type==PlaybackContentType.EPISODE)media.episodeNumber else null)
        val(returned,hits)=subsource.search(query)
        if(returned!=scope)throw SubsourceException("A conta SubSource mudou durante a pesquisa.")
        val options=hits.map{ExternalSubtitleOption("SubSource",it.id,it.language,it.release,seasonNumber=media.seasonNumber,episodeNumber=media.episodeNumber,accountScope=scope,verifyArchiveEpisode=it.seasonPack)}
        subsource.checkScope(scope)
        val edit=prefs.edit()
        prefs.all.entries.filter{(it.key.startsWith("subsource_search_v1_")||it.key.startsWith("subsource_search_v2_")||it.key.startsWith("subsource_search_v3_")||it.key.startsWith("subsource_search_v4_"))&&it.key!=key}
            .sortedByDescending{runCatching{JSONObject(it.value as? String?:"{}").optLong("until")}.getOrDefault(0)}
            .drop(31).forEach{edit.remove(it.key)}
        edit.putString(key,JSONObject().put("until",System.currentTimeMillis()+if(options.isEmpty())600_000L else 1_800_000L)
            .put("options",JSONArray().apply{options.forEach{put(encodeOption(it))}}).toString()).apply()
        return options
    }

    /** Download apenas após escolha explícita; serve do cache de 5 MB primeiro. */
    suspend fun download(option: ExternalSubtitleOption): List<ExternalSubtitleCue> = withContext(Dispatchers.IO) {
        if(option.provider=="Manual")return@withContext manual.read(option.providerId)?.let{ManualSubtitleArchive.cues(it)}.orEmpty()
        if(option.provider=="OpenSubtitles")openSubtitles.checkScope(option.accountScope
            ?:throw OpenSubtitleException(OpenSubtitleProblem.SESSION_EXPIRED))
        if(option.provider=="SubSource")subsource.checkScope(option.accountScope?:throw SubsourceException("SubSource não configurado."))
        val key = downloadedOptionKey(option)
        readSavedBytes(key)?.let { savedBytes ->
            val cues = parsedSubtitle(savedBytes,option.provider,"CACHE")
            if (cues.isNotEmpty()) {
                recordDownloadedOption(option)
                return@withContext cues
            }
            // Nunca reutilizar um arquivo corrompido.
            removeSavedFile(key)
        }

        if(option.provider=="OpenSubtitles") {
            val bytes=openSubtitles.download(option.providerId,option.accountScope!!)
            val unpacked=unpackSubtitle(bytes,option.seasonNumber,option.episodeNumber)?:return@withContext emptyList()
            val cues=parsedSubtitle(unpacked,option.provider,"DOWNLOAD")
            openSubtitles.checkScope(option.accountScope)
            if(cues.isNotEmpty() && unpacked.size.toLong()<=MAX_ONE_SUBTITLE_BYTES && saveSubtitleBytes(key,unpacked))
                recordDownloadedOption(option)
            return@withContext cues
        }
        if(option.provider=="SubSource") {
            val bytes=subsource.download(option.providerId,option.accountScope!!)
            val unpacked=SubsourceArchive.select(bytes,option.seasonNumber,option.episodeNumber,option.verifyArchiveEpisode)?:return@withContext emptyList()
            val cues=parsedSubtitle(unpacked,option.provider,"DOWNLOAD")
            subsource.checkScope(option.accountScope)
            if(cues.isNotEmpty()&&unpacked.size.toLong()<=MAX_ONE_SUBTITLE_BYTES&&saveSubtitleBytes(key,unpacked))recordDownloadedOption(option)
            return@withContext cues
        }
        if (option.provider != "SubDL") return@withContext emptyList()
        val subdlKey = credentials.readKey() ?: return@withContext emptyList()
        val uri = option.downloadUrl?.takeIf { safeUrl(it, "subdl.com") }
            ?: "https://api.subdl.com/api/v2/subtitles/${Uri.encode(option.providerId)}/download?format=zip"
        // Nao enviar Bearer ao host publico de download.
        val headers = if (Uri.parse(uri).host == "api.subdl.com")
            mapOf("Authorization" to "Bearer $subdlKey") else emptyMap()
        val response = fetch(uri, headers, 3_000_000, true)
        if (response.code !in 200..299) return@withContext emptyList()
        val bytes = unpackSubtitle(response.data) ?: return@withContext emptyList()
        val cues = parsedSubtitle(bytes,option.provider,"DOWNLOAD")
        if (cues.isNotEmpty() && bytes.size.toLong() <= MAX_ONE_SUBTITLE_BYTES) {
            if (saveSubtitleBytes(key, bytes)) recordDownloadedOption(option)
        }
        cues
    }

    /** Chave inclui conta, obra, temporada e episódio. Nunca contém URLs privadas. */
    private fun downloadedMediaKey(media: PlaybackMedia, key: String, title: String): String =
        digest("saved_v1|${digest(key)}|${media.type}|${media.tmdbId}|" +
                "${normalizedTitle(title)}|${media.year}|${media.seasonNumber}|${media.episodeNumber}")

    private fun downloadedOptionKey(option: ExternalSubtitleOption): String =
        digest(if(option.provider=="SubSource") "SubSource:v2|${option.providerId}|${option.accountScope}|${option.seasonNumber}|${option.episodeNumber}"
            else if(option.provider=="OpenSubtitles") "${option.provider}|${option.providerId}|${option.accountScope}"
            else "${option.provider}|${option.providerId}|${option.downloadUrl}")

    private fun encodeOption(option: ExternalSubtitleOption): JSONObject = JSONObject()
        .put("p", option.provider).put("id", option.providerId)
        .put("lang", option.language).put("name", option.releaseName)
        .put("url", option.downloadUrl)
        .put("season", option.seasonNumber ?: 0).put("episode", option.episodeNumber ?: 0)
        .put("account",option.accountScope).put("verifyArchiveEpisode",option.verifyArchiveEpisode)

    /** Apenas as opções do conteúdo sendo reproduzido recebem associação. */
    private fun rememberOptionsForMedia(mediaKey: String, options: List<ExternalSubtitleOption>) {
        synchronized(associationsLock) {
            for (option in options) {
                mediaAssociations.getOrPut(downloadedOptionKey(option)) { HashSet() }.add(mediaKey)
            }
        }
        // Recupera inclusive legendas baixadas pela versão anterior do app.
        for (option in options) {
            if (hasSavedSubtitle(downloadedOptionKey(option))) {
                recordDownloadedOption(option)
            }
        }
    }

    /** Guarda a relação filme/episódio -> legenda somente após download real. */
    private fun recordDownloadedOption(option: ExternalSubtitleOption) {
        val optionKey = downloadedOptionKey(option)
        val mediaKeys = synchronized(associationsLock) {
            mediaAssociations[optionKey]?.toList().orEmpty()
        }
        if (mediaKeys.isEmpty()) return

        synchronized(subtitleCacheLock) {
            if (!savedFile(optionKey).isFile) return
            for (mediaKey in mediaKeys) {
                val prefKey = SAVED_MEDIA_PREFIX + mediaKey
                val previous = prefs.getString(prefKey, null)
                    ?.let { runCatching { JSONArray(it) }.getOrNull() } ?: JSONArray()
                val found = decodeOptions(previous).any { downloadedOptionKey(it) == optionKey }
                if (!found) previous.put(encodeOption(option))
                prefs.edit().putString(prefKey, previous.toString()).apply()
            }
        }
    }

    /** Carrega primeiro a legenda já baixada, mesmo offline ou após reiniciar. */
    private fun savedOptionsForMedia(mediaKey: String): List<ExternalSubtitleOption> {
        val prefKey = SAVED_MEDIA_PREFIX + mediaKey
        val entries = prefs.getString(prefKey, null)
            ?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return emptyList()
        val candidates = decodeOptions(entries)
        val valid = candidates.filter { hasSavedSubtitle(downloadedOptionKey(it)) }
        if (valid.size != candidates.size) {
            if (valid.isEmpty()) prefs.edit().remove(prefKey).apply()
            else prefs.edit().putString(prefKey, JSONArray().apply {
                valid.forEach { put(encodeOption(it)) }
            }.toString()).apply()
        }
        return valid
    }

    private fun savedFile(key: String): File = File(savedDirectory, "$key.srt")

    private fun hasSavedSubtitle(key: String): Boolean = synchronized(subtitleCacheLock) {
        val file = savedFile(key)
        if (!file.isFile || file.length() !in 1L..MAX_ONE_SUBTITLE_BYTES) return@synchronized false
        file.setLastModified(System.currentTimeMillis())
        true
    }

    /** LRU em RAM (5 MB) + cópia privada persistente (5 MB). */
    private fun readSavedBytes(key: String): ByteArray? = synchronized(subtitleCacheLock) {
        val file = savedFile(key)
        if (file.isFile && file.length() in 1L..MAX_ONE_SUBTITLE_BYTES) {
            file.setLastModified(System.currentTimeMillis())
            memoryLru[key]?.let { return@synchronized it }
            val data = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context).retain(file).use {
                runCatching { file.readBytes() }.getOrNull()
            }
            if (data != null && data.isNotEmpty()) {
                putMemory(key, data)
                return@synchronized data
            }
        }

        // Migração de SRTs anteriores, que só ficavam no cache temporário.
        val old = File(legacyDirectory, "$key.srt")
        if (old.isFile && old.length() in 1L..MAX_ONE_SUBTITLE_BYTES) {
            val previous = runCatching { old.readBytes() }.getOrNull()
            if (previous != null && previous.isNotEmpty() && saveSubtitleBytes(key, previous)) {
                old.delete()
                return@synchronized previous
            }
        }
        null
    }

    private fun putMemory(key: String, data: ByteArray) {
        memoryLru.remove(key)?.let { memoryTotalBytes -= it.size.toLong() }
        if (data.size.toLong() > SUBTITLE_CACHE_BYTES) return
        memoryLru[key] = data
        memoryTotalBytes += data.size.toLong()
        while (memoryTotalBytes > SUBTITLE_CACHE_BYTES) {
            val eldest = memoryLru.entries.iterator()
            if (!eldest.hasNext()) break
            val entry = eldest.next()
            memoryTotalBytes -= entry.value.size.toLong()
            eldest.remove()
        }
    }

    /** Escrita atômica antes de eliminar o arquivo mais antigo. */
    private fun saveSubtitleBytes(key: String, data: ByteArray): Boolean = synchronized(subtitleCacheLock) {
        if (data.isEmpty() || data.size.toLong() > MAX_ONE_SUBTITLE_BYTES) return@synchronized false
        if (!savedDirectory.isDirectory && !savedDirectory.mkdirs()) return@synchronized false
        val target = savedFile(key)
        val reservation = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
            .reserve(target, data.size.toLong()) ?: return@synchronized false
        val temporary = reservation.temporary
        val success = runCatching {
            FileOutputStream(temporary).use { output ->
                output.write(data)
                output.fd.sync()
            }
            if (target.exists() && !target.delete()) return@runCatching false
            if (!temporary.renameTo(target)) return@runCatching false
            target.setLastModified(System.currentTimeMillis())
            putMemory(key, data)
            pruneSavedDirectory()
            true
        }.getOrDefault(false)
        if (!success) temporary.delete()
        reservation.close()
        success
    }

    private fun removeSavedFile(key: String) = synchronized(subtitleCacheLock) {
        savedFile(key).delete()
        memoryLru.remove(key)?.let { memoryTotalBytes -= it.size.toLong() }
    }

    /** Descarta LRU antiga; nunca ultrapassa 5 MiB de arquivos de legenda. */
    private fun pruneSavedDirectory() {
        val files = savedDirectory.listFiles()?.filter {
            it.isFile && it.name.endsWith(".srt")
        }?.sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name }).orEmpty()
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= br.com.flaviotorres.iptvpremium.core.storage.CacheStorageLimits.SUBTITLES) break
            val size = file.length()
            if (file.delete()) {
                total -= size
                val key = file.name.removeSuffix(".srt")
                memoryLru.remove(key)?.let { memoryTotalBytes -= it.size.toLong() }
            }
        }
        // Metadados de legendas eliminadas são descartados ao próximo search().
    }

    private fun decodeOptions(array: JSONArray): List<ExternalSubtitleOption> = buildList {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val provider = obj.optString("p")
            if (id.isNotBlank() && provider.isNotBlank()) add(ExternalSubtitleOption(
                provider, id, obj.optString("lang", "und"), obj.optString("name", "Legenda"),
                obj.optString("url").takeIf { it.startsWith("https://") },
                obj.optInt("season", 0).takeIf { it > 0 },
                obj.optInt("episode", 0).takeIf { it > 0 },
                accountScope=obj.optString("account").takeIf{it.isNotBlank() && it!="null"},
                verifyArchiveEpisode=obj.optBoolean("verifyArchiveEpisode",false)
            ))
        }
    }

    /** Remove apenas indicadores comuns da playlist; nunca altera titulo exibido no player. */
    private fun cleanIptvTitle(raw: String): String {
        var title = raw.trim()
        // Marcadores tecnicos em colchetes/parenteses; nao remover dados da obra.
        title = title.replace(Regex(
            "(?i)\\s*[\\[(]\\s*(?:4k|uhd|fhd|hdr(?:10\\+?)?|dv|dolby[ ._-]*vision|" +
                    "1080p|2160p|720p|web[ ._-]*dl|webrip|bluray|x265|hevc)" +
                    "(?:[ ._+,-]*(?:4k|uhd|fhd|hdr(?:10\\+?)?|dv|dolby[ ._-]*vision|" +
                    "1080p|2160p|720p|web[ ._-]*dl|webrip|bluray|x265|hevc))*\\s*[\\])]"
        ), " ")
        title = title.replace(Regex(
            "(?i)(?:\\s+|[ ._-]+)(?:4k|2160p|1080p|720p|uhd|fhd|hdr(?:10\\+?)?|" +
                    "dv|dolby[ ._-]*vision|web[ ._-]*dl|webrip|x265|hevc)(?=\\s|$)"
        ), " ")
        title = title.replace(Regex("(?:\\s*[\\[(]?(?:19|20)\\d{2}[\\])]?)\\s*$"), " ")
        return title.replace(Regex("\\s+"), " ").trim(' ', '-', '_', '.', '|', ':')
            .ifBlank { raw.trim() }
    }

    /** Retira somente subtitulos genericos traduzidos: ' - O Filme' / 'The Movie'. */
    private fun discoveryTitle(raw: String): String {
        val clean = cleanIptvTitle(raw)
        val base = clean.replace(Regex(
            "(?i)(?:\\s*[-:|]\\s*|\\s+)(?:(?:o|a)\\s+filme|the\\s+movie|" +
                    "le\\s+film|il\\s+film)\\s*$"
        ), "").trim()
        return base.takeIf { it.length >= 3 } ?: clean
    }

    /** Compara nomes localizados com seguranca, sem usar coincidencia parcial. */
    private fun workTitleKey(raw: String): String =
        normalizedTitle(discoveryTitle(raw))

    private fun yearFromIptvTitle(raw: String): Int? {
        val withoutQuality = raw
            .replace(Regex("(?i)\\s*[\\[(](?:4k|2160p|1080p|hdr(?:10\\+?)?|dv|uhd)[\\])]"), " ")
            .replace(Regex("(?i)\\s+(?:4k|2160p|1080p|hdr(?:10\\+?)?|dv|uhd)(?=\\s|$)"), " ")
            .trim()
        // Nao interpretar o proprio titulo de um filme como "1917" como ano.
        val year = Regex("(?:[\\[(]|\\s)((?:19|20)\\d{2})(?:[\\])])?\\s*$")
            .find(withoutQuality)?.groupValues?.get(1)?.toIntOrNull()
        return year?.takeIf { it in 1888..2100 }
    }

    private data class VerifiedSubdlIdentity(
        val sdId: String,
        val tmdbId: String?,
        val englishTitle: String,
        val originalTitle: String,
        val year: Int?
    )

    /**
     * Consulta o catalogo oficial do SubDL quando a playlist nao traz TMDB.
     * Exige nome equivalente e ano exato, se presente; homonimos ambiguos sao ignorados.
     * Ex.: 'Matchbox - O Filme 4K [DV][HDR]' -> 'Matchbox' -> titulo/ano verificados.
     */
    private suspend fun findIdentityBySubdlTitle(
        media: PlaybackMedia, cleanTitle: String, wantedYear: Int?, subdlKey: String
    ): VerifiedSubdlIdentity? {
        val query = discoveryTitle(cleanTitle)
        val expectedKey = workTitleKey(cleanTitle)
        if (expectedKey.length < 3) return null
        val kind = if (media.type == PlaybackContentType.EPISODE) "tv" else "movie"
        val cacheKey = "subdl_catalog_identity_v1_${digest("$kind|$wantedYear|$expectedKey")}"
        val saved = prefs.getString(cacheKey, null)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (saved != null && saved.optLong("until") > System.currentTimeMillis()) {
            val id = saved.optString("sd_id")
            if (safeSubdlId(id)) return VerifiedSubdlIdentity(
                id, saved.optString("tmdb_id").takeIf { it.matches(Regex("\\d+")) },
                saved.optString("en").ifBlank { query },
                saved.optString("original").ifBlank { query },
                saved.optInt("year", 0).takeIf { it in 1888..2100 }
            ).also { trace("identity catalogCache=verified tmdb=${it.tmdbId ?: "none"}") }
        }
        trace("identity catalogQuery='${traceTitle(query)}' wantedYear=${wantedYear ?: "-"}")
        val response = fetch(
            "https://api.subdl.com/api/v2/movies/search?q=${Uri.encode(query)}&type=$kind&limit=20",
            mapOf("Authorization" to "Bearer $subdlKey", "Accept" to "application/json"),
            400_000
        )
        if (response.code != 200) throw SubdlHttpException(response.code)
        val payload = JSONObject(String(response.data, Charsets.UTF_8))
        if (!payload.optBoolean("status", true)) {
            val reason = payload.optString("message").ifBlank { payload.optString("error") }
            if (!isMissingSubtitles("", reason)) {
                throw SubdlResponseException(safeSubdlError("", reason))
            }
            return null
        }
        val results = payload.optJSONArray("results") ?: return null
        val verified = ArrayList<VerifiedSubdlIdentity>()
        for (i in 0 until minOf(results.length(), 20)) {
            val item = results.optJSONObject(i) ?: continue
            val id = item.optString("sd_id").trim()
            if (!safeSubdlId(id)) continue
            val resultType = item.optString("type").trim().lowercase(Locale.ROOT)
            if (resultType.isNotBlank() && resultType != kind) continue
            val name = item.optString("name").trim()
            val original = item.optString("original_name").trim().ifBlank { name }
            if (workTitleKey(name) != expectedKey && workTitleKey(original) != expectedKey) continue
            val resultYear = item.optInt("year", 0).takeIf { it in 1888..2100 }
            // Com ano informado, so aceitar coincidencia exata; sem ano,
            // exigir nome exato e um unico resultado.
            if (wantedYear != null && resultYear != wantedYear) continue
            val tmdbId = item.optString("tmdb_id").trim().takeIf { it.matches(Regex("\\d+")) }
            verified += VerifiedSubdlIdentity(
                id, tmdbId, name.ifBlank { query }, original,
                resultYear ?: wantedYear
            )
        }
        val unique = verified.distinctBy { it.sdId }
        val selected = unique.singleOrNull()
        trace("identity catalogResults=${results.length()} verified=${unique.size} " +
                "selected=${selected != null} tmdb=${selected?.tmdbId ?: "none"}")
        if (selected != null) prefs.edit().putString(cacheKey, JSONObject()
            .put("until", System.currentTimeMillis() + 7L * 24 * 60 * 60_000)
            .put("sd_id", selected.sdId).put("tmdb_id", selected.tmdbId ?: "")
            .put("en", selected.englishTitle).put("original", selected.originalTitle)
            .put("year", selected.year ?: 0).toString()).apply()
        return selected
    }

    /** Pesquisa TMDB apenas com nomes tratados e exige ano/titulo coerente. */
    private suspend fun resolveTmdb(
        media: PlaybackMedia, title: String, wantedYear: Int?
    ): String? {
        if (tmdbToken.isBlank()) {
            trace("identity tmdbLookup=unavailable (no configured token)")
            return null
        }
        val type = if (media.type == PlaybackContentType.EPISODE) "tv" else "movie"
        val wantedKey = workTitleKey(title)
        val candidates = listOf(cleanIptvTitle(title), discoveryTitle(title))
            .filter { it.length >= 3 }.distinct()
        for (query in candidates) {
            val path = "https://api.themoviedb.org/3/search/$type?query=${Uri.encode(query)}&language=pt-BR"
            val response = try {
                fetch(path, mapOf("Authorization" to "Bearer $tmdbToken"), 400_000)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                trace("identity tmdbLookup=network-error")
                return null
            }
            trace("identity tmdbQuery='${traceTitle(query)}' http=${response.code}")
            if (response.code != 200) continue
            val results = runCatching {
                JSONObject(String(response.data, Charsets.UTF_8)).optJSONArray("results")
            }.getOrNull() ?: continue
            val matches = ArrayList<String>()
            for (i in 0 until minOf(results.length(), 20)) {
                val result = results.optJSONObject(i) ?: continue
                val name = result.optString(if (type == "tv") "name" else "title")
                val original = result.optString(if (type == "tv") "original_name" else "original_title")
                if (workTitleKey(name) != wantedKey && workTitleKey(original) != wantedKey) continue
                val date = result.optString(if (type == "tv") "first_air_date" else "release_date")
                val year = date.take(4).toIntOrNull()
                if (wantedYear != null && year != wantedYear) continue
                val id = result.optLong("id")
                if (id > 0) matches += id.toString()
            }
            val unique = matches.distinct()
            trace("identity tmdbCandidates=${results.length()} verified=${unique.size}")
            if (unique.size == 1) return unique.first()
            // Ambiguidade exige busca posterior no catalogo SubDL, nao escolher a esmo.
        }
        return null
    }
    private fun normalizedTitle(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("\\s*\\(\\d{4}\\)\\s*"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    /** Título internacional e ano verificados; nunca usar o nome de um episódio. */
    private data class EnglishIdentity(
        val englishTitle: String,
        val originalTitle: String,
        val year: Int?
    )

    /**
     * Detalhes TMDB são buscados somente após o SubDL não devolver legendas.
     * A API en-US costuma trazer o nome internacional, mas preservamos o original.
     */
    private suspend fun loadEnglishIdentity(
        media: PlaybackMedia, tmdbId: String?, fallbackTitle: String
    ): EnglishIdentity {
        val fallback = EnglishIdentity(fallbackTitle, fallbackTitle, media.year?.takeIf { it in 1888..2100 })
        if (tmdbId == null || tmdbToken.isBlank()) return fallback

        val kind = if (media.type == PlaybackContentType.EPISODE) "tv" else "movie"
        val cacheKey = "subdl_en_tmdb_${kind}_$tmdbId"
        val cache = prefs.getString(cacheKey, null)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (cache != null && cache.optLong("until") > System.currentTimeMillis()) {
            val english = cache.optString("en").trim()
            if (english.isNotBlank()) return EnglishIdentity(
                english, cache.optString("original").ifBlank { english },
                cache.optInt("year", 0).takeIf { it in 1888..2100 } ?: fallback.year
            )
        }

        val response = try {
            fetch(
                "https://api.themoviedb.org/3/$kind/$tmdbId?language=en-US",
                mapOf("Authorization" to "Bearer $tmdbToken", "Accept" to "application/json"),
                400_000
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return fallback
        }
        if (response.code != 200) return fallback

        val details = try {
            JSONObject(String(response.data, Charsets.UTF_8))
        } catch (_: Exception) {
            return fallback
        }
        val english = details.optString(if (kind == "tv") "name" else "title").trim()
        if (english.isBlank()) return fallback
        val original = details.optString(if (kind == "tv") "original_name" else "original_title")
            .trim().ifBlank { english }
        val year = details.optString(if (kind == "tv") "first_air_date" else "release_date")
            .take(4).toIntOrNull()?.takeIf { it in 1888..2100 } ?: fallback.year

        prefs.edit().putString(cacheKey, JSONObject()
            .put("until", System.currentTimeMillis() + 7L * 24 * 60 * 60_000)
            .put("en", english).put("original", original).put("year", year ?: 0)
            .toString()).apply()
        return EnglishIdentity(english, original, year)
    }

    /**
     * A busca de títulos SubDL retorna sd_id, ano, nomes e IDs.
     * Só aceita correspondência segura; nunca seleciona o primeiro homônimo.
     */
    private suspend fun findVerifiedSubdlId(
        media: PlaybackMedia, tmdbId: String?, english: EnglishIdentity, subdlKey: String
    ): String? {
        val targetName = normalizedTitle(english.englishTitle)
        val originalName = normalizedTitle(english.originalTitle)
        if (targetName.length < 2) return null
        val kind = if (media.type == PlaybackContentType.EPISODE) "tv" else "movie"
        val cacheKey = "subdl_verified_sd_${digest("$kind|$tmdbId|${english.year}|$targetName|$originalName")}"
        val previous = prefs.getString(cacheKey, null)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (previous != null && previous.optLong("until") > System.currentTimeMillis()) {
            val sdId = previous.optString("sd_id")
            if (safeSubdlId(sdId)) {
                trace("sdLookup source=verified-cache match=true")
                return sdId
            }
        }

        val response = fetch(
            "https://api.subdl.com/api/v2/movies/search?q=${Uri.encode(english.englishTitle)}&type=$kind&limit=10",
            mapOf("Authorization" to "Bearer $subdlKey", "Accept" to "application/json"),
            400_000
        )
        if (response.code != 200) throw SubdlHttpException(response.code)
        val payload = JSONObject(String(response.data, Charsets.UTF_8))
        val results = payload.optJSONArray("results")
            ?: throw SubdlResponseException("resposta de títulos não reconhecida")

        data class Verified(val sdId: String, val idMatch: Boolean)
        val candidates = mutableListOf<Verified>()
        for (i in 0 until minOf(results.length(), 10)) {
            val item = results.optJSONObject(i) ?: continue
            val id = item.optString("sd_id").trim()
            if (!safeSubdlId(id)) continue
            val candidateKind = item.optString("type").trim().lowercase(Locale.ROOT)
            if (candidateKind.isNotEmpty() && candidateKind != kind) continue

            val candidateTmdb = item.optString("tmdb_id").trim()
            // Se há dois IDs conhecidos e eles divergem, é outro conteúdo.
            if (tmdbId != null && candidateTmdb.isNotBlank() && candidateTmdb != tmdbId) continue
            val exactId = tmdbId != null && candidateTmdb == tmdbId

            val candidateName = normalizedTitle(item.optString("name"))
            val candidateOriginal = normalizedTitle(item.optString("original_name"))
            val titleMatches = (candidateName.isNotBlank() &&
                    (candidateName == targetName || candidateName == originalName)) ||
                    (candidateOriginal.isNotBlank() &&
                            (candidateOriginal == targetName || candidateOriginal == originalName))
            val candidateYear = item.optInt("year", 0).takeIf { it in 1888..2100 }
            // Sem coincidência de ID, exigimos o título e, se disponível, ano.
            // Ausência do ano impede adivinhar qual remake é o correto.
            val yearMatches = english.year == null || candidateYear == english.year
            if (exactId || (titleMatches && yearMatches &&
                        (english.year == null || candidateYear != null))) {
                candidates.add(Verified(id, exactId))
            }
        }

        val best = candidates.firstOrNull { it.idMatch }
            ?: candidates.distinctBy { it.sdId }.singleOrNull()
        val verifiedId = best?.sdId
        trace("sdLookup apiCandidates=${results.length()} verifiedMatches=${candidates.size} " +
                "unique=${candidates.distinctBy { it.sdId }.size} selected=${verifiedId != null}")
        if (verifiedId == null) return null
        prefs.edit().putString(cacheKey, JSONObject()
            .put("sd_id", verifiedId)
            .put("until", System.currentTimeMillis() + 7L * 24 * 60 * 60_000)
            .toString()).apply()
        return verifiedId
    }

    private fun safeSubdlId(id: String): Boolean =
        id.length in 1..50 && id.matches(Regex("[a-zA-Z0-9_-]+"))


    private class SubdlHttpException(val status: Int) : Exception()
    private class SubdlResponseException(val safeReason: String) : Exception()
    private class SubdlTitleNotFoundException : Exception()

    private suspend fun searchSubdl(
        media: PlaybackMedia, title: String, tmdb: String?, subdlKey: String,
        sdId: String? = null, allowUndocumentedEmptyResponse: Boolean = false
    ): List<ExternalSubtitleOption> {
        val kind = if (media.type == PlaybackContentType.EPISODE) "tv" else "movie"
        val route = when {
            sdId != null -> "sd_id"
            tmdb != null -> "tmdb_id"
            else -> "film_name"
        }
        trace("request route=$route type=$kind tmdb=${tmdb ?: "none"} " +
                "title='${traceTitle(title)}' season=${media.seasonNumber ?: "-"} " +
                "episode=${media.episodeNumber ?: "-"} languages=BR_PT,PT")
        val params = mutableListOf("type=$kind")
        when {
            sdId != null -> params.add("sd_id=${Uri.encode(sdId)}")
            tmdb != null -> params.add("tmdb_id=$tmdb")
            else -> params.add("film_name=${Uri.encode(title)}")
        }
        if (media.type == PlaybackContentType.EPISODE) {
            params.add("season=${media.seasonNumber}")
            params.add("episode=${media.episodeNumber}")
        }
        // Solicitar PT-BR e portugues na MESMA chamada da API. O servidor pode
        // retornar codigos diferentes; fazemos uma segunda filtragem local.
        params.add("languages=BR_PT,PT")
        params.add("subs_per_page=30")
        params.add("unpack=1")
        val response = fetch("https://api.subdl.com/api/v2/subtitles/search?${params.joinToString("&")}",
            mapOf("Authorization" to "Bearer $subdlKey", "Accept" to "application/json"), 1_000_000)
        trace("response route=$route http=${response.code} bytes=${response.data.size}")
        if (response.code != 200) throw SubdlHttpException(response.code)
        if (response.data.isEmpty()) throw SubdlResponseException("resposta vazia (HTTP 200)")
        val payload = JSONObject(String(response.data, Charsets.UTF_8))
        // SubDL pode retornar HTTP 200 com status=false e campo "error".
        // O código anterior descartava esse motivo e mostrava uma mensagem genérica.
        val errorObject = payload.optJSONObject("error")
        val errorCode = errorObject?.optString("code").orEmpty()
        val errorText = errorObject?.optString("message").orEmpty().ifBlank {
            if (errorObject != null) "" else payload.optString("error")
        }.ifBlank { payload.optString("message") }

        if (!payload.optBoolean("status", true)) {
            trace("response route=$route apiStatus=false " +
                    "noResults=${isMissingSubtitles(errorCode, errorText)}")
            // O SubDL as vezes devolve status=false com HTTP 200 para consultas
            // por nome/ID sem resultados. Neste caso SOMENTE a consulta inicial
            // pode continuar para a verificacao de nome e ano via sd_id.
            // Erros de chave, parametros, cota e acesso nunca sao mascarados.
            if (isMissingSubtitles(errorCode, errorText)) return emptyList()
            if (allowUndocumentedEmptyResponse &&
                isNonCriticalTitleRejection(errorCode, errorText)) throw SubdlTitleNotFoundException()
            throw SubdlResponseException(safeSubdlError(errorCode, errorText))
        }
        val subtitles = payload.optJSONArray("subtitles")
            ?: if (isMissingSubtitles(errorCode, errorText)) return emptyList()
            else if (allowUndocumentedEmptyResponse && isNonCriticalTitleRejection(errorCode, errorText))
                throw SubdlTitleNotFoundException()
            else throw SubdlResponseException("JSON sem lista de legendas")
        trace("response route=$route apiStatus=true packages=${subtitles.length()}")
        val result = ArrayList<ExternalSubtitleOption>()
        val seen = HashSet<String>()
        var skippedEpisode = 0
        var skippedUrl = 0
        var skippedDuplicate = 0
        for (i in 0 until subtitles.length()) {
            val item = subtitles.optJSONObject(i) ?: continue
            val id = item.optString("n_id").ifBlank { item.optString("nId") }
                .ifBlank { item.optString("id") }
            val lang = item.optString("lang").ifBlank { item.optString("language", "und") }
            val release = item.optString("release_name").ifBlank { item.optString("name", "Legenda") }
            val children = item.optJSONArray("unpack_files")
            if (children != null && children.length() > 0) {
                for (j in 0 until children.length()) {
                    val file = children.optJSONObject(j) ?: continue
                    val childName = file.optString("release_name")
                        .ifBlank { file.optString("name", release) }
                    if (media.type == PlaybackContentType.EPISODE) {
                        val season = file.optInt("season", -1)
                        val episode = file.optInt("episode", -1)
                        if (season > 0 && season != media.seasonNumber) {
                            skippedEpisode++
                            continue
                        }
                        if (episode > 0 && episode != media.episodeNumber) {
                            skippedEpisode++
                            continue
                        }
                        if (episode <= 0) {
                            // Pacotes sem identificador não podem ser atribuídos a
                            // qualquer episódio por engano.
                            val match = Regex("(?i)S(\\d{1,2})[ ._-]*E(\\d{1,3})")
                                .find(childName)
                            val foundSeason = match?.groupValues?.get(1)?.toIntOrNull()
                            val foundEpisode = match?.groupValues?.get(2)?.toIntOrNull()
                            if (foundSeason != media.seasonNumber ||
                                foundEpisode != media.episodeNumber) {
                                skippedEpisode++
                                continue
                            }
                        }
                    }
                    val url = subdlDownloadUrl(file.optString("url"))
                    if (url == null) {
                        skippedUrl++
                        continue
                    }
                    val childLang = file.optString("language").ifBlank { file.optString("lang", lang) }
                    val identifier = file.optString("file_n_id").ifBlank { id }
                    val signature = "$identifier|$url"
                    if (seen.add(signature)) result.add(ExternalSubtitleOption(
                        "SubDL", identifier, childLang, childName, url))
                    else skippedDuplicate++
                }
            } else {
                // Arquivo avulso: em séries, nunca oferecer ZIP genérico de
                // temporada sem identificação específica do episódio.
                val episodeMatches = media.type != PlaybackContentType.EPISODE || (
                        !item.optBoolean("full_season", false) &&
                                item.optInt("episode", -1) == media.episodeNumber &&
                                (item.optInt("season", -1) <= 0 ||
                                        item.optInt("season", -1) == media.seasonNumber)
                        )
                if (episodeMatches) {
                    val url = subdlDownloadUrl(item.optString("url"))
                    if (id.isNotBlank() || url != null) {
                        val signature = "$id|$url"
                        if (seen.add(signature)) result.add(ExternalSubtitleOption(
                            "SubDL", id.ifBlank { digest(url!!) }, lang, release, url))
                        else skippedDuplicate++
                    }
                }
            }
            if (result.size >= 100) break
        }
        trace("parsed route=$route options=${result.size} " +
                "ptBR=${result.count { portugueseSubtitleRank(it.language, it.releaseName) == 0 }} " +
                "pt=${result.count { portugueseSubtitleRank(it.language, it.releaseName) == 1 }} " +
                "other=${result.count { portugueseSubtitleRank(it.language, it.releaseName) == 2 }} " +
                "rejectedEpisode=$skippedEpisode rejectedUrl=$skippedUrl duplicates=$skippedDuplicate")
        return result
    }

    /** Converte erros HTTP 200 do SubDL em diagnósticos seguros e legíveis. */
    private fun safeSubdlError(code: String, message: String): String {
        val combined = "$code $message".lowercase(Locale.ROOT)
        val reason = when {
            combined.contains("quota") || combined.contains("rate limit") ||
                    combined.contains("too many requests") -> "cota ou limite da API"
            combined.contains("api key") || combined.contains("api_key") ||
                    combined.contains("invalid key") || combined.contains("authorization") ||
                    combined.contains("unauthorized") -> "credencial recusada pela API"
            combined.contains("tmdb") -> "identificador TMDB rejeitado"
            combined.contains("season") || combined.contains("episode") -> "temporada/episódio rejeitado"
            combined.contains("language") -> "idioma rejeitado"
            combined.contains("parameter") || combined.contains("invalid") ||
                    combined.contains("validation") -> "parâmetros não aceitos"
            else -> {
                // Código do serviço apenas se não contiver dados sensíveis.
                val publicCode = code.trim().takeIf {
                    it.length in 1..35 && it.matches(Regex("[a-zA-Z0-9_-]+"))
                }
                if (publicCode != null) {
                    "erro: $publicCode"
                } else {
                    // Mostra o texto curto devolvido pela API quando for seguro.
                    // Nunca exibe chaves, endereços ou tokens de autenticação.
                    val preview = message.trim().replace(Regex("\\s+"), " ")
                    val sensitive = preview.contains("http", ignoreCase = true) ||
                            preview.contains("bearer", ignoreCase = true) ||
                            preview.contains("subdl_", ignoreCase = true) ||
                            preview.contains("api_key", ignoreCase = true) ||
                            preview.contains(Regex("[A-Za-z0-9]{28,}"))
                    val safePreview = preview.takeIf { !sensitive && it.length in 3..65 &&
                            it.matches(Regex("[\\p{L}\\p{N} .,;:/()!?_-]+")) }
                    safePreview ?: "resposta recusada (HTTP 200)"
                }
            }
        }
        return reason
    }

    /** Distingue "nada encontrado" de um erro de autenticação ou parâmetros. */
    private fun isMissingSubtitles(code: String, message: String): Boolean {
        val error = "$code $message".lowercase(Locale.ROOT)
        if (error.contains("key") || error.contains("token") ||
            error.contains("auth") || error.contains("quota") ||
            error.contains("limit") || error.contains("invalid") ||
            error.contains("parameter")) return false
        return error.contains("no subtitles") ||
                error.contains("no subtitle") ||
                error.contains("no matching subtitle") ||
                error.contains("nothing found") ||
                error.contains("does not exist") ||
                error.contains("no results") ||
                error.contains("no result") ||
                error.contains("no match") ||
                error.contains("not_found") ||
                error.contains("not found") ||
                error.contains("sem legenda") ||
                error.contains("sem resultado")
    }

    /** Resposta 200 ambigua: liberar fallback somente se nao for erro real de API. */
    private fun isNonCriticalTitleRejection(code: String, message: String): Boolean {
        val combined = "$code $message".lowercase(Locale.ROOT)
        val hardError = listOf("key", "token", "auth", "quota", "limit", "rate",
            "invalid", "parameter", "validation", "denied", "forbidden", "permission",
            "access", "expired", "server", "internal", "unavailable", "timeout")
        return hardError.none { combined.contains(it) }
    }

    /** A API retorna URLs relativas /subtitle/...; aceitar somente o host oficial. */
    private fun subdlDownloadUrl(raw: String): String? {
        if (raw.isBlank()) return null
        val resolved = if (raw.startsWith("/subtitle/")) "https://dl.subdl.com$raw" else raw
        return resolved.takeIf { safeUrl(it, "subdl.com") }
    }

    private data class HttpResponse(val code: Int, val data: ByteArray)
    private suspend fun fetch(url: String, headers: Map<String, String>, limit: Int,
                              allowSameProviderRedirect: Boolean = false): HttpResponse {
        coroutineContext.ensureActive()
        var current = url
        repeat(3) {
            require(current.startsWith("https://")) { "Somente HTTPS é permitido" }
            val con = URL(current).openConnection() as HttpURLConnection
            try {
                con.connectTimeout = 6_000
                con.readTimeout = 9_000
                con.instanceFollowRedirects = false
                con.setRequestProperty("User-Agent", userAgent)
                // Nunca reutilizar uma chave API em outro host após redirect.
                if (URL(current).host.equals(URL(url).host, ignoreCase = true)) {
                    headers.forEach { (key, value) -> con.setRequestProperty(key, value) }
                }
                val code = con.responseCode
                if (code == 429) {
                    val provider = when {
                        current.contains("api.subdl.com") -> "subdl"
                        else -> null
                    }
                    if (provider != null) {
                        val waitSeconds = con.getHeaderField("Retry-After")?.toLongOrNull()
                            ?.coerceIn(60L, 86_400L) ?: 3_600L
                        prefs.edit().putLong("blocked_until_$provider",
                            System.currentTimeMillis() + waitSeconds * 1000L).apply()
                    }
                }
                if (code in 300..399 && allowSameProviderRedirect) {
                    val next = con.getHeaderField("Location") ?: return HttpResponse(code, byteArrayOf())
                    val redirect = URL(URL(current), next).toString()
                    val allowedRoot = "subdl.com"
                    if (!safeUrl(redirect, allowedRoot)) return HttpResponse(403, byteArrayOf())
                    current = redirect
                } else {
                    val contentLength = con.contentLength
                    if (contentLength > limit) return HttpResponse(413, byteArrayOf())
                    val payload = if (code in 200..299) con.inputStream.use { it.readAtMost(limit + 1) }
                    else byteArrayOf()
                    return HttpResponse(if (payload.size > limit) 413 else code, payload.take(limit).toByteArray())
                }
            } finally { con.disconnect() }
        }
        return HttpResponse(310, byteArrayOf())
    }


    /** Leitura limitada compatível com Android anterior à API 33. */
    private fun InputStream.readAtMost(maxBytes: Int): ByteArray {
        require(maxBytes > 0)
        val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
        val chunk = ByteArray(8192)
        var total = 0
        while (total < maxBytes) {
            val amount = read(chunk, 0, minOf(chunk.size, maxBytes - total))
            if (amount == -1) break
            if (amount == 0) {
                val singleByte = read()
                if (singleByte == -1) break
                output.write(singleByte)
                total++
            } else {
                output.write(chunk, 0, amount)
                total += amount
            }
        }
        return output.toByteArray()
    }

    private fun safeUrl(value: String, root: String): Boolean = runCatching {
        val uri = Uri.parse(value)
        uri.scheme.equals("https", true) &&
                (uri.host.equals(root, true) || uri.host?.endsWith(".$root", true) == true)
    }.getOrDefault(false)

    private fun unpackSubtitle(
        bytes: ByteArray, season: Int? = null, episode: Int? = null
    ): ByteArray? {
        if (bytes.size < 6) return null
        if (bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) return bytes
        val candidates = ArrayList<Pair<String, ByteArray>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            var inspected = 0
            while (entry != null && inspected++ < 500) {
                if (!entry.isDirectory && (entry.name.endsWith(".srt", true) || entry.name.endsWith(".vtt", true))) {
                    val data = zip.readAtMost(1_000_001)
                    if (data.size in 1..1_000_000) candidates += entry.name to data
                }
                entry = zip.nextEntry
            }
        }
        if (candidates.isEmpty()) return null
        if (season != null && episode != null) {
            val desired = candidates.firstOrNull { (name, _) ->
                Regex("(?i)S(\\d{1,2})[ ._-]*E(\\d{1,3})").find(name)?.let { match ->
                    match.groupValues[1].toIntOrNull() == season &&
                            match.groupValues[2].toIntOrNull() == episode
                } == true
            }
            if (desired != null) return desired.second
            if (candidates.size > 1) return null // Nunca escolher outro episódio.
        }
        return candidates.first().second
    }

    private fun parsedSubtitle(bytes:ByteArray,provider:String,source:String):List<ExternalSubtitleCue> {
        val cues=parseSubtitle(bytes)
        Log.i("SubtitleSyncPerf","stage=timing provider=$provider source=$source cues=${cues.size} firstMs=${cues.firstOrNull()?.startMs?:-1} lastMs=${cues.maxOfOrNull{it.endMs}?:-1}")
        return cues
    }

    private fun parseSubtitle(bytes: ByteArray): List<ExternalSubtitleCue> {
        val utf8 = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        val text = if (utf8.count { it == '\uFFFD' } > 5) String(bytes, Charsets.ISO_8859_1) else utf8
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
            .removePrefix("WEBVTT").trim()
        val cues = mutableListOf<ExternalSubtitleCue>()
        for (block in normalized.split(Regex("\\n\\s*\\n"))) {
            val lines = block.lines()
            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingIndex < 0) continue
            val parts = lines[timingIndex].split("-->")
            if (parts.size != 2) continue
            val start = parseTime(parts[0].trim()) ?: continue
            val end = parseTime(parts[1].trim().substringBefore(' ')) ?: continue
            if (end <= start) continue
            val content = lines.drop(timingIndex + 1).joinToString("\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .trim()
            if (content.isNotEmpty()) cues.add(ExternalSubtitleCue(start, end, content))
            if (cues.size >= 20_000) break
        }
        return cues.sortedBy { it.startMs }
    }

    private fun parseTime(raw: String): Long? {
        val chunks = raw.replace(',', '.').split(':')
        if (chunks.size !in 2..3) return null
        val seconds = chunks.last().toDoubleOrNull() ?: return null
        if (seconds < 0 || seconds >= 60) return null
        val mins = chunks[chunks.size - 2].toLongOrNull() ?: return null
        val hours = if (chunks.size == 3) chunks[0].toLongOrNull() ?: return null else 0L
        if (mins !in 0..59 || hours !in 0..999) return null
        return (hours * 3_600_000 + mins * 60_000 + seconds * 1_000).toLong()
    }

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).take(16).joinToString("") { "%02x".format(it) }
}
