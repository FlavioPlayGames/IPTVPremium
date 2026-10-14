$ErrorActionPreference = "Stop"
$payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("eyJwYXRjaGVzIjogW3siZmlsZSI6ICJwbGF5ZXIvU3VidGl0bGVBcHBlYXJhbmNlLmt0IiwgIm9sZCI6ICJpbnRlcm5hbCB2YWwgc3VidGl0bGVUZXh0Q29sb3JzID0gbGlzdE9mKFxuICAgIFN1YnRpdGxlVGV4dENvbG9yKFwiQnJhbmNvXCIsIDB4RkZGRkZGRkYudG9JbnQoKSksXG4gICAgU3VidGl0bGVUZXh0Q29sb3IoXCJBbWFyZWxvIHN1YXZlXCIsIDB4RkZGRkU5OTEudG9JbnQoKSksXG4gICAgU3VidGl0bGVUZXh0Q29sb3IoXCJDaW56YSBjbGFyb1wiLCAweEZGRTVFNUU1LnRvSW50KCkpLFxuICAgIFN1YnRpdGxlVGV4dENvbG9yKFwiQ3JlbWVcIiwgMHhGRkZGRjRCRC50b0ludCgpKVxuKSIsICJuZXciOiAiaW50ZXJuYWwgdmFsIHN1YnRpdGxlVGV4dENvbG9ycyA9IGxpc3RPZihcbiAgICBTdWJ0aXRsZVRleHRDb2xvcihcIkJyYW5jbyBmb3J0ZVwiLCAweEZGRkZGRkZGLnRvSW50KCkpLFxuICAgIFN1YnRpdGxlVGV4dENvbG9yKFwiQnJhbmNvIG3DqWRpb1wiLCAweEZGRjJGMkYyLnRvSW50KCkpLFxuICAgIFN1YnRpdGxlVGV4dENvbG9yKFwiQnJhbmNvIHN1YXZlXCIsIDB4RkZFNUU1RTUudG9JbnQoKSksXG4gICAgU3VidGl0bGVUZXh0Q29sb3IoXCJBbWFyZWxvIGZvcnRlXCIsIDB4RkZGRkZGMDAudG9JbnQoKSksXG4gICAgU3VidGl0bGVUZXh0Q29sb3IoXCJBbWFyZWxvIG3DqWRpb1wiLCAweEZGRkZENTRGLnRvSW50KCkpLFxuICAgIFN1YnRpdGxlVGV4dENvbG9yKFwiQW1hcmVsbyBzdWF2ZVwiLCAweEZGRkZFOTkxLnRvSW50KCkpXG4pIn0sIHsiZmlsZSI6ICJwbGF5ZXIvUGxheWJhY2tTY3JlZW4ua3QiLCAib2xkIjogIiAgICAgICAgICAgICAgICAgICAgICAgIGRlc2NyaXB0aW9uLFxuICAgICAgICAgICAgICAgICAgICAgICAgY29sb3IgPSBDb2xvci5XaGl0ZS5jb3B5KGFscGhhID0gMC42OGYpLFxuICAgICAgICAgICAgICAgICAgICAgICAgZm9udFNpemUgPSAxMS5zcCwiLCAibmV3IjogIiAgICAgICAgICAgICAgICAgICAgICAgIGRlc2NyaXB0aW9uLFxuICAgICAgICAgICAgICAgICAgICAgICAgY29sb3IgPSBDb2xvcigweEZGRjJGMkYyKSxcbiAgICAgICAgICAgICAgICAgICAgICAgIGZvbnRTaXplID0gMTIuc3AsIn0sIHsiZmlsZSI6ICJwbGF5ZXIvU3VidGl0bGVBcHBlYXJhbmNlUmVuZGVyaW5nLmt0IiwgIm9sZCI6ICIgICAgICAgIHZhbCBjb2xvciA9IHByZWZlcmVuY2VzLmdldEludChcImNvbG9yXCIsIC0xKS50YWtlSWYgeyBhcmdiIC0+IHN1YnRpdGxlVGV4dENvbG9ycy5hbnkgeyBpdC5hcmdiID09IGFyZ2IgfSB9ID86IC0xIiwgIm5ldyI6ICIgICAgICAgIHZhbCBzYXZlZENvbG9yID0gcHJlZmVyZW5jZXMuZ2V0SW50KFwiY29sb3JcIiwgLTEpXG4gICAgICAgIC8vIE1pZ3JhdGUgdGhlIGZvcm1lciBjcmVhbSBvcHRpb24gdG8gdGhlIG5ldyBuZXV0cmFsIHdoaXRlIHBhbGV0dGUuXG4gICAgICAgIHZhbCBtaWdyYXRlZENvbG9yID0gaWYgKHNhdmVkQ29sb3IgPT0gMHhGRkZGRjRCRC50b0ludCgpKSAweEZGRjJGMkYyLnRvSW50KCkgZWxzZSBzYXZlZENvbG9yXG4gICAgICAgIHZhbCBjb2xvciA9IG1pZ3JhdGVkQ29sb3IudGFrZUlmIHsgYXJnYiAtPiBzdWJ0aXRsZVRleHRDb2xvcnMuYW55IHsgaXQuYXJnYiA9PSBhcmdiIH0gfSA/OiAtMSJ9XSwgImhlbHBlcnMiOiBbXX0=")) | ConvertFrom-Json
$base = Join-Path (Get-Location) "app\src\main\java\br\com\flaviotorres\iptvpremium\core\navigation"
if (-not (Test-Path -LiteralPath $base)) {
    $base = Join-Path (Get-Location) "src\main\java\br\com\flaviotorres\iptvpremium\core\navigation"
}
if (-not (Test-Path -LiteralPath $base)) { throw "Abra o terminal na raiz do projeto ou na pasta app." }
$marker = "// IPTV_SUBTITLE_READABILITY_V1"
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
Write-Host "Concluido: textos mais legiveis e seis tons para legendas. Compile e execute o app." -ForegroundColor Green
Write-Host "Backups dos arquivos alterados: .backup-$stamp"
