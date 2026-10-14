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
$updated = $text
$updated = $updated.Replace('0xFFFFDF35', '0xFFD4AF37')
$updated = $updated.Replace('0xFFFFF8CF', '0xFFFFF4BD')
$updated = $updated.Replace('0xFFFFD339', '0xFFB98132')
$updated = $updated.Replace('0xFFFFED83', '0xFFFFE991')
$updated = $updated.Replace('0xFFE9B628', '0xFF9E761B')
$updated = $updated.Replace('0xFFFFF2AC', '0xFFFFE991')
$updated = $updated.Replace('0xFFFFE566', '0xFFE3B83F')
$updated = $updated.Replace('0xFFE8BA25', '0xFFBE8B20')
$updated = $updated.Replace('0xFFB88214', '0xFF81540F')
$updated = $updated.Replace('0xFFFFF7CB', '0xFFFFF4BD')
$updated = $updated.Replace('0xFFFFD23A', '0xFF9E761B')
$updated = $updated.Replace('0xFFFFED79', '0xFFFFE991')
$updated = $updated.Replace('0xFFE2B427', '0xFF8A6414')
$updated = $updated.Replace('0xBEFFF2AA', '0xBEFFF4BD')
$updated = $updated.Replace('0xFFFFF8CD', '0xFFFFF4BD')
$updated = $updated.Replace('0xFFFFE96D', '0xFFFFE991')
$updated = $updated.Replace('0xFFFFD535', '0xFFD4AF37')
$updated = $updated.Replace('0xFFFFE76C', '0xFFFFE991')
$updated = $updated.Replace('0xFFFFF3BD', '0xFFFFF4BD')
$updated = $updated.Replace('0xFFFFF9D6', '0xFFFFF4BD')
$updated = $updated.Replace('0xFFFFEE73', '0xFFFFE991')
$updated = $updated.Replace('0xFFFFD83E', '0xFFE3B83F')
$updated = $updated.Replace('0xFFFFC52F', '0xFFB78A25')
$updated = $updated.Replace('0xFFFFF6BD', '0xFFFFF4BD')
$newGlyph = @'
@Composable
private fun PlayerGlyphDrawing(
    glyph: PlayerGlyph,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val iconBrush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFF4BD).copy(alpha = color.alpha),
                color,
                Color(0xFFC49A2C).copy(alpha = color.alpha)
            )
        )
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
$pattern = '(?ms)^@Composable\r?\nprivate fun PlayerGlyphDrawing\(.*?^\}'
if ([regex]::Matches($updated, $pattern).Count -ne 1) { throw "Desenho dos icones diferente do esperado. Nenhum arquivo foi alterado." }
$replacement = $newGlyph.Replace("`r`n", "`n").Replace("`n", $nl)
$match = [regex]::Match($updated, $pattern)
$updated = $updated.Substring(0, $match.Index) + $replacement + $updated.Substring($match.Index + $match.Length)
if ($updated -ceq $text) {
    Write-Host "As cores ja estao aplicadas." -ForegroundColor Yellow
} else {
    $backup = "$playerFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmssfff')"
    [System.IO.File]::WriteAllBytes($backup, $bytes)
    [System.IO.File]::WriteAllText($playerFile, $updated, $encoding)
    Write-Host "Cores e acabamento metalico da referencia aplicados. Formato e tamanhos preservados." -ForegroundColor Green
    Write-Host "Backup: $backup"
}
Write-Host "Compile e execute novamente na TV."
