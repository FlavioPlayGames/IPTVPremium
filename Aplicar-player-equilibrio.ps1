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
$callIndex = $text.IndexOf("                PlayerCinemaPanel(")
if ($callIndex -lt 0) { throw "A barra anterior nao foi encontrada. Nenhuma alteracao foi feita." }
$timelineStart = $text.IndexOf("                    Row(verticalAlignment = Alignment.CenterVertically)", $callIndex)
if ($timelineStart -lt 0) { throw "Linha do tempo nao encontrada. Nenhuma alteracao foi feita." }
$timelineEnd = $text.IndexOf("                    // O trio de transporte", $timelineStart)
if ($timelineEnd -lt 0) { throw "Controles nao encontrados. Nenhuma alteracao foi feita." }
$newTimeline = @'
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isLive) "AO VIVO" else formatPlaybackTime(seekTargetMs ?: playback.positionMs),
                            color = if (seekTargetMs != null) gold else Color.White,
                            fontSize = if (isCompactScreen) 16.sp else 18.sp
                        )
                        Spacer(Modifier.width(18.dp))
                        Box(Modifier.weight(1f)) {
                            if (!isLive && playback.durationMs != null && playback.durationMs!! > 0L) {
                                val target = seekTargetMs
                                // O tempo no topo e o marcador dourado fornecem feedback
                                // sem alterar a altura do painel ao segurar as setas.
                                PlayerProgressBar(
                                    progress = if (target != null) {
                                        (target.toFloat() / playback.durationMs!!.toFloat()).coerceIn(0f, 1f)
                                    } else playback.progressFraction,
                                    durationMs = playback.durationMs!!,
                                    gold = gold,
                                    focusRequester = seekFocus,
                                    onFocused = { seekFocused = it },
                                    onScrubStart = { reveal() },
                                    onScrubEnd = { commitSeek() },
                                    onScrubCancel = { seekTargetMs = null },
                                    onSeek = { position ->
                                        if (playback.canSeek) {
                                            dpadScrubbing = false
                                            seekTargetMs = position
                                            reveal()
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    Modifier.fillMaxWidth().height(4.dp)
                                        .background(gold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Text(
                            if (isLive) "Ao vivo" else formatPlaybackTime(playback.durationMs ?: 0L),
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = if (isCompactScreen) 16.sp else 18.sp
                        )
                    }
'@
$newTimeline = (Normalize-NewLine $newTimeline) + $nl + $nl
$updated = $text.Substring(0, $timelineStart) + $newTimeline + $text.Substring($timelineEnd)
$controlStart = $updated.IndexOf("                    // O trio de transporte", $callIndex)
$controlEnd = $updated.IndexOf("            if (panel != PlayerPanel.NONE)", $controlStart)
if ($controlEnd -lt 0) { throw "Fim dos controles nao encontrado. Nenhuma alteracao foi feita." }
$controlText = $updated.Substring($controlStart, $controlEnd - $controlStart)
$heightPattern = '\.height\(if \(isCompactScreen\) [0-9.]+\.dp else [0-9.]+\.dp\)'
if ([regex]::Matches($controlText, $heightPattern).Count -ne 1) { throw "Altura dos controles diferente do esperado. Nenhuma alteracao foi feita." }
$controlText = [regex]::Replace($controlText, $heightPattern, '.height(if (isCompactScreen) 74.dp else 80.dp)')
$updated = $updated.Substring(0, $controlStart) + $controlText + $updated.Substring($controlEnd)
$newFunction0 = @'
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
            Color(0xFFFFF0B7),
            Color(0xFFB98132),
            Color(0xFFFFD36D),
            Color(0xFF956024),
            Color(0xFFFFE69E)
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
                        listOf(Color.Transparent, gold.copy(alpha = 0.56f), Color.Transparent)
                    ),
                    start = Offset(size.width * 0.08f, size.height - 1.dp.toPx()),
                    end = Offset(size.width * 0.92f, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(0.8.dp, goldRim, shape)
            .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 3.5.dp)
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
if ([regex]::Matches($updated, $pattern0).Count -ne 1) { throw "PlayerCinemaPanel nao encontrado de forma unica. Nenhuma alteracao foi feita." }
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
    val activeGlow = prominent || focused
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
                elevation = if (prominent) 5.dp else if (focused) 3.dp else 0.dp,
                shape = shape,
                ambientColor = gold.copy(alpha = if (activeGlow) 0.24f else 0f),
                spotColor = gold.copy(alpha = if (activeGlow) 0.23f else 0f)
            )
            .clip(shape)
            .background(
                if (prominent) {
                    // Fundo ambar radial, aro dourado iluminado: referencia.
                    Brush.radialGradient(
                        0.0f to Color(0xFFD5A620),
                        0.56f to Color(0xFFAE780B),
                        1.0f to Color(0xFF603C05)
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xA62A241A), Color(0xCC0B0A08))
                    )
                }
            )
            .border(
                width = if (prominent) 2.dp else if (focused) 1.7.dp else 1.2.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFFEDB0),
                        Color(0xFFAC7426),
                        Color(0xFFFFD369),
                        Color(0xFF8C591C),
                        Color(0xFFFFEDB0)
                    )
                ),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Aro de reflexo interno sutil; nao interfere no foco/controle remoto.
        if (prominent || focused) {
            Box(
                Modifier.fillMaxSize()
                    .padding(3.dp)
                    .border(0.6.dp, Color(0x70FFEB9D), CircleShape)
            )
        }
        PlayerGlyphDrawing(
            glyph = icon,
            color = if (prominent) Color(0xFFFFF0B9) else Color(0xFFFFD579),
            // Ícones a 90% do desenho anterior; área clicável permanece igual.
            modifier = Modifier.size(if (compact) 23.dp else 26.dp)
        )
    }
}
'@
$pattern1 = '(?ms)^@Composable\r?\nprivate fun PlayerIconButton\(.*?^\}'
if ([regex]::Matches($updated, $pattern1).Count -ne 1) { throw "PlayerIconButton nao encontrado de forma unica. Nenhuma alteracao foi feita." }
$match = [regex]::Match($updated, $pattern1)
$replacement = Normalize-NewLine $newFunction1
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
$newFunction2 = @'
@Composable
private fun PlayerProgressBar(
    progress: Float,
    durationMs: Long,
    gold: Color,
    focusRequester: FocusRequester,
    onFocused: (Boolean) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    onScrubCancel: () -> Unit,
    onSeek: (Long) -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val currentSeek by rememberUpdatedState(onSeek)
    val currentStart by rememberUpdatedState(onScrubStart)
    val currentEnd by rememberUpdatedState(onScrubEnd)
    val currentCancel by rememberUpdatedState(onScrubCancel)
    Canvas(
        modifier = Modifier.fillMaxWidth()
            .height(24.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.isFocused
                onFocused(it.isFocused)
            }
            .focusable()
            .pointerInput(durationMs) {
                fun update(x: Float) {
                    if (durationMs > 0L && size.width > 0) {
                        val fraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                        currentSeek((durationMs * fraction).toLong())
                    }
                }
                detectDragGestures(
                    onDragStart = { position -> currentStart(); update(position.x) },
                    onDragEnd = { currentEnd() },
                    onDragCancel = { currentCancel() },
                    onDrag = { change, _ -> change.consume(); update(change.position.x) }
                )
            }
            .pointerInput(durationMs) {
                detectTapGestures { tap ->
                    if (durationMs > 0L && size.width > 0) {
                        val fraction = (tap.x / size.width.toFloat()).coerceIn(0f, 1f)
                        currentStart()
                        currentSeek((durationMs * fraction).toLong())
                        currentEnd()
                    }
                }
            }
    ) {
        val fraction = progress.coerceIn(0f, 1f)
        val lineHeight = 2.7.dp.toPx()
        val lineTop = (size.height - lineHeight) / 2f
        val thumbRadius = 8.dp.toPx()
        val thumbCenter = Offset(
            x = (size.width * fraction).coerceIn(
                thumbRadius + 0.5.dp.toPx(),
                (size.width - thumbRadius - 0.5.dp.toPx()).coerceAtLeast(thumbRadius + 0.5.dp.toPx())
            ),
            y = size.height / 2f
        )
        // Trilha cinza bem fina, com pontas suavemente arredondadas.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.48f),
            topLeft = Offset(0f, lineTop),
            size = Size(size.width, lineHeight),
            cornerRadius = CornerRadius(lineHeight / 2f)
        )
        if (fraction > 0f) {
            // Halo linear ao longo do trecho reproduzido.
            val glowHeight = 10.dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.5f to gold.copy(alpha = 0.42f),
                    1.0f to Color.Transparent,
                    startY = size.height / 2f - glowHeight / 2f,
                    endY = size.height / 2f + glowHeight / 2f
                ),
                topLeft = Offset(0f, size.height / 2f - glowHeight / 2f),
                size = Size(size.width * fraction, glowHeight),
                cornerRadius = CornerRadius(glowHeight / 2f)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFE5AA3C), Color(0xFFFFD779), Color(0xFFFFE9B4))
                ),
                topLeft = Offset(0f, lineTop),
                size = Size(size.width * fraction, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2f)
            )
        }
        // O foco muda apenas o halo; o acabamento metálico permanece.
        if (hasFocus) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to gold.copy(alpha = 0.32f),
                    0.55f to gold.copy(alpha = 0.17f),
                    1.0f to Color.Transparent,
                    center = thumbCenter,
                    radius = 19.dp.toPx()
                ),
                center = thumbCenter,
                radius = 19.dp.toPx()
            )
            drawCircle(
                color = Color(0xFFFFE9AE).copy(alpha = 0.78f),
                radius = 12.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gold.copy(alpha = 0.48f), Color.Transparent),
                center = thumbCenter,
                radius = 11.dp.toPx()
            ),
            center = thumbCenter,
            radius = 11.dp.toPx()
        )
        val highlight = thumbCenter - Offset(thumbRadius * 0.30f, thumbRadius * 0.35f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFF8E2).copy(alpha = 0.92f),
                0.42f to Color(0xFFFFE7AA).copy(alpha = 0.80f),
                0.76f to Color(0xFFECC15F).copy(alpha = 0.68f),
                1f to Color(0xFFAD7629).copy(alpha = 0.48f),
                center = highlight,
                radius = thumbRadius * 1.65f
            ),
            radius = thumbRadius,
            center = thumbCenter
        )
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Color(0xFFFFF0BA).copy(alpha = 0.72f),
                    Color(0xFFAD7629).copy(alpha = 0.48f),
                    Color(0xFFFFF0BA).copy(alpha = 0.72f)
                )
            ),
            radius = thumbRadius,
            center = thumbCenter,
            style = Stroke(width = 0.6.dp.toPx())
        )
    }
}
'@
$pattern2 = '(?ms)^@Composable\r?\nprivate fun PlayerProgressBar\(.*?^\}'
if ([regex]::Matches($updated, $pattern2).Count -ne 1) { throw "PlayerProgressBar nao encontrado de forma unica. Nenhuma alteracao foi feita." }
$match = [regex]::Match($updated, $pattern2)
$replacement = Normalize-NewLine $newFunction2
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
if (-not [regex]::IsMatch($updated, '(?m)^import androidx\.compose\.ui\.graphics\.TransformOrigin\r?$')) {
    $importPattern = '(?m)^import androidx\.compose\.ui\.graphics\.ImageBitmap\r?$'
    if ([regex]::Matches($updated, $importPattern).Count -ne 1) { throw "Import esperado nao encontrado. Nenhuma alteracao foi feita." }
    $updated = [regex]::Replace($updated, $importPattern, "import androidx.compose.ui.graphics.TransformOrigin$nl" + "import androidx.compose.ui.graphics.ImageBitmap")
}
if ($updated -ceq $text) {
    Write-Host "O menu Equilibrio ja esta aplicado." -ForegroundColor Yellow
} else {
    $backup = "$playerFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmssfff')"
    [System.IO.File]::WriteAllBytes($backup, $bytes)
    [System.IO.File]::WriteAllText($playerFile, $updated, $encoding)
    Write-Host "Opcao 8 aplicada: barra baixa, cantos arredondados e cursor dourado translucido." -ForegroundColor Green
    Write-Host "Backup: $backup"
}
Write-Host "Compile e execute novamente na TV."
