param([string]$SitePath = (Join-Path $env:USERPROFILE "Downloads\Central-IPTV-Premium-site\site"))
$ErrorActionPreference = "Stop"

$site = (Resolve-Path -LiteralPath $SitePath).Path
$publisher = Join-Path $site "Publicar-Painel.ps1"
$workerPath = Join-Path $site "src\worker.js"
if (-not (Test-Path -LiteralPath $publisher)) { throw "Informe a pasta original do site publicado usando -SitePath." }
$worker = [IO.File]::ReadAllText($workerPath)
$oldExpiry = "const token=random(32),expires=Date.now()+5*60000;"
$newExpiry = "const token=random(32),expires=Date.now()+15*60000;"
if (-not $worker.Contains($oldExpiry) -and -not $worker.Contains($newExpiry)) { throw "Versao de servidor diferente. Nada foi alterado." }

$payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("eyJwYXRjaGVzIjogW3siZmlsZSI6ICJhcHAvc3JjL21haW4vamF2YS9ici9jb20vZmxhdmlvdG9ycmVzL2lwdHZwcmVtaXVtL2NvcmUvZGV2aWNlL0RldmljZVBvcnRhbFFyLmt0IiwgIm9sZCI6ICJ2XHUwMGUxbGlkbyBwb3IgNSBtaW51dG9zLiIsICJuZXciOiAidlx1MDBlMWxpZG8gcG9yIDE1IG1pbnV0b3MuIiwgImNvdW50IjogMX0sIHsiZmlsZSI6ICJhcHAvc3JjL21haW4vamF2YS9ici9jb20vZmxhdmlvdG9ycmVzL2lwdHZwcmVtaXVtL2NvcmUvZGV2aWNlL0RldmljZVBvcnRhbFFyLmt0IiwgIm9sZCI6ICIzMDBfMDAwTCIsICJuZXciOiAiOTAwXzAwMEwiLCAiY291bnQiOiAxfV0sICJoZWxwZXJzIjogW119")) | ConvertFrom-Json
$base = (Get-Location).Path
if (-not (Test-Path -LiteralPath (Join-Path $base "app\src\main"))) {
    if ((Test-Path -LiteralPath (Join-Path $base "src\main")) -and (Test-Path -LiteralPath (Join-Path $base "..\settings.gradle.kts"))) {
        $base = Split-Path $base -Parent
    } else { throw "Abra o terminal na raiz do projeto Android." }
}
$marker = "IPTV_QR_15_MINUTES_V1"
$staged = @{}
$formats = @{}
$utf8 = New-Object Text.UTF8Encoding($false, $true)
foreach ($change in $payload.patches) {
    $path = Join-Path $base ($change.file.Replace('/', '\'))
    if (-not $staged.ContainsKey($path)) {
        if (-not (Test-Path -LiteralPath $path)) { throw "Arquivo ausente: $($change.file)" }
        $bytes = [IO.File]::ReadAllBytes($path)
        $bom = $bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191
        $text = $utf8.GetString($bytes).TrimStart([char]0xFEFF)
        $formats[$path] = @{ Bom = $bom; CRLF = $text.Contains("`r`n"); Applied = $text.Contains($marker) }
        $staged[$path] = $text.Replace("`r`n", "`n")
    }
    if ($formats[$path].Applied) { continue }
    $old = $change.old.Replace("`r`n", "`n")
    $new = $change.new.Replace("`r`n", "`n")
    $count = [regex]::Matches($staged[$path], [regex]::Escape($old)).Count
    $expectedCount = if ($change.count) { [int]$change.count } else { 1 }
    if ($count -ne $expectedCount) { throw "Versao diferente em $($change.file). Nada foi alterado. Envie os arquivos Kotlin atuais para adaptar o patch." }
    $staged[$path] = $staged[$path].Replace($old, $new)
}
foreach ($helper in $payload.helpers) {
    $path = Join-Path $base ($helper.file.Replace('/', '\'))
    $text = $helper.text.Replace("`r`n", "`n")
    if (Test-Path -LiteralPath $path) {
        $existing = $utf8.GetString([IO.File]::ReadAllBytes($path)).TrimStart([char]0xFEFF).Replace("`r`n", "`n")
        if ($existing -ne $text) { throw "Arquivo auxiliar ja existe e foi modificado: $($helper.file). Nada foi alterado." }
        continue
    }
    $staged[$path] = $text
    $formats[$path] = @{ Bom = $false; CRLF = $true; Applied = $false; Helper = $true }
}
# Validate all files first; preserve original bytes in backups before any source write.
$stamp = Get-Date -Format "yyyyMMdd-HHmmssfff"
foreach ($path in $staged.Keys) {
    if ($formats[$path].Applied) { continue }
    if (Test-Path -LiteralPath $path) { Copy-Item -LiteralPath $path -Destination ($path + ".backup-" + $stamp) }
}
foreach ($path in $staged.Keys) {
    if ($formats[$path].Applied) { continue }
    $text = $staged[$path]
    if (-not $formats[$path].Helper) {
        $fileComment = if ([IO.Path]::GetExtension($path) -eq ".xml") { "<!-- " + $marker + " -->" } else { "// " + $marker }
        $text += "`n" + $fileComment + "`n"
    }
    if ($formats[$path].CRLF) { $text = $text.Replace("`n", "`r`n") }
    $encoding = New-Object Text.UTF8Encoding([bool]$formats[$path].Bom)
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))
    [IO.File]::WriteAllText($path + ".iptv-tmp", $text, $encoding)
    Move-Item -LiteralPath ($path + ".iptv-tmp") -Destination $path -Force
}
Write-Host "Aplicativo preparado: QR de listas e legendas com validade de 15 minutos." -ForegroundColor Green
Write-Host "Backups dos arquivos alterados: .backup-$stamp"


if ($worker.Contains($oldExpiry)) {
    Copy-Item -LiteralPath $workerPath -Destination ($workerPath + ".backup-" + $stamp)
    [IO.File]::WriteAllText($workerPath, $worker.Replace($oldExpiry, $newExpiry), (New-Object Text.UTF8Encoding($false)))
}
Write-Host "Publicando a validade de 15 minutos no servidor..." -ForegroundColor Cyan
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $publisher -ConectarDominio
if ($LASTEXITCODE -ne 0) { throw "Servidor ainda nao atualizado. Execute este script novamente para concluir. Nao instale o APK antes de concluir a publicacao." }
Write-Host "Concluido: compile e atualize o APK sem desinstalar. Gere um novo QR para usar os 15 minutos." -ForegroundColor Green
