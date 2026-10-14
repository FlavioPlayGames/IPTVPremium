$ErrorActionPreference = "Stop"
$relativePath = "app\src\main\java\br\com\flaviotorres\iptvpremium\core\navigation\player\PlaybackScreen.kt"
$playerFile = @($relativePath, "..\$relativePath") | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $playerFile) { throw "Abra o terminal na pasta principal do projeto." }
$playerFile = (Resolve-Path -LiteralPath $playerFile).Path
$bytes = [System.IO.File]::ReadAllBytes($playerFile)
$hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
$encoding = [System.Text.UTF8Encoding]::new($hasBom, $true)
$text = $encoding.GetString($bytes)
if ($hasBom) { $text = $text.Substring(1) }
$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
function Normalize-NewLine([string]$value) { return $value.Replace("`r`n", "`n").Replace("`n", $nl) }
$newHeader = @'
                if (media.type == PlaybackContentType.MOVIE && titleArtwork != null) {
                    // Quando houver logo/titulo artistico oficial, ele assume
                    // o lugar do cartao de texto, sem placa ao redor da arte.
                    Image(
                        bitmap = titleArtwork!!,
                        contentDescription = media.title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = if (isCompactScreen) 16.dp else 30.dp,
                                top = if (isCompactScreen) 16.dp else 28.dp
                            )
                            .width(artworkWidth)
                            .height(artworkHeight)
                    )
                } else {
                    PlayerTitleCard(
                        media = media,
                        gold = gold,
                        compact = isCompactScreen,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = if (isCompactScreen) 16.dp else 30.dp,
                                top = if (isCompactScreen) 16.dp else 28.dp
                            )
                    )
                }
'@
$newOptions = @'
            if (panel != PlayerPanel.NONE) {
                // Fundo levemente escurecido: permite ver o video enquanto o menu
                // permanece legivel, sem modificar a SurfaceView HDR do player.
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f))
                        .clickable { dismissOverlaysToVideo() }
                )
                val menuGlassShape = RoundedCornerShape(14.dp)
                val panelWidth = (LocalConfiguration.current.screenWidthDp *
                    if (isCompactScreen) 0.84f else if (panel == PlayerPanel.INFO) 0.40f else 0.32f
                ).dp.coerceAtMost(if (panel == PlayerPanel.INFO) 560.dp else 420.dp)
                val menuTitle = when (panel) {
                    PlayerPanel.MAIN -> "OPÇÕES"
                    PlayerPanel.ENGINE -> "REPRODUTOR"
                    PlayerPanel.AUDIO -> "FAIXAS DE ÁUDIO"
                    PlayerPanel.SUBTITLES -> "LEGENDAS"
                    PlayerPanel.SPEED -> "VELOCIDADE"
                    PlayerPanel.ASPECT -> "FORMATO DE TELA"
                    PlayerPanel.MANUAL -> "AJUSTE MANUAL"
                    PlayerPanel.INFO -> "INFORMAÇÕES TÉCNICAS"
                    PlayerPanel.NONE -> "OPÇÕES"
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(panelWidth)
                        .then(
                            if (panel == PlayerPanel.INFO) Modifier.fillMaxHeight()
                            else Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp - 24).coerceAtLeast(120).dp)
                        )
                        .padding(
                            start = if (isCompactScreen) 7.dp else 10.dp,
                            end = if (isCompactScreen) 8.dp else 16.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        )
                        .shadow(
                            elevation = 10.dp,
                            shape = menuGlassShape,
                            ambientColor = Color.Black.copy(alpha = 0.35f),
                            spotColor = Color.Black.copy(alpha = 0.45f)
                        )
                        .clip(menuGlassShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x98171B1C),
                                    Color(0xAD0D1112),
                                    Color(0xB8070A0B)
                                )
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFFF6CE).copy(alpha = 0.045f),
                                    Color.White.copy(alpha = 0.015f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            1.1.dp,
                            Brush.linearGradient(listOf(Color(0xFFFFF8CF), gold, Color(0xFFFFED83))),
                            menuGlassShape
                        )
                        .then(
                            if (panel == PlayerPanel.INFO) Modifier
                            else Modifier.verticalScroll(menuScrollState)
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 16.dp
                        )
                ) {
'@
$updated = $text
$headerStart = $updated.IndexOf("                val titleShape = RoundedCornerShape(")
if ($headerStart -ge 0) {
    $headerEnd = $updated.IndexOf("                PlayerCinemaPanel(", $headerStart)
    if ($headerEnd -lt 0) { throw "Fim do cabecalho nao encontrado. Nenhum arquivo foi alterado." }
    $updated = $updated.Substring(0, $headerStart) + (Normalize-NewLine $newHeader) + $nl + $nl + $updated.Substring($headerEnd)
} elseif (-not $updated.Contains("                    PlayerTitleCard(")) {
    throw "Cabecalho diferente do esperado. Nenhum arquivo foi alterado."
}
$optionsStart = $updated.IndexOf("            if (panel != PlayerPanel.NONE) {")
if ($optionsStart -lt 0) { throw "Painel de opcoes nao encontrado. Nenhum arquivo foi alterado." }
$optionsEnd = $updated.IndexOf("                    Text(menuTitle,", $optionsStart)
if ($optionsEnd -lt 0) { throw "Titulo do painel nao encontrado. Nenhum arquivo foi alterado." }
$updated = $updated.Substring(0, $optionsStart) + (Normalize-NewLine $newOptions) + $nl + $updated.Substring($optionsEnd)
$newFunction0 = @'
@Composable
private fun PlayerTitleCard(
    media: PlaybackMedia,
    gold: Color,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val isEpisode = media.type == PlaybackContentType.EPISODE
    val mainTitle = if (isEpisode) {
        media.subtitleSearchTitle?.trim()?.takeIf { it.isNotBlank() } ?: media.title
    } else media.title
    val details = when (media.type) {
        PlaybackContentType.LIVE_CHANNEL -> "AO VIVO"
        PlaybackContentType.MOVIE -> "FILME"
        PlaybackContentType.EPISODE -> buildList {
            media.seasonNumber?.takeIf { it >= 0 }?.let {
                add(if (it == 0) "Especiais" else "Temporada $it")
            }
            media.episodeNumber?.takeIf { it > 0 }?.let { add("Episódio $it") }
        }.joinToString(" · ").ifBlank { "SÉRIE" }
    }
    val maxWidth = (LocalConfiguration.current.screenWidthDp * if (compact) 0.88f else 0.64f).dp
    val shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    Column(
        modifier = modifier
            .widthIn(max = maxWidth)
            .wrapContentWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(Color(0xAA141819), Color(0xB8090C0E)))
            )
            .border(
                1.1.dp,
                Brush.linearGradient(
                    listOf(Color(0xFFFFF8CF), gold, Color(0xFFFFED83), Color(0xFFFFD339))
                ),
                shape
            )
            .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = mainTitle,
            color = Color(0xFFFFF9EC),
            fontSize = if (compact) 18.sp else 21.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = details,
            color = gold,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
'@
$pattern0 = '(?ms)^@Composable\r?\nprivate fun PlayerTitleCard\(.*?^\}'
$count = [regex]::Matches($updated, $pattern0).Count
$replacement = Normalize-NewLine $newFunction0
if ($count -eq 1) {
    $match = [regex]::Match($updated, $pattern0)
    $updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
} elseif ($count -eq 0) {
    $anchor = '@Composable' + $nl + 'private fun PlayerCinemaPanel('
    if (-not $updated.Contains($anchor)) { throw "Ponto de insercao do titulo nao encontrado. Nenhum arquivo foi alterado." }
    $updated = $updated.Replace($anchor, $replacement + $nl + $nl + $anchor)
} else { throw "PlayerTitleCard nao encontrado de forma unica. Nenhum arquivo foi alterado." }
$newFunction1 = @'
@Composable
private fun PlayerCinemaPanel(
    compact: Boolean,
    gold: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 12.dp else 14.dp)
    val goldRim = Brush.linearGradient(
        listOf(
            Color(0xFFFFF8CF),
            Color(0xFFFFD339),
            Color(0xFFFFED83),
            Color(0xFFE9B628),
            Color(0xFFFFF2AC)
        )
    )
    val visualScale = 0.5f
    val density = LocalDensity.current
    val originalGapPx = with(density) { 5.dp.roundToPx() }

    Layout(
        content = content,
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x701E211F),
                        Color(0x80110F0D),
                        Color(0x95090807)
                    )
                )
            )
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, gold.copy(alpha = 0.76f), Color.Transparent)
                    ),
                    start = Offset(size.width * 0.08f, size.height - 1.dp.toPx()),
                    end = Offset(size.width * 0.92f, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(1.1.dp, goldRim, shape)
            .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 6.5.dp)
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(
            minWidth = (constraints.minWidth / visualScale).toInt(),
            maxWidth = (constraints.maxWidth / visualScale).toInt(),
            minHeight = 0
        )
        val children = measurables.map { it.measure(childConstraints) }
        val originalHeight = children.sumOf { it.height } +
            originalGapPx * (children.size - 1).coerceAtLeast(0)
        // Mantém as proporções aprovadas, agora com metade da altura visual.
        val extraHeight = 0
        val width = kotlin.math.round((children.maxOfOrNull { it.width } ?: 0) * visualScale).toInt()
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = kotlin.math.round((originalHeight + extraHeight) * visualScale).toInt()
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var y = 0
            children.forEachIndexed { index, child ->
                child.placeRelativeWithLayer(0, kotlin.math.round(y * visualScale).toInt()) {
                    scaleX = visualScale
                    scaleY = visualScale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                y += child.height
                if (index < children.lastIndex) {
                    y += originalGapPx
                }
            }
        }
    }
}
'@
$pattern1 = '(?ms)^@Composable\r?\nprivate fun PlayerCinemaPanel\(.*?^\}'
$count = [regex]::Matches($updated, $pattern1).Count
$replacement = Normalize-NewLine $newFunction1
if ($count -eq 1) {
    $match = [regex]::Match($updated, $pattern1)
    $updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
} else { throw "PlayerCinemaPanel nao encontrado de forma unica. Nenhum arquivo foi alterado." }
$newFunction2 = @'
@Composable
private fun PlayerIconButton(
    icon: PlayerGlyph,
    label: String,
    gold: Color,
    focusRequester: FocusRequester? = null,
    prominent: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = CircleShape
    val requestModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else Modifier
    val buttonSize = when {
        prominent && compact -> 70.dp
        prominent -> 76.dp
        compact -> 49.dp
        else -> 54.dp
    }
    val activeGlow = focused
    Box(
        modifier = Modifier
            .then(requestModifier)
            .semantics { contentDescription = label }
            .size(buttonSize)
            .onFocusChanged { focused = it.isFocused }
            // Halo dourado curto: brilho discreto apenas ao redor da borda;
            // sem espalhar uma mancha de luz pelo painel inteiro.
            .drawBehind {
                if (activeGlow) {
                    val haloRadius = size.minDimension * 0.66f
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to gold.copy(alpha = if (prominent) 0.34f else 0.18f),
                            0.68f to gold.copy(alpha = if (prominent) 0.15f else 0.07f),
                            1.0f to Color.Transparent,
                            center = center,
                            radius = haloRadius
                        ),
                        radius = haloRadius
                    )
                }
            }
            .shadow(
                elevation = if (focused && prominent) 5.dp else if (focused) 3.dp else 0.dp,
                shape = shape,
                ambientColor = gold.copy(alpha = if (activeGlow) 0.24f else 0f),
                spotColor = gold.copy(alpha = if (activeGlow) 0.23f else 0f)
            )
            .clip(shape)
            .background(
                if (focused) {
                    // Fundo ambar radial, aro dourado iluminado: referencia.
                    Brush.radialGradient(
                        0.0f to Color(0xFFFFE566),
                        0.56f to Color(0xFFE8BA25),
                        1.0f to Color(0xFFB88214)
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .border(
                width = if (focused && prominent) 2.dp else if (focused) 1.7.dp else 1.2.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFFF7CB),
                        Color(0xFFFFD23A),
                        Color(0xFFFFED79),
                        Color(0xFFE2B427),
                        Color(0xFFFFF7CB)
                    )
                ),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Aro de reflexo interno sutil; nao interfere no foco/controle remoto.
        if (focused) {
            Box(
                Modifier.fillMaxSize()
                    .padding(3.dp)
                    .border(0.6.dp, Color(0xBEFFF2AA), CircleShape)
            )
        }
        PlayerGlyphDrawing(
            glyph = icon,
            color = if (focused) Color(0xFFFFF8CD) else Color(0xFFFFE96D),
            // Ícones a 90% do desenho anterior; área clicável permanece igual.
            modifier = Modifier.size(if (compact) 23.dp else 26.dp)
        )
    }
}
'@
$pattern2 = '(?ms)^@Composable\r?\nprivate fun PlayerIconButton\(.*?^\}'
$count = [regex]::Matches($updated, $pattern2).Count
$replacement = Normalize-NewLine $newFunction2
if ($count -eq 1) {
    $match = [regex]::Match($updated, $pattern2)
    $updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
} else { throw "PlayerIconButton nao encontrado de forma unica. Nenhum arquivo foi alterado." }
$newFunction3 = @'
@Composable
private fun PlayerMenuItem(
    label: String,
    description: String,
    accent: Color,
    focusRequester: FocusRequester? = null,
    glyph: PlayerMenuGlyph? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            // O halo permanece pequeno e encostado no contorno, como no player.
            .shadow(
                elevation = if (focused) 3.dp else 0.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = if (focused) 0.18f else 0f),
                spotColor = accent.copy(alpha = if (focused) 0.18f else 0f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (focused) listOf(
                        accent.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.42f)
                    ) else listOf(
                        Color(0x55303538), Color(0x66090D10)
                    )
                )
            )
            .border(
                width = if (focused) 1.2.dp else 0.8.dp,
                color = if (focused) accent.copy(alpha = 0.92f)
                else accent.copy(alpha = 0.38f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (glyph != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x9B101519))
                        .border(
                            width = 0.8.dp,
                            color = if (focused) accent.copy(alpha = 0.72f)
                            else accent.copy(alpha = 0.55f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerMenuGlyphDrawing(
                        glyph = glyph,
                        color = accent.copy(alpha = if (focused) 1f else 0.90f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = if (focused) accent else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.isNotBlank()) {
                    Text(
                        description,
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
'@
$pattern3 = '(?ms)^@Composable\r?\nprivate fun PlayerMenuItem\(.*?^\}'
$count = [regex]::Matches($updated, $pattern3).Count
$replacement = Normalize-NewLine $newFunction3
if ($count -eq 1) {
    $match = [regex]::Match($updated, $pattern3)
    $updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
} else { throw "PlayerMenuItem nao encontrado de forma unica. Nenhum arquivo foi alterado." }
foreach ($name in @("widthIn", "wrapContentWidth", "heightIn")) {
    $exactImport = 'import androidx.compose.foundation.layout.' + $name
    if (-not [regex]::IsMatch($updated, '(?m)^' + [regex]::Escape($exactImport) + '\r?$')) {
        $importPattern = '(?m)^import androidx\.compose\.foundation\.layout\.width\r?$'
        if ([regex]::Matches($updated, $importPattern).Count -ne 1) { throw "Import esperado nao encontrado. Nenhum arquivo foi alterado." }
        $updated = [regex]::Replace($updated, $importPattern, 'import androidx.compose.foundation.layout.width' + $nl + $exactImport)
    }
}
if ($updated -ceq $text) {
    Write-Host "As alteracoes ja estao aplicadas." -ForegroundColor Yellow
} else {
    $backup = "$playerFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmssfff')"
    [System.IO.File]::WriteAllBytes($backup, $bytes)
    [System.IO.File]::WriteAllText($playerFile, $updated, $encoding)
    Write-Host "Titulo adaptavel, opcoes compactas e botao transparente sem foco aplicados." -ForegroundColor Green
    Write-Host "Backup: $backup"
}
Write-Host "Compile e execute novamente na TV."
