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
$updated = $text
$updated = $updated.Replace('Color(0xFFFFD54A)', 'Color(0xFFFFDF35)')
$updated = $updated.Replace('Color(0xFFFFF0B7)', 'Color(0xFFFFF8CF)')
$updated = $updated.Replace('Color(0xFFB98132)', 'Color(0xFFFFD339)')
$updated = $updated.Replace('Color(0xFFFFD36D)', 'Color(0xFFFFED83)')
$updated = $updated.Replace('Color(0xFF956024)', 'Color(0xFFE9B628)')
$updated = $updated.Replace('Color(0xFFFFE69E)', 'Color(0xFFFFF2AC)')
$updated = $updated.Replace('Color(0xFFD5A620)', 'Color(0xFFFFE566)')
$updated = $updated.Replace('Color(0xFFAE780B)', 'Color(0xFFE8BA25)')
$updated = $updated.Replace('Color(0xFF603C05)', 'Color(0xFFB88214)')
$updated = $updated.Replace('Color(0xFFFFEDB0)', 'Color(0xFFFFF7CB)')
$updated = $updated.Replace('Color(0xFFAC7426)', 'Color(0xFFFFD23A)')
$updated = $updated.Replace('Color(0xFFFFD369)', 'Color(0xFFFFED79)')
$updated = $updated.Replace('Color(0xFF8C591C)', 'Color(0xFFE2B427)')
$updated = $updated.Replace('Color(0x70FFEB9D)', 'Color(0xBEFFF2AA)')
$updated = $updated.Replace('Color(0xFFFFF0B9)', 'Color(0xFFFFF8CD)')
$updated = $updated.Replace('Color(0xFFFFD579)', 'Color(0xFFFFE96D)')
$updated = $updated.Replace('Color(0xFFE5AA3C)', 'Color(0xFFFFD535)')
$updated = $updated.Replace('Color(0xFFFFD779)', 'Color(0xFFFFE76C)')
$updated = $updated.Replace('Color(0xFFFFE9B4)', 'Color(0xFFFFF3BD)')
$updated = $updated.Replace('Color(0xFFFFF8E2).copy(alpha = 0.92f)', 'Color(0xFFFFF9D6).copy(alpha = 0.98f)')
$updated = $updated.Replace('Color(0xFFFFE7AA).copy(alpha = 0.80f)', 'Color(0xFFFFEE73).copy(alpha = 0.90f)')
$updated = $updated.Replace('Color(0xFFECC15F).copy(alpha = 0.68f)', 'Color(0xFFFFD83E).copy(alpha = 0.84f)')
$updated = $updated.Replace('Color(0xFFAD7629).copy(alpha = 0.48f)', 'Color(0xFFFFC52F).copy(alpha = 0.70f)')
$updated = $updated.Replace('Color(0xFFFFF0BA).copy(alpha = 0.72f)', 'Color(0xFFFFF6BD).copy(alpha = 0.92f)')
$updated = $updated.Replace('color = gold.copy(alpha = 0.78f)', 'color = gold.copy(alpha = 0.98f)')
$updated = $updated.Replace('.border(0.8.dp, goldRim, shape)', '.border(1.1.dp, goldRim, shape)')
$updated = $updated.Replace('gold.copy(alpha = 0.56f)', 'gold.copy(alpha = 0.76f)')
if ($updated -ceq $text) {
    Write-Host "Nenhuma cor antiga encontrada; o ajuste pode ja estar aplicado." -ForegroundColor Yellow
} else {
    $backup = "$playerFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmssfff')"
    [System.IO.File]::WriteAllBytes($backup, $bytes)
    [System.IO.File]::WriteAllText($playerFile, $updated, $encoding)
    Write-Host "Dourado brilhante aplicado aos icones, bordas e cursor do player." -ForegroundColor Green
    Write-Host "Backup: $backup"
}
Write-Host "Compile e execute novamente na TV."
