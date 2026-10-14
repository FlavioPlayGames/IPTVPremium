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
$helperPattern = '(?ms)^@Composable\r?\nprivate fun PlayerCinemaPanel\(.*?^\}'
$progressPattern = '(?ms)^@Composable\r?\nprivate fun PlayerProgressBar\(.*?^\}'
if ([regex]::Matches($text, $helperPattern).Count -ne 1 -or [regex]::Matches($text, $progressPattern).Count -ne 1) {
    throw "O menu esperado nao foi encontrado. Nenhuma alteracao foi feita."
}
$newHelper = @'
@Composable
private fun PlayerCinemaPanel(
    compact: Boolean,
    gold: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 5.dp else 6.dp)
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
    val verticalPaddingPx = with(density) { 7.dp.roundToPx() }
    val originalGapPx = with(density) { (if (compact) 2.dp else 3.dp).roundToPx() }

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
                val accentWidth = (size.width * 0.16f).coerceAtMost(125.dp.toPx())
                val margin = (if (compact) 14.dp else 18.dp).toPx()
                val top = size.height - 3.5.dp.toPx()
                val bottom = size.height - 1.dp.toPx()
                val stroke = 0.45.dp.toPx()
                for (start in listOf(margin, size.width - margin - accentWidth)) {
                    drawLine(gold.copy(alpha = 0.54f), Offset(start, top), Offset(start + accentWidth, top), stroke)
                    drawLine(gold.copy(alpha = 0.76f), Offset(start, bottom), Offset(start + accentWidth, bottom), stroke)
                    val pitch = 7.dp.toPx()
                    var x = start + 2.dp.toPx()
                    while (x + 3.dp.toPx() < start + accentWidth) {
                        drawRoundRect(
                            color = gold.copy(alpha = 0.68f),
                            topLeft = Offset(x, top + 0.45.dp.toPx()),
                            size = Size(3.dp.toPx(), 1.6.dp.toPx()),
                            cornerRadius = CornerRadius(0.4.dp.toPx()),
                            style = Stroke(width = 0.4.dp.toPx())
                        )
                        x += pitch
                    }
                }
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, gold.copy(alpha = 0.56f), Color.Transparent)
                    ),
                    start = Offset(size.width * 0.32f, size.height - 1.dp.toPx()),
                    end = Offset(size.width * 0.68f, size.height - 1.dp.toPx()),
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
        val originalOuterHeight = originalHeight + verticalPaddingPx * 2
        val extraHeight = kotlin.math.round(originalOuterHeight * 0.10f).toInt()
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
                    // Todo o espaço extra fica entre a timeline e os botões.
                    if (index == 1) y += extraHeight
                }
            }
        }
    }
}
'@
$newProgress = @'
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
            .height(32.dp)
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
                0f to Color(0xFFFFF8E2),
                0.42f to Color(0xFFFFE7AA),
                0.76f to Color(0xFFECC15F),
                1f to Color(0xFFAD7629),
                center = highlight,
                radius = thumbRadius * 1.65f
            ),
            radius = thumbRadius,
            center = thumbCenter
        )
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(Color(0xFFFFF0BA), Color(0xFFAD7629), Color(0xFFFFF0BA))
            ),
            radius = thumbRadius,
            center = thumbCenter,
            style = Stroke(width = 0.6.dp.toPx())
        )
    }
}
'@
$updated = $text
$newHelper = Normalize-NewLine $newHelper
$newProgress = Normalize-NewLine $newProgress
$match = [regex]::Match($updated, $helperPattern)
$updated = $updated.Substring(0, $match.Index) + $newHelper + $updated.Substring($match.Index + $match.Length)
$match = [regex]::Match($updated, $progressPattern)
$updated = $updated.Substring(0, $match.Index) + $newProgress + $updated.Substring($match.Index + $match.Length)
if (-not [regex]::IsMatch($updated, '(?m)^import androidx\.compose\.ui\.graphics\.TransformOrigin\r?$')) {
    $importPattern = '(?m)^import androidx\.compose\.ui\.graphics\.ImageBitmap\r?$'
    if ([regex]::Matches($updated, $importPattern).Count -ne 1) { throw "Import esperado nao encontrado. Nenhuma alteracao foi feita." }
    $updated = [regex]::Replace($updated, $importPattern, "import androidx.compose.ui.graphics.TransformOrigin$nl" + "import androidx.compose.ui.graphics.ImageBitmap")
}
if ($updated -ceq $text) {
    Write-Host "O menu compacto ja esta aplicado." -ForegroundColor Yellow
} else {
    $backup = "$playerFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmssfff')"
    [System.IO.File]::WriteAllBytes($backup, $bytes)
    [System.IO.File]::WriteAllText($playerFile, $updated, $encoding)
    Write-Host "Barra e controles reduzidos pela metade; cursor dourado metalico aplicado." -ForegroundColor Green
    Write-Host "Backup: $backup"
}
Write-Host "Compile e execute novamente na TV."
