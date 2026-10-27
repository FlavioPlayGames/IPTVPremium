package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.DialogProperties
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import br.com.flaviotorres.iptvpremium.ui.components.AppKeyboard
import br.com.flaviotorres.iptvpremium.ui.components.TvKeyboardSearchText
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardType
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardDelete
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardMoveCursor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton as PortalOutlineButton
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.CategoryType
import br.com.flaviotorres.iptvpremium.domain.model.CategorySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val parentalFrame get() = Brush.linearGradient(listOf(
    br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFF976C25),
    br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFFFFE6A0),
    br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFFB99245)))

private val parentalAccent get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.accent

internal data class ParentalCategoryRequest(val playlist: String, val type: String, val category: String?, val title: String, val onGranted: () -> Unit)

@Composable
internal fun ParentalPinApproval(title: String, onCancel: () -> Unit, onApproved: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val epoch by ParentalSession.epoch.collectAsState()
    var pin by remember(epoch, title) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember(epoch) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CONTEÚDO BLOQUEADO", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 22.sp)
        Text(title, color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 17.sp)
        PinField("PIN do responsável", pin, { pin = it }, !busy, legacyAuth = true)
        message?.let { Text(it, color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 14.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Voltar", modifier = Modifier.weight(1f), onClick = onCancel)
            LaunchedEffect(pin) {
            if (pin.length == 6 && !busy) {
                val submittedPin = pin
                val submittedEpoch = epoch
                scope.launch {
                    busy = true
                    try {
                        val valid = withContext(Dispatchers.IO) { ParentalPinStore(context).let { it.enabled() && it.verify(submittedPin) } }
                        if (valid && submittedEpoch == ParentalSession.epoch.value) onApproved()
                        else message = "PIN incorreto ou sessão encerrada. Após cinco erros, aguarde 30 segundos."
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { message = "Não foi possível validar. Após cinco erros, aguarde 30 segundos." }
                    finally { busy = false; pin = "" }
                }
            
            }
        }
        }
        Text("A autorização termina ao sair do aplicativo.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFB7BBC1), fontSize = 13.sp)
    }
}

@Composable
internal fun ParentalCategoryDialog(request: ParentalCategoryRequest?, onDismiss: () -> Unit) {
    if (request == null) return
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.widthIn(max = 480.dp).background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF101419), RoundedCornerShape(14.dp))) {
            ParentalPinApproval(request.title, onDismiss) {
                ParentalPolicy.grant(request.playlist, request.type, request.category)
                request.onGranted()
                onDismiss()
            }
        }
    }
}

/** A tela/player so e composto depois da verificacao local; nao apenas coberto por um aviso. */
@Composable
internal fun ParentalItemAllowed(playlist: String?, type: String, id: String?, repository: PlaylistCatalogRepository, onBack: () -> Unit): Boolean {
    val access by ParentalPolicy.accessFlow.collectAsState(initial = ParentalPolicy.access())
    if (!access.hasRestrictions(playlist.orEmpty(), type)) return true
    val key = listOf(playlist, type, id, access)
    var blocked by remember(key) { mutableStateOf<Boolean?>(null) }
    var category by remember(key) { mutableStateOf<String?>(null) }
    var failure by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        try {
            val result = withContext(Dispatchers.IO) {
                repository.parentalTarget(playlist.orEmpty(), type, id.orEmpty())?.let { target ->
                    target to repository.isParentalBlocked(playlist.orEmpty(), type, id.orEmpty())
                }
            }
            if (result == null) { failure = true; blocked = true }
            else { category = result.first.categoryId; blocked = result.second }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { failure = true; blocked = true }
    }
    if (blocked == false) return true
    Box(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.background), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (blocked == null) {
                Text("Verificando proteção…", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 18.sp)
                br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Voltar", onClick = onBack)
            } else if (failure) {
                Text("Não foi possível verificar a proteção deste conteúdo.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
                br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Voltar", onClick = onBack)
            } else if (access.kidsCatalog || access.rules.kidsOnly) {
                Text("Este conteúdo não está disponível no modo Kids.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
                br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Voltar", onClick = onBack)
            } else ParentalPinApproval("Digite o PIN para acessar esta categoria.", onBack) {
                ParentalPolicy.grant(playlist.orEmpty(), type, category)
            }
        }
    }
    return false
}

@Composable
internal fun ParentalPreferencesSettings(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val store = remember { ParentalPinStore(context) }
    val enabled by ParentalSession.enabled.collectAsState()
    val epoch by ParentalSession.epoch.collectAsState()
    val rules by ParentalPolicy.rules.collectAsState()
    var authorized by remember(epoch) { mutableStateOf(!store.enabled() || ParentalSession.unlocked.value) }
    var authorizedEpoch by remember { mutableStateOf<Long?>(if(ParentalSession.unlocked.value) epoch else null) }
    var needsPinUpgrade by remember(epoch) { mutableStateOf(false) }
    LaunchedEffect(enabled, epoch) {
        if (enabled && authorizedEpoch != epoch && !ParentalSession.unlocked.value) authorized = false
        if (!enabled) authorized = true
    }
    var pin by remember(epoch) { mutableStateOf("") }
    var confirm by remember(epoch) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember(epoch) { mutableStateOf<String?>(null) }
    var page by remember(epoch) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val tileFocus = remember { List(5) { FocusRequester() } }
    var lastTile by remember { mutableIntStateOf(0) }
    BackHandler(enabled = authorized && page != null && !busy && !needsPinUpgrade) { page = if(page?.startsWith("KIDS_")==true)"KIDS" else null; pin = ""; confirm = "" }
    LaunchedEffect(page, authorized, enabled) {
        if (enabled && authorized && page == null && "__invalid__" !in rules.all) {
            withFrameNanos { }; withFrameNanos { }; tileFocus[lastTile].requestFocus()
        }
    }
    fun save(change: (ParentalRules) -> ParentalRules) {
        if (busy) return
        busy = true
        scope.launch {
            try { withContext(Dispatchers.IO) { ParentalPolicy.update(change) }; message = "Proteção salva." }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Não foi possível salvar. Tente novamente." }
            finally { busy = false }
        }
    }
    if (enabled && authorized && (page == "MOVIES" || page == "SERIES" || page?.startsWith("KIDS_") == true) && "__invalid__" !in rules.all) {
        ParentalCategorySettings(page!!.removePrefix("KIDS_"), busy, ::save, message?.takeUnless { it == "Proteção salva." }, kids = page!!.startsWith("KIDS_")) { page = if(page?.startsWith("KIDS_")==true)"KIDS" else null; message = null }
        return
    }
    Column(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.background).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Controle parental", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 24.sp,
                fontFamily = if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular)androidx.compose.ui.text.font.FontFamily.SansSerif else androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.cinzel_semibold)), modifier = Modifier.weight(1f))
            br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("‹ Voltar", modifier = Modifier.width(104.dp), onClick = { if(page=="KIDS")page=null else onBack() })
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.edge.copy(alpha=.5f)))
    if (page != "MOVIES" && page != "SERIES") Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (enabled) "Proteção da família" else "Crie seu PIN de acesso", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text,
            fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (enabled) Text("●  PIN ATIVO", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 11.sp)
    }
    if (!authorized) {
        ParentalForm {
        Text("Digite seu PIN para gerenciar os bloqueios.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFB7BBC1), fontSize = 14.sp)
        PinField("PIN do responsável", pin, { pin = it }, !busy, legacyAuth = true)
        LaunchedEffect(pin) {
            if (pin.length == 6 && !busy) {
            val submittedPin = pin
                val submittedEpoch = epoch
                scope.launch {
                busy = true
                try {
                    val valid = withContext(Dispatchers.IO) { store.enabled() && store.verify(submittedPin) }
                    authorized = valid && submittedEpoch == ParentalSession.epoch.value
                    if (authorized) {
                        authorizedEpoch = epoch
                        ParentalSession.unlocked.value = true
                    }
                    if (authorized && submittedPin.length < 6) { needsPinUpgrade = true; page = "pin" }
                    message = if (authorized) null else "PIN incorreto. Após cinco erros, aguarde 30 segundos."
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { message = "Não foi possível validar. Após cinco erros, aguarde 30 segundos." }
                finally { busy = false; pin = "" }
            }
        
            }
        }
        }
    } else if (!enabled || page == "pin" || needsPinUpgrade) {
        ParentalForm {
        if (needsPinUpgrade) Text("Seu PIN atual foi confirmado. Cadastre 6 números para continuar.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 14.sp)
        PinField(if (enabled) "Novo PIN (6 números)" else "Criar PIN (6 números)", pin, { pin = it }, !busy)
        PinField("Repita o PIN", confirm, { confirm = it }, !busy, autoFocus=false)
        Text("Use exatamente 6 números. Guarde seu PIN: não há recuperação pelo site.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFB7BBC1), fontSize = 12.sp)
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton(if (busy) "Salvando…" else "Salvar PIN", enabled = !busy && pin.length == 6 && pin == confirm) {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) {
                        if (!enabled) ParentalPolicy.reset()
                        store.set(pin)
                    }
                    if (epoch == ParentalSession.epoch.value) {
                        authorizedEpoch = epoch
                        authorized = true
                        ParentalSession.unlocked.value = true
                        needsPinUpgrade = false; page = null; message = "PIN salvo. Escolha os bloqueios abaixo."
                    }
                    ParentalSession.enabled.value = true
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { message = "Não foi possível salvar o PIN." }
                finally { busy = false; pin = ""; confirm = "" }
            }
        }
        if (enabled && !needsPinUpgrade) ParentalAction("‹ Voltar aos controles", "", enabled = !busy) { page = null; pin = ""; confirm = "" }
        }
    } else if ("__invalid__" in rules.all) {
        Text("As regras salvas não puderam ser lidas. O acesso continua protegido pelo PIN.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Recriar regras de proteção", enabled = !busy) { save { ParentalRules(general = true) } }

    } else if (page == "disable") {
        ParentalForm {
            Text("Desativar proteção", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 19.sp)
            Text("Desativar o PIN e todos os filtros neste aparelho?", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Cancelar", modifier = Modifier.weight(1f), enabled = !busy) { page = null }
                br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Confirmar", modifier = Modifier.weight(1f), enabled = !busy) {
                    scope.launch {
                        busy = true
                        try {
                            withContext(Dispatchers.IO) { ParentalPolicy.reset(); store.disable() }
                            ParentalSession.enabled.value = false; page = null; message = "Proteção desativada."
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { message = "Não foi possível desativar." }
                        finally { busy = false }
                    }
                }
            }
        }
    } else if(page == "KIDS") {
        Column(Modifier.fillMaxWidth().border(1.dp,parentalAccent.copy(alpha=.5f),RoundedCornerShape(14.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("KIDS",color=br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text,fontWeight=FontWeight.Bold,fontSize=20.sp)
            Text("Configurações exclusivas do ambiente infantil",color=br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFB7BBC1),fontSize=12.sp)
            ParentalAction("Apenas Kids",if(rules.kidsOnly) "Ativado · Acesso adulto bloqueado" else "Desativado",enabled=!busy) { save {it.copy(kidsOnly=!it.kidsOnly)} }
            ParentalAction("Iniciar diretamente no Kids",if(rules.startInKids) "Ativado" else "Desativado",enabled=!busy) {save {it.copy(startInKids=!it.startInKids)} }
            listOf("MOVIES" to "Filmes", "SERIES" to "Séries").forEach { (type,label) ->
                ParentalAction("Bloquear $label no Kids",if(type in rules.kidsBlockedSections) "Bloqueado · Não aparece na Home Kids" else "Disponível",enabled=!busy) {
                    save {it.copy(kidsBlockedSections=if(type in it.kidsBlockedSections)it.kidsBlockedSections-type else it.kidsBlockedSections+type)}
                }
            }
            Text("Bloqueios por conteúdo · independentes do catálogo principal",color=br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text,fontSize=13.sp)
            listOf("LIVE_TV" to "Canais", "MOVIES" to "Filmes", "SERIES" to "Séries").forEach { (type,label) ->
                ParentalAction("Categorias de $label no Kids","Escolher conteúdo bloqueado",enabled=!busy) {page="KIDS_$type"}
            }
        }
        ParentalAction("‹ Voltar ao controle parental","",enabled=!busy){page=null;message=null}
    } else {
        Text("Escolha o que deseja proteger. As alterações são salvas automaticamente.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFB7BBC1), fontSize = 13.sp)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = 1
            val entries = listOf(
                Triple("Bloqueio geral", if (rules.general) "Ativado · PIN ao abrir o aplicativo" else "Desativado · Acesso ao aplicativo livre", "M10 21H38V43H10Z M15 21V14A9 9 0 0 1 33 14V21 M24 29V35"),
                Triple("Canais adultos", if (rules.adultChannels) "Bloqueados · Acesso com PIN" else "Filtro adulto desativado", "M5 9H43V35H5Z M17 43H31 M24 35V43 M18 3L24 9L30 3"),
                Triple("Filmes", "Escolha as categorias da sua lista", "M6 11H42V40H6Z M6 20H42 M12 11L18 20 M25 11L31 20 M24 26L32 30L24 35Z"),
                Triple("Séries", "Escolha as categorias da sua lista", "M8 6H35V34H8Z M14 12H41V41H14 M19 19L29 25L19 31Z"),
                Triple("Kids", "Abertura, acesso e bloqueios infantis", "M24 4L29 17L43 18L32 27L36 42L24 34L12 42L16 27L5 18L19 17Z")
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.indices.chunked(columns).forEach { indices ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        indices.forEach { index ->
                            val entry = entries[index]
                            ParentalTile(entry.first, entry.second, entry.third,
                                active = when (index) { 0 -> rules.general; 1 -> rules.adultChannels; else -> null },
                                enabled = true, modifier = Modifier.weight(1f).focusRequester(tileFocus[index])
                                    .onFocusChanged { if (it.isFocused) lastTile = index }
                                    .focusProperties {
                                        left = if (index % columns > 0) tileFocus[index - 1] else FocusRequester.Cancel
                                        right = if (index % columns < columns - 1 && index + 1 < entries.size) tileFocus[index + 1] else FocusRequester.Cancel
                                        if (index >= columns) up = tileFocus[index - columns]
                                        if (index + columns < entries.size) down = tileFocus[index + columns]
                                    }) {
                                if (!busy) when (index) {
                                    0 -> save { it.copy(general = !it.general) }
                                    1 -> save { it.copy(adultChannels = !it.adultChannels) }
                                    2 -> page = "MOVIES"
                                    3 -> page = "SERIES"
                                    4 -> page = "KIDS"
                                }
                            }
                        }
                        if(indices.size<columns)Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Text("Os conteúdos bloqueados continuam visíveis com cadeado e pedem PIN para abrir.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFF9D9FA5), fontSize = 12.sp)
        Box(Modifier.fillMaxWidth().height(1.dp).background(parentalAccent.copy(alpha = .2f)))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val actions: @Composable () -> Unit = {
                ParentalAction("Alterar PIN", "Defina um novo código", enabled = !busy) { page = "pin" }
                ParentalAction("Bloquear novamente", "Encerrar acessos liberados", enabled = !busy) {
                    ParentalPolicy.grants.value = emptySet(); message = "Categorias bloqueadas novamente."
                }
                ParentalAction("Desativar proteção", "Remover PIN e bloqueios", enabled = !busy) { page = "disable" }
            }
            // Actions wrap on narrow screens; every target remains reachable by touch and D-pad.
            if (maxWidth >= 660.dp) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ParentalAction("Alterar PIN", "Defina um novo código", enabled = !busy) { page = "pin" } }
                Box(Modifier.weight(1f)) { ParentalAction("Bloquear novamente", "Encerrar acessos liberados", enabled = !busy) { ParentalPolicy.grants.value = emptySet(); message = "Categorias bloqueadas novamente." } }
                Box(Modifier.weight(1f)) { ParentalAction("Desativar proteção", "Remover PIN e bloqueios", enabled = !busy) { page = "disable" } }
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }

    }
    message?.let { if ((page != "MOVIES" && page != "SERIES") || it != "Proteção salva.") Text(it, color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 14.sp) }
    }
}

@Composable
private fun ParentalCategorySettings(type: String, busy: Boolean, save: ((ParentalRules) -> ParentalRules) -> Unit, message: String?, kids: Boolean = false, onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val storage = remember { PlaylistStorage(context) }
    val selection by storage.selection.collectAsState(initial = null)
    var selected by remember(type) { mutableStateOf<String?>(null) }
    val playlists = selection?.playlists.orEmpty()
    val current = playlists.firstOrNull { it.id == selected } ?: playlists.firstOrNull { it.id == selection?.activeId } ?: playlists.firstOrNull()
    val rules by ParentalPolicy.rules.collectAsState()
    val id = current?.id.orEmpty()
    var choosePlaylist by remember { mutableStateOf(false) }
    val dao = remember { PlaylistCacheDatabase.getInstance(context).playlistCacheDao() }
    val generation by remember(id) { dao.observeActiveGenerationId(id) }.collectAsState(initial = null)
    var error by remember(id, type) { mutableStateOf(false) }
    val categories by produceState<List<Category>?>(null, id, type, generation) {
        error = false
        try {
            value = withContext(Dispatchers.IO) {
                val currentGeneration = generation
                if (currentGeneration == null) emptyList() else dao.getCategoriesByType(id, currentGeneration, type).map {
                    Category(it.categoryId, it.name, if (type == "MOVIES") CategoryType.MOVIES else if(type=="LIVE_TV") CategoryType.LIVE_TV else CategoryType.SERIES, CategorySource.PROVIDER)
                } + Category("__uncategorized__", "Sem categoria", if (type == "MOVIES") CategoryType.MOVIES else if(type=="LIVE_TV") CategoryType.LIVE_TV else CategoryType.SERIES, CategorySource.PROVIDER)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { error = true; value = emptyList() }
    }
    var categoryQuery by remember(id, type) { mutableStateOf("") }
    var searchOpen by remember(id, type) { mutableStateOf(false) }
    var cursor by remember(id, type) { mutableIntStateOf(0) }
    var restoreSearch by remember { mutableIntStateOf(0) }
    val matching = remember(categories, categoryQuery, kids) { categories.orEmpty().distinctBy { it.id }.filter { (!kids || br.com.flaviotorres.iptvpremium.ui.kids.KidsContent.category(it.name)) && it.name.contains(categoryQuery, ignoreCase = true) } }
    val key = (if(kids) "kids/" else "") + "$id/$type"
    val allBlocked = key in rules.all
    val blockedCount = if (allBlocked) categories.orEmpty().distinctBy { it.id }.size
        else categories.orEmpty().distinctBy { it.id }.count { it.id in rules.categories[key].orEmpty() }
    val grid = rememberLazyGridState()
    val searchFocus = remember { FocusRequester() }
    val firstFocus = remember(id, type) { FocusRequester() }
    LaunchedEffect(id, type, categoryQuery) { grid.scrollToItem(0) }
    LaunchedEffect(restoreSearch) { if (restoreSearch > 0) { withFrameNanos { }; searchFocus.requestFocus() } }
    fun closeSearch() { searchOpen = false; restoreSearch += 1 }
    LaunchedEffect(id, type, categories) {
        if (!categories.isNullOrEmpty() && categoryQuery.isEmpty()) { withFrameNanos { }; firstFocus.requestFocus() }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.background).padding(4.dp).border(1.dp, parentalFrame, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        val wide = maxWidth >= 780.dp
        val columns = if (maxWidth >= 640.dp) 3 else if (maxWidth >= 390.dp) 2 else 1
        val search: @Composable (Modifier) -> Unit = { modifier ->
            var focused by remember { mutableStateOf(false) }
            Row(modifier.heightIn(min = 36.dp).focusRequester(searchFocus).onFocusChanged { focused = it.isFocused }
                .background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF101315), RoundedCornerShape(5.dp))
                .border(if (focused) 2.dp else 1.dp, parentalFrame, RoundedCornerShape(5.dp))
                .clickable(role = Role.Button) { cursor = categoryQuery.length; searchOpen = true }
                .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CategoryLineIcon("M20 5A15 15 0 1 1 20 35A15 15 0 1 1 20 5 M31 31L43 43", br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFCCCCCF), Modifier.size(17.dp))
                Text(categoryQuery.ifEmpty { "Pesquisar categoria" }, color = if (categoryQuery.isEmpty()) br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFA7A9AE) else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF),
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
        val toggle: @Composable (Modifier) -> Unit = { modifier ->
            CompactCategoryControl("Bloquear tudo", modifier, selected = allBlocked, icon = categoryLockPath, enabled = id.isNotEmpty()) {
                save { it.copy(all = if (key in it.all) it.all - key else it.all + key) }
            }
        }
        val heading: @Composable (Modifier) -> Unit = { modifier ->
            Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(themePainterResource(R.drawable.iptv_premium_logo), contentDescription = "IPTV Premium", modifier = Modifier.size(width = 72.dp, height = 44.dp))
                Box(Modifier.width(1.dp).height(35.dp).background(Color(0xFF494949)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text((if(kids) "KIDS · " else "CONTROLE PARENTAL · ") + when(type){"MOVIES"->"FILMES";"LIVE_TV"->"CANAIS";else->"SÉRIES"}, color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text,
                        fontSize = if (wide) 15.sp else 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    var focused by remember { mutableStateOf(false) }
                    Text("Lista: " + current?.name.orEmpty() + if (playlists.size > 1) " ▾" else "", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFCDCDD0), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.onFocusChanged { focused = it.isFocused }
                            .border(if (focused) 1.dp else 0.dp, if (focused) parentalAccent else Color.Transparent, RoundedCornerShape(3.dp))
                            .clickable(enabled = playlists.size > 1, role = Role.Button) { choosePlaylist = true }.padding(vertical = 3.dp))
                }
            }
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                heading(Modifier.width(352.dp)); search(Modifier.weight(1f)); toggle(Modifier.width(132.dp))
                CompactCategoryControl("‹ Voltar", Modifier.width(78.dp), onClick = onBack)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    heading(Modifier.weight(1f)); CompactCategoryControl("‹ Voltar", Modifier.width(78.dp), onClick = onBack)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    search(Modifier.weight(1f)); toggle(Modifier.width(132.dp))
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    playlists.isEmpty() -> Text("Cadastre uma lista para escolher suas categorias.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
                    error -> Text("Não foi possível ler as categorias. Volte e tente novamente.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
                    categories == null -> Text("Carregando categorias…", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
                    categories!!.isEmpty() -> Text("Sincronize essa lista para carregar as categorias.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
                    matching.isEmpty() -> Text("Nenhuma categoria encontrada.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF))
                    else -> LazyVerticalGrid(columns = GridCells.Fixed(columns), state = grid, modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        itemsIndexed(matching, key = { _, category -> category.id }) { index, category ->
                            CategoryGridTile(category.name, type, category.id in rules.categories[key].orEmpty() || allBlocked,
                                modifier = (if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                    .focusProperties { if (index < columns) up = searchFocus }) {
                                if (!allBlocked) save { r ->
                                    val old = r.categories[key].orEmpty()
                                    val next = if (category.id in old) old - category.id else old + category.id
                                    r.copy(categories = r.categories + (key to next))
                                }
                            }
                        }
                    }
                }
            }
            Text(message ?: if (busy) "Salvando…" else if (allBlocked) "$blockedCount categorias bloqueadas · Bloqueio da lista inteira ativado"
                else "$blockedCount categorias bloqueadas · Salvo automaticamente", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFA7A9AE), fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(parentalAccent.copy(alpha = .65f)))
        }
    }
    if (searchOpen) Dialog(onDismissRequest = ::closeSearch, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.widthIn(max = 940.dp).fillMaxWidth(.96f)
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * .92f).dp)
            .background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF080A0C), RoundedCornerShape(10.dp)).border(1.dp, parentalAccent, RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PESQUISAR CATEGORIA", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 16.sp)
            TvKeyboardSearchText(categoryQuery, true, cursor, "Pesquisar categoria", Modifier.fillMaxWidth().padding(8.dp))
            AppKeyboard(modifier = Modifier.fillMaxWidth(), actionText = "CONCLUIR", currentText = categoryQuery, enterSubmits = false, themeBorders = true,
                onCharacter = { character -> val edited = tvKeyboardType(categoryQuery, cursor, character); categoryQuery = edited.value; cursor = edited.cursor },
                onBackspace = { val edited = tvKeyboardDelete(categoryQuery, cursor); categoryQuery = edited.value; cursor = edited.cursor },
                onMoveCursorLeft = { cursor = tvKeyboardMoveCursor(categoryQuery, cursor, -1) },
                onMoveCursorRight = { cursor = tvKeyboardMoveCursor(categoryQuery, cursor, 1) },
                onReplaceAll = { categoryQuery = it.take(200); cursor = categoryQuery.length },
                onAction = ::closeSearch)
        }
        }
    }
    if (choosePlaylist) Dialog(onDismissRequest = { choosePlaylist = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 400.dp).fillMaxWidth(.9f).heightIn(max = 360.dp)
            .background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF101214), RoundedCornerShape(10.dp)).border(1.dp, parentalAccent, RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Escolher lista", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 18.sp)
            playlists.forEach { item -> CompactCategoryControl(item.name, Modifier.fillMaxWidth(), selected = current?.id == item.id) { selected = item.id; choosePlaylist = false } }
            CompactCategoryControl("Cancelar", Modifier.fillMaxWidth()) { choosePlaylist = false }
        }
    }
}

private const val categoryLockPath = "M10 21H38V43H10Z M15 21V14A9 9 0 0 1 33 14V21 M24 29V35"

@Composable
private fun CategoryLineIcon(icon: String, color: Color, modifier: Modifier) {
    val path = remember(icon) { PathParser().parsePathString(icon).toPath() }
    Canvas(modifier) { scale(size.width / 48f, size.height / 48f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
        drawPath(path, color, style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } }
}

@Composable
private fun CompactCategoryControl(title: String, modifier: Modifier, selected: Boolean = false,
    icon: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(5.dp)
    Row(modifier.heightIn(min = 36.dp).clip(shape).background(if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor() else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF101315))
        .border(if (focused) 2.dp else 1.dp, parentalFrame, shape)
        .onFocusChanged { focused = it.isFocused }.semantics { if (icon != null) stateDescription = if (selected) "Ativado" else "Desativado" }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) CategoryLineIcon(icon, if (selected) parentalAccent else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFD0D0D3), Modifier.size(16.dp))
        Text(title, color = if (selected || focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 12.sp)
    }
}

@Composable
private fun CategoryGridTile(title: String, type: String, blocked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(modifier.fillMaxWidth().heightIn(min = 42.dp).clip(shape)
        .background(Brush.verticalGradient(if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColors()
            else listOf(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF181A1C), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF101214))))
        .border(if (focused) 2.dp else 1.dp, parentalFrame, shape)
        .onFocusChanged { focused = it.isFocused }
        .semantics { stateDescription = if (blocked) "Bloqueada. PIN necessário." else "Livre" }
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val normalized = remember(title) { java.text.Normalizer.normalize(title.lowercase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "") }
        val icon = when {
            "document" in normalized -> "M10 4H29L39 14V44H10Z M29 4V14H39 M17 22H32 M17 29H32 M17 36H28"
            "esport" in normalized -> "M24 4A20 20 0 1 1 24 44A20 20 0 1 1 24 4 M24 14L34 21L30 33H18L14 21Z M24 4V14 M4 24L14 21 M34 21L44 24 M30 33L36 41 M18 33L12 41"
            "4k" in normalized -> "M4 7H44V35H4Z M24 35V43 M14 43H34"
            "comedia" in normalized -> "M24 4A20 20 0 1 1 24 44A20 20 0 1 1 24 4 M15 17H17 M31 17H33 M14 28Q24 40 34 28"
            "terror" in normalized || "suspense" in normalized -> "M9 42V20A15 15 0 0 1 39 20V42L32 36L24 42L17 36Z M17 19V24 M30 19V24"
            "adult" in normalized || "xxx" in normalized -> "M24 4A20 20 0 1 1 24 44A20 20 0 1 1 24 4 M13 17L17 14V33 M23 17H33V24H23V33H33V24 M38 12V20 M34 16H42"
            "anim" in normalized -> "M10 17L7 5L20 12L28 12L41 5L38 17Q45 25 37 35L31 43H17L11 35Q3 25 10 17 M15 23H18 M30 23H33 M21 32L24 35L27 32"
            type == "SERIES" -> "M8 6H35V34H8Z M14 12H41V41H14 M19 19L29 25L19 31Z"
            else -> "M6 11H42V40H6Z M6 20H42 M12 11L18 20 M25 11L31 20 M24 26L32 30L24 35Z"
        }
        CategoryLineIcon(icon, br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFD0D0D3), Modifier.size(23.dp))
        Text(title, modifier = Modifier.weight(1f), color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 14.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (blocked) {
            val lock = remember { PathParser().parsePathString(categoryLockPath).toPath() }
            Canvas(Modifier.size(19.dp)) { scale(size.width / 48f, size.height / 48f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                drawPath(lock, parentalAccent, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } }
        } else Spacer(Modifier.size(19.dp))
    }
}

@Composable
private fun ParentalForm(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF171717), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF0D0F11))), RoundedCornerShape(14.dp))
            .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ParentalTile(title:String,description:String,icon:String,active:Boolean?,enabled:Boolean,
    modifier:Modifier=Modifier,onClick:()->Unit) {
    var focused by remember{mutableStateOf(false)}
    val shape=RoundedCornerShape(9.dp)
    val look=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook
    Row(modifier.heightIn(min=54.dp).clip(shape)
        .background(if(focused)look.selection else look.panel)
        .border(if(focused)2.dp else 1.dp,look.border(focused),shape)
        .onFocusChanged{focused=it.isFocused}.semantics{stateDescription=when(active){true->"Ativado";false->"Desativado";null->"Abrir categorias"}}
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick).padding(horizontal=14.dp,vertical=9.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        val path=remember(icon){PathParser().parsePathString(icon).toPath()}
        Canvas(Modifier.size(25.dp)) {
            scale(size.width/48f,size.height/48f,pivot=androidx.compose.ui.geometry.Offset.Zero) {
                drawPath(path,if(focused)parentalAccent else look.secondary,style=Stroke(2.2f,cap=StrokeCap.Round,join=StrokeJoin.Round))
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(title,color=look.text,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
            Text(description,color=look.secondary,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
        if(active!=null)br.com.flaviotorres.iptvpremium.ui.components.SettingsSwitchVisual(active)
        else Text("›",color=look.secondary,fontSize=22.sp)
    }
}

@Composable
private fun ParentalAction(title: String, description: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(9.dp)
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
        .background(if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor() else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF111315))
        .border(if (focused) 2.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.border(focused||selected), shape)
        .onFocusChanged { focused = it.isFocused }
        .semantics { if (selected) stateDescription = "Bloqueada" }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = if (enabled) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFAAAAAA), fontSize = 14.sp,
                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (description.isNotEmpty()) Text(description, color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFA7A9AE), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Text("✓", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 18.sp)
    }
}
