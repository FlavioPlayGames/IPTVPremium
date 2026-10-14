$ErrorActionPreference = "Stop"
$payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("eyJwYXRjaGVzIjogW3siZmlsZSI6ICJwbGF5ZXIvUGxheWJhY2tTY3JlZW4ua3QiLCAib2xkIjogInByaXZhdGUgZnVuIHBsYXllckdvbGRGcmFtZUJydXNoKGFscGhhOiBGbG9hdCA9IDFmKTogQnJ1c2ggPSBCcnVzaC5saW5lYXJHcmFkaWVudChcbiAgICBsaXN0T2YoQ29sb3IoMHhGRkZGRjRCRCksIENvbG9yKDB4RkZCOTgxMzIpLCBDb2xvcigweEZGRkZFOTkxKSwgQ29sb3IoMHhGRjlFNzYxQiksIENvbG9yKDB4RkZGRkU5OTEpKVxuICAgICAgICAubWFwIHsgaXQuY29weShhbHBoYSA9IGFscGhhKSB9XG4pIiwgIm5ldyI6ICJwcml2YXRlIGZ1biBwbGF5ZXJHb2xkRnJhbWVCcnVzaChhbHBoYTogRmxvYXQgPSAxZik6IEJydXNoID0gQnJ1c2gubGluZWFyR3JhZGllbnQoXG4gICAgbGlzdE9mKENvbG9yKDB4RkY3QTUyMTUpLCBDb2xvcigweEZGRkZFNkEwKSwgQ29sb3IoMHhGRkQ2QTMyRSksIENvbG9yKDB4RkZGRkY1Q0QpLCBDb2xvcigweEZGOTI2MzE1KSlcbiAgICAgICAgLm1hcCB7IGl0LmNvcHkoYWxwaGEgPSBhbHBoYSkgfVxuKSJ9LCB7ImZpbGUiOiAicGxheWVyL1BsYXliYWNrU2NyZWVuLmt0IiwgIm9sZCI6ICJwcml2YXRlIGZ1biBwbGF5ZXJHb2xkQ2lyY2xlQnJ1c2goKTogQnJ1c2ggPSBCcnVzaC5zd2VlcEdyYWRpZW50KFxuICAgIGxpc3RPZihDb2xvcigweEZGRkZGNEJEKSwgQ29sb3IoMHhGRjlFNzYxQiksIENvbG9yKDB4RkZGRkU5OTEpLCBDb2xvcigweEZGOEE2NDE0KSwgQ29sb3IoMHhGRkZGRjRCRCkpXG4pIiwgIm5ldyI6ICJwcml2YXRlIGZ1biBwbGF5ZXJHb2xkQ2lyY2xlQnJ1c2goKTogQnJ1c2ggPSBCcnVzaC5zd2VlcEdyYWRpZW50KFxuICAgIGxpc3RPZihDb2xvcigweEZGN0E1MjE1KSwgQ29sb3IoMHhGRkZGRTZBMCksIENvbG9yKDB4RkZENkEzMkUpLCBDb2xvcigweEZGRkZGNUNEKSwgQ29sb3IoMHhGRjkyNjMxNSkpXG4pIn0sIHsiZmlsZSI6ICJwbGF5ZXIvUGxheWJhY2tTY3JlZW4ua3QiLCAib2xkIjogInByaXZhdGUgZnVuIHBsYXllckdvbGRJY29uQnJ1c2goY29sb3I6IENvbG9yKTogQnJ1c2ggPSBCcnVzaC52ZXJ0aWNhbEdyYWRpZW50KFxuICAgIGxpc3RPZihDb2xvcigweEZGRkZGNEJEKS5jb3B5KGFscGhhID0gY29sb3IuYWxwaGEpLCBjb2xvciwgQ29sb3IoMHhGRkM0OUEyQykuY29weShhbHBoYSA9IGNvbG9yLmFscGhhKSlcbikiLCAibmV3IjogInByaXZhdGUgZnVuIHBsYXllckdvbGRJY29uQnJ1c2goY29sb3I6IENvbG9yKTogQnJ1c2ggPSBCcnVzaC52ZXJ0aWNhbEdyYWRpZW50KFxuICAgIGxpc3RPZihDb2xvcigweEZGN0E1MjE1KSwgQ29sb3IoMHhGRkZGRTZBMCksIENvbG9yKDB4RkZENkEzMkUpLCBDb2xvcigweEZGRkZGNUNEKSwgQ29sb3IoMHhGRjkyNjMxNSkpXG4gICAgICAgIC5tYXAgeyBpdC5jb3B5KGFscGhhID0gY29sb3IuYWxwaGEpIH1cbikifSwgeyJmaWxlIjogInBsYXllci9QbGF5YmFja1NjcmVlbi5rdCIsICJvbGQiOiAibGlzdE9mKENvbG9yKDB4RkZENEFGMzcpLCBDb2xvcigweEZGRkZFOTkxKSwgQ29sb3IoMHhGRkZGRjRCRCkpIiwgIm5ldyI6ICJsaXN0T2YoQ29sb3IoMHhGRjdBNTIxNSksIENvbG9yKDB4RkZGRkU2QTApLCBDb2xvcigweEZGRDZBMzJFKSwgQ29sb3IoMHhGRkZGRjVDRCksIENvbG9yKDB4RkY5MjYzMTUpKSJ9LCB7ImZpbGUiOiAicGxheWVyL1BsYXliYWNrU2NyZWVuLmt0IiwgIm9sZCI6ICIgICAgICAgICAgICAgICAgMGYgdG8gQ29sb3IoMHhGRkZGRjRCRCkuY29weShhbHBoYSA9IDAuOThmKSxcbiAgICAgICAgICAgICAgICAwLjQyZiB0byBDb2xvcigweEZGRkZFOTkxKS5jb3B5KGFscGhhID0gMC45MGYpLFxuICAgICAgICAgICAgICAgIDAuNzZmIHRvIENvbG9yKDB4RkZFM0I4M0YpLmNvcHkoYWxwaGEgPSAwLjg0ZiksXG4gICAgICAgICAgICAgICAgMWYgdG8gQ29sb3IoMHhGRkI3OEEyNSkuY29weShhbHBoYSA9IDAuNzBmKSwiLCAibmV3IjogIiAgICAgICAgICAgICAgICAwZiB0byBDb2xvcigweEZGRkZGNUNEKS5jb3B5KGFscGhhID0gMC45OGYpLFxuICAgICAgICAgICAgICAgIDAuNDJmIHRvIENvbG9yKDB4RkZGRkU2QTApLmNvcHkoYWxwaGEgPSAwLjkwZiksXG4gICAgICAgICAgICAgICAgMC43NmYgdG8gQ29sb3IoMHhGRkQ2QTMyRSkuY29weShhbHBoYSA9IDAuODRmKSxcbiAgICAgICAgICAgICAgICAxZiB0byBDb2xvcigweEZGOTI2MzE1KS5jb3B5KGFscGhhID0gMC43MGYpLCJ9XSwgImhlbHBlcnMiOiBbXX0=")) | ConvertFrom-Json
$base = Join-Path (Get-Location) "app\src\main\java\br\com\flaviotorres\iptvpremium\core\navigation"
if (-not (Test-Path -LiteralPath $base)) {
    $base = Join-Path (Get-Location) "src\main\java\br\com\flaviotorres\iptvpremium\core\navigation"
}
if (-not (Test-Path -LiteralPath $base)) { throw "Abra o terminal na raiz do projeto ou na pasta app." }
$marker = "// IPTV_PLAYER_RESUME_GOLD_V1"
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
    if ($count -ne 1) { throw "Versao diferente em $($change.file). Nada foi alterado. Envie os arquivos Kotlin atuais para adaptar o patch." }
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
    if (-not $formats[$path].Helper) { $text += "`n" + $marker + "`n" }
    if ($formats[$path].CRLF) { $text = $text.Replace("`n", "`r`n") }
    $encoding = New-Object Text.UTF8Encoding([bool]$formats[$path].Bom)
    [IO.File]::WriteAllText($path + ".iptv-tmp", $text, $encoding)
    Move-Item -LiteralPath ($path + ".iptv-tmp") -Destination $path -Force
}
Write-Host "Concluido: dourado da tela de retomada aplicado ao menu do player. Compile e execute o app." -ForegroundColor Green
Write-Host "Backups dos arquivos alterados: .backup-$stamp"
