$ErrorActionPreference = "Stop"
$relativePath = "app\src\main\java\br\com\flaviotorres\iptvpremium\ui\home\HomeScreen.kt"
$homeFile = @($relativePath, "..\$relativePath") | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $homeFile) { throw "Abra o terminal na pasta principal do projeto." }
$homeFile = (Resolve-Path -LiteralPath $homeFile).Path
$projectRoot = $homeFile.Substring(0, $homeFile.Length - $relativePath.Length)
$resourceDir = Join-Path $projectRoot "app\src\main\res\drawable"
$bytes = [System.IO.File]::ReadAllBytes($homeFile)
$hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
$encoding = [System.Text.UTF8Encoding]::new($hasBom, $true)
$text = $encoding.GetString($bytes)
if ($hasBom) { $text = $text.Substring(1) }
$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$functionPattern = '(?ms)^@Composable\r?\nprivate fun HomeTopIconButton\(.*?^\}'
if ([regex]::Matches($text, $functionPattern).Count -ne 1) { throw "HomeTopIconButton nao encontrado de forma unica. Nenhum arquivo foi alterado." }
$newFunction = @'
@Composable
private fun HomeTopIconButton(
    imageResId: Int,
    contentDescription: String,
    isWideScreen: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val touchAreaSize =
        if (isWideScreen) {
            46.dp
        } else {
            40.dp
        }

    val iconSize =
        if (isWideScreen) {
            39.dp
        } else {
            33.8.dp
        }

    // Fundo transparente; foco indicado pelo aumento suave do ícone.
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(touchAreaSize)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        HomeScaledResourceImage(
            imageResId = imageResId,
            contentDescription = contentDescription,
            targetWidth = iconSize * 1.12f,
            targetHeight = iconSize * 1.12f,
            modifier = Modifier
                .size(iconSize)
                .scale(if (isFocused) 1.12f else 1f),
            contentScale = ContentScale.Fit
        )
    }
}
'@
$newFunction = $newFunction.Replace("`r`n", "`n").Replace("`n", $nl)
$match = [regex]::Match($text, $functionPattern)
$updated = $text.Substring(0, $match.Index) + $newFunction + $text.Substring($match.Index + $match.Length)
$decodePattern = '(?s)var sample = 1\s+while\s*\(.*?\)\s*\{\s*sample\s*\*=\s*2\s*\}\s+val options\s*=\s*BitmapFactory\.Options\(\)\.apply\s*\{.*?\}\s+val image\s*=\s*BitmapFactory\.decodeResource\(\s*resources,\s*imageResId,\s*options\s*\)\s*\?\.asImageBitmap\(\)'
$newDecode = @'
// Decodifica sem redução para não perder as bordas antes da filtragem.
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        inScaled = false
                        inPremultiplied = true
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val original = BitmapFactory.decodeResource(
                        resources,
                        imageResId,
                        decodeOptions
                    ) ?: return@withPermit null

                    val factor = minOf(
                        requiredWidth.toFloat() / original.width,
                        requiredHeight.toFloat() / original.height,
                        1f
                    )
                    val targetWidth = (original.width * factor).toInt().coerceAtLeast(1)
                    val targetHeight = (original.height * factor).toInt().coerceAtLeast(1)
                    var prepared = original

                    // Reduz em etapas filtradas, preservando a transparência.
                    while (
                        prepared.width / 2 >= targetWidth &&
                        prepared.height / 2 >= targetHeight
                    ) {
                        val next = Bitmap.createScaledBitmap(
                            prepared,
                            prepared.width / 2,
                            prepared.height / 2,
                            true
                        )
                        if (next !== prepared) prepared.recycle()
                        prepared = next
                    }
                    if (prepared.width != targetWidth || prepared.height != targetHeight) {
                        val next = Bitmap.createScaledBitmap(
                            prepared,
                            targetWidth,
                            targetHeight,
                            true
                        )
                        if (next !== prepared) prepared.recycle()
                        prepared = next
                    }
                    val image = prepared.asImageBitmap()
'@
$newDecode = $newDecode.Replace("`r`n", "`n").Replace("`n", $nl)
$decodeMatches = [regex]::Matches($updated, $decodePattern).Count
if ($decodeMatches -eq 1) {
    $match = [regex]::Match($updated, $decodePattern)
    $updated = $updated.Substring(0, $match.Index) + $newDecode + $updated.Substring($match.Index + $match.Length)
} elseif (-not $updated.Contains("val image = prepared.asImageBitmap()")) {
    throw "Carregamento de imagens diferente do esperado. Nenhum arquivo foi alterado."
}
if (-not $updated.Contains("import androidx.compose.ui.graphics.FilterQuality")) {
    $importPattern = '(?m)^import androidx\.compose\.ui\.graphics\.ImageBitmap\r?$'
    if ([regex]::Matches($updated, $importPattern).Count -ne 1) { throw "Import de ImageBitmap nao encontrado. Nenhum arquivo foi alterado." }
    $updated = [regex]::Replace($updated, $importPattern, "import androidx.compose.ui.graphics.ImageBitmap$nl" + "import androidx.compose.ui.graphics.FilterQuality")
}
$imageScale = '(?m)^(?<indent>[ \t]*)contentScale = contentScale(?<comma>,?)[ \t]*\r?$'
$filterPattern = 'filterQuality\s*=\s*FilterQuality\.\w+'
$filterCount = [regex]::Matches($updated, $filterPattern).Count
if ($filterCount -gt 1) { throw "Mais de um filtro encontrado. Nenhum arquivo foi alterado." }
if ($filterCount -eq 1) {
    $updated = [regex]::Replace($updated, $filterPattern, 'filterQuality = FilterQuality.High')
} else {
    $scaleMatch = [regex]::Match($updated, $imageScale)
    if (-not $scaleMatch.Success) { throw "Exibicao das imagens diferente do esperado. Nenhum arquivo foi alterado." }
    $indent = $scaleMatch.Groups['indent'].Value
    $line = $indent + "contentScale = contentScale," + $nl + $indent + "filterQuality = FilterQuality.High"
    $updated = $updated.Substring(0, $scaleMatch.Index) + $line + $updated.Substring($scaleMatch.Index + $scaleMatch.Length)
}
$asset = Join-Path $PSScriptRoot "icon_atualizar.png"
if (-not (Test-Path -LiteralPath $asset)) { throw "Recurso ausente: icon_atualizar.png" }
if ((Get-FileHash -LiteralPath $asset -Algorithm SHA256).Hash -ne "51cc723aa6afad0cbfc5a368fd651accafd8ceb7b722d12902c9ba9f0f05fc4f") { throw "Recurso diferente do esperado: icon_atualizar.png" }
$asset = Join-Path $PSScriptRoot "icon_configuracoes.png"
if (-not (Test-Path -LiteralPath $asset)) { throw "Recurso ausente: icon_configuracoes.png" }
if ((Get-FileHash -LiteralPath $asset -Algorithm SHA256).Hash -ne "2cbab58d9f41fa3cdba067fa8910812a21f3130c735e59f086db057c51e27ae8") { throw "Recurso diferente do esperado: icon_configuracoes.png" }
$backupDir = Join-Path $projectRoot ("backup-icones-superiores-" + (Get-Date -Format "yyyyMMdd-HHmmssfff"))
New-Item -ItemType Directory -Path $backupDir | Out-Null
[System.IO.File]::WriteAllBytes((Join-Path $backupDir "HomeScreen.kt"), $bytes)
foreach ($name in @("icon_atualizar.png", "icon_configuracoes.png")) {
    $destination = Join-Path $resourceDir $name
    if (Test-Path -LiteralPath $destination) { Copy-Item -LiteralPath $destination -Destination (Join-Path $backupDir $name) }
}
try {
    foreach ($name in @("icon_atualizar.png", "icon_configuracoes.png")) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $resourceDir $name) -Force
    }
    [System.IO.File]::WriteAllText($homeFile, $updated, $encoding)
} catch {
    [System.IO.File]::WriteAllBytes($homeFile, $bytes)
    foreach ($name in @("icon_atualizar.png", "icon_configuracoes.png")) {
        $backupAsset = Join-Path $backupDir $name
        if (Test-Path -LiteralPath $backupAsset) { Copy-Item -LiteralPath $backupAsset -Destination (Join-Path $resourceDir $name) -Force }
    }
    throw
}
Write-Host "Opcao 1 aplicada. Icones 30% maiores, sem moldura quadrada." -ForegroundColor Green
Write-Host "Backup: $backupDir"
Write-Host "Compile e execute novamente no Android Studio."
