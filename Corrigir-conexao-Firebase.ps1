param([string]$SitePath = (Join-Path $env:USERPROFILE "Downloads\Central-IPTV-Premium-site\site"))
$ErrorActionPreference = "Stop"
$payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("eyJwYXRjaGVzIjogW3siZmlsZSI6ICJzcmMvZmNtLmpzIiwgIm9sZCI6ICIgIH0gY2F0Y2ggeyB0aHJvdyBuZXcgQXV0aGVudGljYXRpb25FcnJvcignb2F1dGhfa2V5Jyk7IH1cbiAgbGV0IHJlc3BvbnNlO1xuICB0cnkge1xuICAgIHJlc3BvbnNlID0gYXdhaXQgZmV0Y2goJ2h0dHBzOi8vb2F1dGgyLmdvb2dsZWFwaXMuY29tL3Rva2VuJywge21ldGhvZDogJ1BPU1QnLCByZWRpcmVjdDogJ2Vycm9yJyxcbiAgICAgIGhlYWRlcnM6IHsnQ29udGVudC1UeXBlJzogJ2FwcGxpY2F0aW9uL3gtd3d3LWZvcm0tdXJsZW5jb2RlZCd9LCBzaWduYWw6IEFib3J0U2lnbmFsLnRpbWVvdXQoODAwMCksXG4gICAgICBib2R5OiBuZXcgVVJMU2VhcmNoUGFyYW1zKHtncmFudF90eXBlOiAndXJuOmlldGY6cGFyYW1zOm9hdXRoOmdyYW50LXR5cGU6and0LWJlYXJlcicsIGFzc2VydGlvbjogaGVhZGVyICsgJy4nICsgYm9keSArICcuJyArIGJhc2U2NHVybChuZXcgVWludDhBcnJheShzaWduYXR1cmUpKX0pLnRvU3RyaW5nKCl9KTtcbiAgfSBjYXRjaCB7IHRocm93IG5ldyBBdXRoZW50aWNhdGlvbkVycm9yKCdvYXV0aF9uZXR3b3JrJyk7IH1cbiIsICJuZXciOiAiICB9IGNhdGNoIHsgdGhyb3cgbmV3IEF1dGhlbnRpY2F0aW9uRXJyb3IoJ29hdXRoX2tleScpOyB9XG4gIGxldCByZXNwb25zZTtcbiAgdHJ5IHtcbiAgICByZXNwb25zZSA9IGF3YWl0IGZldGNoKCdodHRwczovL29hdXRoMi5nb29nbGVhcGlzLmNvbS90b2tlbicsIHttZXRob2Q6ICdQT1NUJywgcmVkaXJlY3Q6ICdtYW51YWwnLFxuICAgICAgaGVhZGVyczogeydDb250ZW50LVR5cGUnOiAnYXBwbGljYXRpb24veC13d3ctZm9ybS11cmxlbmNvZGVkJ30sIHNpZ25hbDogQWJvcnRTaWduYWwudGltZW91dCg4MDAwKSxcbiAgICAgIGJvZHk6IG5ldyBVUkxTZWFyY2hQYXJhbXMoe2dyYW50X3R5cGU6ICd1cm46aWV0ZjpwYXJhbXM6b2F1dGg6Z3JhbnQtdHlwZTpqd3QtYmVhcmVyJywgYXNzZXJ0aW9uOiBoZWFkZXIgKyAnLicgKyBib2R5ICsgJy4nICsgYmFzZTY0dXJsKG5ldyBVaW50OEFycmF5KHNpZ25hdHVyZSkpfSkudG9TdHJpbmcoKX0pO1xuICB9IGNhdGNoIHsgdGhyb3cgbmV3IEF1dGhlbnRpY2F0aW9uRXJyb3IoJ29hdXRoX25ldHdvcmsnKTsgfVxuIiwgImNvdW50IjogMX0sIHsiZmlsZSI6ICJzcmMvZmNtLmpzIiwgIm9sZCI6ICIgICAgY29uc3QgYXV0aG9yaXphdGlvbiA9IGF3YWl0IGFjY2Vzc1Rva2VuKGVudiwgc2VydmljZSk7XG4gICAgcGhhc2UgPSAnc2VuZCc7XG4gICAgY29uc3QgcmVzcG9uc2UgPSBhd2FpdCBmZXRjaChgaHR0cHM6Ly9mY20uZ29vZ2xlYXBpcy5jb20vdjEvcHJvamVjdHMvJHtzZXJ2aWNlLnByb2plY3RfaWR9L21lc3NhZ2VzOnNlbmRgLCB7XG4gICAgICBtZXRob2Q6ICdQT1NUJywgcmVkaXJlY3Q6ICdlcnJvcicsIHNpZ25hbDogQWJvcnRTaWduYWwudGltZW91dCg4MDAwKSxcbiAgICAgIGhlYWRlcnM6IHsnQXV0aG9yaXphdGlvbic6ICdCZWFyZXIgJyArIGF1dGhvcml6YXRpb24sICdDb250ZW50LVR5cGUnOiAnYXBwbGljYXRpb24vanNvbid9LFxuICAgICAgYm9keTogSlNPTi5zdHJpbmdpZnkoe21lc3NhZ2U6IHt0b2tlbiwgZGF0YToge3R5cGU6ICdsaXN0c19jaGFuZ2VkJywgdmVyc2lvbjogJzEnfSxcbiAgICAgICAgYW5kcm9pZDoge3ByaW9yaXR5OiAnTk9STUFMJywgdHRsOiAnODY0MDBzJywgY29sbGFwc2Vfa2V5OiAncG9ydGFsX2xpc3RzJ319fSl9KTtcbiIsICJuZXciOiAiICAgIGNvbnN0IGF1dGhvcml6YXRpb24gPSBhd2FpdCBhY2Nlc3NUb2tlbihlbnYsIHNlcnZpY2UpO1xuICAgIHBoYXNlID0gJ3NlbmQnO1xuICAgIGNvbnN0IHJlc3BvbnNlID0gYXdhaXQgZmV0Y2goYGh0dHBzOi8vZmNtLmdvb2dsZWFwaXMuY29tL3YxL3Byb2plY3RzLyR7c2VydmljZS5wcm9qZWN0X2lkfS9tZXNzYWdlczpzZW5kYCwge1xuICAgICAgbWV0aG9kOiAnUE9TVCcsIHJlZGlyZWN0OiAnbWFudWFsJywgc2lnbmFsOiBBYm9ydFNpZ25hbC50aW1lb3V0KDgwMDApLFxuICAgICAgaGVhZGVyczogeydBdXRob3JpemF0aW9uJzogJ0JlYXJlciAnICsgYXV0aG9yaXphdGlvbiwgJ0NvbnRlbnQtVHlwZSc6ICdhcHBsaWNhdGlvbi9qc29uJ30sXG4gICAgICBib2R5OiBKU09OLnN0cmluZ2lmeSh7bWVzc2FnZToge3Rva2VuLCBkYXRhOiB7dHlwZTogJ2xpc3RzX2NoYW5nZWQnLCB2ZXJzaW9uOiAnMSd9LFxuICAgICAgICBhbmRyb2lkOiB7cHJpb3JpdHk6ICdOT1JNQUwnLCB0dGw6ICc4NjQwMHMnLCBjb2xsYXBzZV9rZXk6ICdwb3J0YWxfbGlzdHMnfX19KX0pO1xuIiwgImNvdW50IjogMX1dLCAiaGVscGVycyI6IFtdfQ==")) | ConvertFrom-Json
$base = (Resolve-Path -LiteralPath $SitePath).Path
$publisher = Join-Path $base "Publicar-Painel.ps1"
if (-not (Test-Path -LiteralPath $publisher)) { throw "Informe a pasta original do site com -SitePath." }
$config = [IO.File]::ReadAllText((Join-Path $base "wrangler.jsonc")) | ConvertFrom-Json
if (-not $config.d1_databases[0].database_id -or $config.d1_databases[0].database_id -eq "SUBSTITUIR_PELO_ID_DO_BANCO") { throw "Use a pasta do site ja publicado. Nada foi alterado." }
$marker = "IPTV_FCM_FETCH_REDIRECT_FIX_V3"
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
    if ($count -ne $expectedCount) { throw "Versao diferente em $($change.file). Nada foi alterado. Informe o arquivo do site indicado para adaptar a atualizacao." }
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
Write-Host "Corrigindo modo de conexao compativel com Cloudflare e publicando a central..." -ForegroundColor Green
Write-Host "Backups dos arquivos alterados: .backup-$stamp"


& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $publisher -ConectarDominio
if ($LASTEXITCODE -ne 0) { throw "Publicacao incompleta. Confira o erro e reexecute." }
Write-Host "Conexao corrigida e publicada. No site, atualize com Ctrl+F5 e teste varios envios com o aplicativo aberto." -ForegroundColor Green
Write-Host "Teste excluir e enviar outra lista. O painel deve mostrar Aviso aceito pelo Firebase e, apos a sincronizacao, Recebido pelo aplicativo." -ForegroundColor Yellow
Write-Host "Esta atualizacao e so no site. Nenhum APK, segredo ou credencial Firebase precisa ser substituido." -ForegroundColor Yellow
