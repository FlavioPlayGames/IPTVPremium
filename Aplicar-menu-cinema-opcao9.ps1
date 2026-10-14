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
$openingPattern = '(?s)val glassShape = RoundedCornerShape\([^\r\n]*\)\s+Column\(\s+verticalArrangement = Arrangement\.spacedBy.*?\r?\n[ \t]*\)\s*\{\s*(?=Row\(verticalAlignment = Alignment.CenterVertically\))'
$newOpening = @'
PlayerCinemaPanel(
                    compact = isCompactScreen,
                    gold = gold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isCompactScreen) 8.dp else 16.dp,
                            vertical = if (isCompactScreen) 9.dp else 18.dp
                        )
                ) {
'@
$newOpening = (Normalize-NewLine $newOpening) + $nl + "                    "
$openingCount = [regex]::Matches($text, $openingPattern).Count
$updated = $text
if ($openingCount -eq 1) {
    $match = [regex]::Match($updated, $openingPattern)
    $updated = $updated.Substring(0, $match.Index) + $newOpening + $updated.Substring($match.Index + $match.Length)
} elseif ($openingCount -ne 0 -or -not $updated.Contains("private fun PlayerCinemaPanel(")) {
    throw "A barra deste arquivo tem uma estrutura diferente. Nenhum arquivo foi alterado."
}
$newHelper = @'
@Composable
private fun PlayerCinemaPanel(
    compact: Boolean,
    gold: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    val goldRim = Brush.linearGradient(
        listOf(
            Color(0xFFFFF0B7),
            Color(0xFFB98132),
            Color(0xFFFFD36D),
            Color(0xFF956024),
            Color(0xFFFFE69E)
        )
    )
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
                val top = size.height - 7.dp.toPx()
                val bottom = size.height - 2.dp.toPx()
                val stroke = 0.7.dp.toPx()
                for (start in listOf(margin, size.width - margin - accentWidth)) {
                    drawLine(gold.copy(alpha = 0.54f), Offset(start, top), Offset(start + accentWidth, top), stroke)
                    drawLine(gold.copy(alpha = 0.76f), Offset(start, bottom), Offset(start + accentWidth, bottom), stroke)
                    val pitch = 7.dp.toPx()
                    var x = start + 2.dp.toPx()
                    while (x + 3.dp.toPx() < start + accentWidth) {
                        drawRoundRect(
                            color = gold.copy(alpha = 0.68f),
                            topLeft = Offset(x, top + 0.9.dp.toPx()),
                            size = Size(3.dp.toPx(), 3.2.dp.toPx()),
                            cornerRadius = CornerRadius(0.4.dp.toPx()),
                            style = Stroke(width = 0.6.dp.toPx())
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
            .border(1.1.dp, goldRim, shape)
            .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 7.dp)
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minHeight = 0)
        val children = measurables.map { it.measure(childConstraints) }
        val originalHeight = children.sumOf { it.height } +
            originalGapPx * (children.size - 1).coerceAtLeast(0)
        // A altura externa cresce 10%, incluindo o padding original.
        val originalOuterHeight = originalHeight + verticalPaddingPx * 2
        val extraHeight = kotlin.math.round(originalOuterHeight * 0.10f).toInt()
        val width = (children.maxOfOrNull { it.width } ?: constraints.minWidth)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (originalHeight + extraHeight)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var y = 0
            children.forEachIndexed { index, child ->
                child.placeRelative(0, y)
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
$newButton = @'
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
        prominent && compact -> 47.dp
        prominent -> 52.dp
        compact -> 39.dp
        else -> 45.dp
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
            modifier = Modifier.size(if (compact) 17.1.dp else 19.8.dp)
        )
    }
}
'@
$newHelper = Normalize-NewLine $newHelper
$newButton = Normalize-NewLine $newButton
$buttonPattern = '(?ms)^@Composable\r?\nprivate fun PlayerIconButton\(.*?^\}'
if ([regex]::Matches($updated, $buttonPattern).Count -ne 1) { throw "PlayerIconButton nao encontrado de forma unica. Nenhum arquivo foi alterado." }
$match = [regex]::Match($updated, $buttonPattern)
$updated = $updated.Substring(0, $match.Index) + $newButton + $updated.Substring($match.Index + $match.Length)
$helperPattern = '(?ms)^@Composable\r?\nprivate fun PlayerCinemaPanel\(.*?^\}'
$helperCount = [regex]::Matches($updated, $helperPattern).Count
if ($helperCount -eq 1) {
    $match = [regex]::Match($updated, $helperPattern)
    $updated = $updated.Substring(0, $match.Index) + $newHelper + $updated.Substring($match.Index + $match.Length)
} elseif ($helperCount -eq 0) {
    $anchor = '@Composable' + $nl + 'private fun PlayerAuxiliaryButtons('
    if (-not $updated.Contains($anchor)) { throw "Ponto de insercao nao encontrado. Nenhum arquivo foi alterado." }
    $updated = $updated.Replace($anchor, $newHelper + $nl + $nl + $anchor)
} else { throw "Mais de um painel encontrado. Nenhum arquivo foi alterado." }
if (-not $updated.Contains("import androidx.compose.ui.layout.Layout")) {
    $importPattern = '(?m)^import androidx\.compose\.ui\.layout\.ContentScale\r?$'
    if ([regex]::Matches($updated, $importPattern).Count -ne 1) { throw "Import esperado nao encontrado. Nenhum arquivo foi alterado." }
    $updated = [regex]::Replace($updated, $importPattern, "import androidx.compose.ui.layout.Layout$nl" + "import androidx.compose.ui.layout.ContentScale")
}
if ($updated -ceq $text) {
    Write-Host "O menu ja esta atualizado." -ForegroundColor Yellow
} else {
    $backup = "$playerFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmssfff')"
    [System.IO.File]::WriteAllBytes($backup, $bytes)
    [System.IO.File]::WriteAllText($playerFile, $updated, $encoding)
    Write-Host "Opcao 9 aplicada: altura +10%, cantos suaves e espaco entre timeline e botoes." -ForegroundColor Green
    Write-Host "Backup: $backup"
}
Write-Host "Compile e execute novamente o aplicativo no Android Studio."
