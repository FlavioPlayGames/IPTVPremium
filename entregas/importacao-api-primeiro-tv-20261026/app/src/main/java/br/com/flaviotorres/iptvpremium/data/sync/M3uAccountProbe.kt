package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder

internal class M3uAccountProbe(private val client: PlaylistHttpClient) {
    suspend fun verify(source: PlaylistSource.XtreamCodes): Boolean {
        val url = source.serverUrl.trimEnd('/') + "/player_api.php?username=" + encode(source.username) + "&password=" + encode(source.password)
        try {
            return client.withReader(url) { reader ->
                val text = StringBuilder()
                val buffer = CharArray(2048)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = reader.read(buffer)
                    if (count < 0) break
                    check(text.length + count <= 65_536) { "Resposta de autenticação excede o limite." }
                    text.append(buffer, 0, count)
                }
                val raw = text.toString().trim().removePrefix("\uFEFF")
                check(raw.startsWith("{") && raw.endsWith("}")) { "Resposta de autenticação incompleta." }
                val tokens = JSONTokener(raw)
                val json = tokens.nextValue() as? JSONObject ?: error("Resposta de autenticação inválida.")
                check(tokens.nextClean() == '\u0000') { "Resposta de autenticação inválida." }
                val info = json.optJSONObject("user_info") ?: error("API não confirmada.")
                check(info.optString("auth") in setOf("1", "true") && info.optString("status").equals("Active", true)) {
                    "A API não confirmou uma conta ativa. Confira a lista."
                }
                true
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            // These exact messages belong to our HTTP client. Only a definite missing API is
            // negative capability; auth errors, bad JSON, DNS and timeouts remain failures.
            if (error is IllegalStateException && error.message in setOf(
                    "Erro HTTP 404 ao acessar a lista.", "Erro HTTP 405 ao acessar a lista.")) return false
            throw IllegalStateException("Não foi possível validar uma conta Xtream ativa. Tente novamente.")
        }
    }
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
