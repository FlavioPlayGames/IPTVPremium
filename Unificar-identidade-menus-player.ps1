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
$updated = $text
$newOpening = @'
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
                        .background(playerGlassBrush())
                        .border(1.1.dp, playerGoldFrameBrush(), menuGlassShape)
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
$newPalette = @'
// BEGIN_PLAYER_SHARED_PALETTE
private fun playerGlassBrush(): Brush = Brush.verticalGradient(
    listOf(Color(0x701E211F), Color(0x80110F0D), Color(0x95090807))
)

private fun playerGoldFrameBrush(alpha: Float = 1f): Brush = Brush.linearGradient(
    listOf(Color(0xFFFFF4BD), Color(0xFFB98132), Color(0xFFFFE991), Color(0xFF9E761B), Color(0xFFFFE991))
        .map { it.copy(alpha = alpha) }
)

private fun playerGoldCircleBrush(): Brush = Brush.sweepGradient(
    listOf(Color(0xFFFFF4BD), Color(0xFF9E761B), Color(0xFFFFE991), Color(0xFF8A6414), Color(0xFFFFF4BD))
)

private fun playerGoldSelectionBrush(alpha: Float = 1f): Brush = Brush.radialGradient(
    0f to Color(0xFFE3B83F).copy(alpha = alpha),
    0.56f to Color(0xFFBE8B20).copy(alpha = alpha),
    1f to Color(0xFF81540F).copy(alpha = alpha)
)

private fun playerGoldIconBrush(color: Color): Brush = Brush.verticalGradient(
    listOf(Color(0xFFFFF4BD).copy(alpha = color.alpha), color, Color(0xFFC49A2C).copy(alpha = color.alpha))
)
// END_PLAYER_SHARED_PALETTE
'@
$start = $updated.IndexOf("            if (panel != PlayerPanel.NONE) {")
if ($start -lt 0) { throw "Painel de opcoes nao encontrado. Nenhum arquivo foi alterado." }
$end = $updated.IndexOf("                    Text(menuTitle,", $start)
if ($end -lt 0) { throw "Fim do painel nao encontrado. Nenhum arquivo foi alterado." }
$updated = $updated.Substring(0, $start) + (Normalize-NewLine $newOpening) + $nl + $updated.Substring($end)
$newFunction0 = @'
@Composable
private fun PlayerCinemaPanel(
    compact: Boolean,
    gold: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 12.dp else 14.dp)
    val goldRim = playerGoldFrameBrush()
    val visualScale = 0.5f
    val density = LocalDensity.current
    val originalGapPx = with(density) { 5.dp.roundToPx() }

    Layout(
        content = content,
        modifier = modifier
            .clip(shape)
            .background(playerGlassBrush())
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
$pattern0 = '(?ms)^@Composable\r?\nprivate fun PlayerCinemaPanel\(.*?^\}'
if ([regex]::Matches($updated, $pattern0).Count -ne 1) { throw "PlayerCinemaPanel diferente do esperado. Nenhum arquivo foi alterado." }
$match = [regex]::Match($updated, $pattern0)
$replacement = Normalize-NewLine $newFunction0
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
$newFunction1 = @'
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
                    playerGoldSelectionBrush()
                } else {
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .border(
                width = if (focused && prominent) 2.dp else if (focused) 1.7.dp else 1.2.dp,
                brush = playerGoldCircleBrush(),
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
                    .border(0.6.dp, Color(0xBEFFF4BD), CircleShape)
            )
        }
        PlayerGlyphDrawing(
            glyph = icon,
            color = if (focused) Color(0xFFFFF4BD) else Color(0xFFFFE991),
            // Ícones a 90% do desenho anterior; área clicável permanece igual.
            modifier = Modifier.size(if (compact) 23.dp else 26.dp)
        )
    }
}
'@
$pattern1 = '(?ms)^@Composable\r?\nprivate fun PlayerIconButton\(.*?^\}'
if ([regex]::Matches($updated, $pattern1).Count -ne 1) { throw "PlayerIconButton diferente do esperado. Nenhum arquivo foi alterado." }
$match = [regex]::Match($updated, $pattern1)
$replacement = Normalize-NewLine $newFunction1
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
$newFunction2 = @'
@Composable
private fun PlayerGlyphDrawing(
    glyph: PlayerGlyph,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val iconBrush = playerGoldIconBrush(color)
        val w = size.width
        val h = size.height
        fun triangle(points: List<Offset>) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                close()
            }
            drawPath(path, iconBrush)
        }
        when (glyph) {
            PlayerGlyph.PLAY -> triangle(
                listOf(Offset(w * .25f, h * .12f), Offset(w * .85f, h * .5f), Offset(w * .25f, h * .88f))
            )
            PlayerGlyph.PAUSE -> {
                drawRoundRect(iconBrush, Offset(w * .22f, h * .16f), Size(w * .20f, h * .68f), CornerRadius(w * .04f))
                drawRoundRect(iconBrush, Offset(w * .58f, h * .16f), Size(w * .20f, h * .68f), CornerRadius(w * .04f))
            }
            PlayerGlyph.BACK -> {
                triangle(listOf(Offset(w * .47f, h * .17f), Offset(w * .10f, h * .5f), Offset(w * .47f, h * .83f)))
                triangle(listOf(Offset(w * .91f, h * .17f), Offset(w * .54f, h * .5f), Offset(w * .91f, h * .83f)))
            }
            PlayerGlyph.FORWARD -> {
                triangle(listOf(Offset(w * .09f, h * .17f), Offset(w * .46f, h * .5f), Offset(w * .09f, h * .83f)))
                triangle(listOf(Offset(w * .53f, h * .17f), Offset(w * .90f, h * .5f), Offset(w * .53f, h * .83f)))
            }
            PlayerGlyph.AUDIO -> {
                // Alto-falante com duas ondas: não depende de Material Icons.
                val speaker = Path().apply {
                    moveTo(w * .12f, h * .37f)
                    lineTo(w * .34f, h * .37f)
                    lineTo(w * .57f, h * .17f)
                    lineTo(w * .57f, h * .83f)
                    lineTo(w * .34f, h * .63f)
                    lineTo(w * .12f, h * .63f)
                    close()
                }
                drawPath(speaker, color)
                drawArc(iconBrush, -65f, 130f, false,
                    Offset(w * .44f, h * .22f), Size(w * .35f, h * .56f),
                    style = Stroke(width = w * .075f))
                drawArc(iconBrush, -60f, 120f, false,
                    Offset(w * .43f, h * .08f), Size(w * .53f, h * .84f),
                    style = Stroke(width = w * .065f))
            }
            PlayerGlyph.SUBTITLES -> {
                drawRoundRect(iconBrush, Offset(w * .07f, h * .18f), Size(w * .86f, h * .65f),
                    CornerRadius(w * .09f), style = Stroke(width = w * .085f))
                drawLine(iconBrush, Offset(w * .20f, h * .48f), Offset(w * .44f, h * .48f), w * .10f)
                drawLine(iconBrush, Offset(w * .56f, h * .48f), Offset(w * .80f, h * .48f), w * .10f)
                drawLine(iconBrush, Offset(w * .20f, h * .66f), Offset(w * .80f, h * .66f), w * .08f)
            }
            PlayerGlyph.ASPECT -> {
                // Quatro cantos de tela cheia, como na referencia aprovada.
                val thickness = w * .085f
                drawLine(iconBrush, Offset(w * .09f, h * .38f), Offset(w * .09f, h * .09f), thickness)
                drawLine(iconBrush, Offset(w * .09f, h * .09f), Offset(w * .38f, h * .09f), thickness)
                drawLine(iconBrush, Offset(w * .62f, h * .09f), Offset(w * .91f, h * .09f), thickness)
                drawLine(iconBrush, Offset(w * .91f, h * .09f), Offset(w * .91f, h * .38f), thickness)
                drawLine(iconBrush, Offset(w * .09f, h * .62f), Offset(w * .09f, h * .91f), thickness)
                drawLine(iconBrush, Offset(w * .09f, h * .91f), Offset(w * .38f, h * .91f), thickness)
                drawLine(iconBrush, Offset(w * .62f, h * .91f), Offset(w * .91f, h * .91f), thickness)
                drawLine(iconBrush, Offset(w * .91f, h * .62f), Offset(w * .91f, h * .91f), thickness)
            }
            PlayerGlyph.SETTINGS -> {
                drawCircle(iconBrush, radius = w * .28f, style = Stroke(width = w * .12f))
                drawCircle(iconBrush, radius = w * .10f)
                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4.0).toFloat()
                    val cx = w * .5f
                    val cy = h * .5f
                    val c = cos(angle)
                    val s = sin(angle)
                    drawLine(
                        color,
                        Offset(cx + c * w * .33f, cy + s * h * .33f),
                        Offset(cx + c * w * .45f, cy + s * h * .45f),
                        strokeWidth = w * .10f
                    )
                }
            }
        }
    }
}
'@
$pattern2 = '(?ms)^@Composable\r?\nprivate fun PlayerGlyphDrawing\(.*?^\}'
if ([regex]::Matches($updated, $pattern2).Count -ne 1) { throw "PlayerGlyphDrawing diferente do esperado. Nenhum arquivo foi alterado." }
$match = [regex]::Match($updated, $pattern2)
$replacement = Normalize-NewLine $newFunction2
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
$newFunction3 = @'
@Composable
private fun PlayerMenuGlyphDrawing(
    glyph: PlayerMenuGlyph,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (glyph == PlayerMenuGlyph.SCREEN) {
        PlayerGlyphDrawing(PlayerGlyph.ASPECT, color, modifier)
        return
    }
    Canvas(modifier) {
        val iconBrush = playerGoldIconBrush(color)
        val w = size.width
        val h = size.height
        val stroke = w * 0.095f
        when (glyph) {
            PlayerMenuGlyph.PLAYER -> {
                drawRoundRect(
                    color, Offset(w * 0.10f, h * 0.16f),
                    Size(w * 0.80f, h * 0.58f),
                    CornerRadius(w * 0.085f), style = Stroke(stroke)
                )
                drawLine(iconBrush, Offset(w * 0.37f, h * 0.89f), Offset(w * 0.63f, h * 0.89f), stroke)
                drawLine(iconBrush, Offset(w * 0.50f, h * 0.74f), Offset(w * 0.50f, h * 0.88f), stroke)
                val play = Path().apply {
                    moveTo(w * 0.43f, h * 0.29f)
                    lineTo(w * 0.65f, h * 0.45f)
                    lineTo(w * 0.43f, h * 0.61f)
                    close()
                }
                drawPath(play, color)
            }
            PlayerMenuGlyph.SPEED -> {
                drawArc(
                    color, startAngle = 155f, sweepAngle = 230f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.15f),
                    size = Size(w * 0.76f, h * 0.76f),
                    style = Stroke(width = stroke)
                )
                drawLine(iconBrush, Offset(w * 0.49f, h * 0.54f), Offset(w * 0.73f, h * 0.31f), stroke)
                drawCircle(iconBrush, radius = w * 0.075f, center = Offset(w * 0.49f, h * 0.54f))
            }
            PlayerMenuGlyph.INFO -> {
                drawCircle(iconBrush, radius = w * 0.39f, style = Stroke(width = stroke))
                drawCircle(iconBrush, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.30f))
                drawLine(iconBrush, Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.70f), stroke)
            }
            PlayerMenuGlyph.CLOSE -> {
                drawLine(iconBrush, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), stroke)
                drawLine(iconBrush, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), stroke)
            }
            PlayerMenuGlyph.SCREEN -> Unit
        }
    }
}
'@
$pattern3 = '(?ms)^@Composable\r?\nprivate fun PlayerMenuGlyphDrawing\(.*?^\}'
if ([regex]::Matches($updated, $pattern3).Count -ne 1) { throw "PlayerMenuGlyphDrawing diferente do esperado. Nenhum arquivo foi alterado." }
$match = [regex]::Match($updated, $pattern3)
$replacement = Normalize-NewLine $newFunction3
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
$newFunction4 = @'
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
                if (focused) playerGoldSelectionBrush(alpha = 0.25f)
                else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .border(
                width = if (focused) 1.2.dp else 0.8.dp,
                brush = playerGoldFrameBrush(alpha = if (focused) 1f else 0.50f),
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
                        .background(if (focused) playerGoldSelectionBrush() else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                        .border(
                            width = 0.8.dp,
                            brush = playerGoldCircleBrush(),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerMenuGlyphDrawing(
                        glyph = glyph,
                        color = if (focused) Color(0xFFFFF4BD) else Color(0xFFFFE991),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = if (focused) Color(0xFFFFF4BD) else Color.White,
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
$pattern4 = '(?ms)^@Composable\r?\nprivate fun PlayerMenuItem\(.*?^\}'
if ([regex]::Matches($updated, $pattern4).Count -ne 1) { throw "PlayerMenuItem diferente do esperado. Nenhum arquivo foi alterado." }
$match = [regex]::Match($updated, $pattern4)
$replacement = Normalize-NewLine $newFunction4
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
$palettePattern = '(?ms)^// BEGIN_PLAYER_SHARED_PALETTE.*?^// END_PLAYER_SHARED_PALETTE'
$count = [regex]::Matches($updated, $palettePattern).Count
$palette = Normalize-NewLine $newPalette
if ($count -eq 1) {
    $match = [regex]::Match($updated, $palettePattern)
    $updated = $updated.Substring(0, $match.Index) + $palette + $updated.Substring($match.Index + $match.Length)
} elseif ($count -eq 0) {
    $anchor = '@Composable' + $nl + 'private fun PlayerCinemaPanel('
    if (-not $updated.Contains($anchor)) { throw "Ponto de insercao nao encontrado. Nenhum arquivo foi alterado." }
    $updated = $updated.Replace($anchor, $palette + $nl + $nl + $anchor)
} else { throw "Mais de uma paleta encontrada. Nenhum arquivo foi alterado." }
if ($updated -ceq $text) {
    Write-Host "Identidade visual ja aplicada." -ForegroundColor Yellow
} else {
    $backup = "$playerFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmssfff')"
    [System.IO.File]::WriteAllBytes($backup, $bytes)
    [System.IO.File]::WriteAllText($playerFile, $updated, $encoding)
    Write-Host "Menu de opcoes e barra do player agora compartilham vidro, dourado e icones metalicos." -ForegroundColor Green
    Write-Host "Backup: $backup"
}
Write-Host "Compile e execute novamente na TV."
