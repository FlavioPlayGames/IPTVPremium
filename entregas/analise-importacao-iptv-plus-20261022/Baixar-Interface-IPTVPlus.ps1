[CmdletBinding()]
param([string]$PastaSaida = '')
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
if (-not $PastaSaida) { $PastaSaida = Join-Path $env:USERPROFILE 'Downloads' }
$folder = Join-Path $PastaSaida ('Interface-IPTVPlus-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,6))
New-Item -ItemType Directory -Path $folder | Out-Null
$file = Join-Path $folder 'iptvplus-interface.js'
$url = 'https://tv.iptvpluseplayer.live/bundle.js'
try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $file -PassThru -TimeoutSec 60
    $info = Get-Item -LiteralPath $file
    if ($info.Length -lt 1) { throw 'Resposta vazia.' }
    $reader = [IO.File]::OpenRead($file)
    try {
        $first = New-Object byte[] 512
        $count = $reader.Read($first,0,$first.Length)
        $prefix = [Text.Encoding]::UTF8.GetString($first,0,$count)
    } finally { $reader.Dispose() }
    if (($response.Headers['Content-Type'] -join ',') -match '(?i)text/html' -or $prefix.TrimStart([char]0xFEFF) -match '(?is)^\s*(?:<!doctype\s+html|<html\b)') { throw 'O servidor devolveu uma pagina HTML em vez do JavaScript.' }
    [ordered]@{
        origemPublica=$url; obtidoUtc=[DateTime]::UtcNow.ToString('o'); bytes=$info.Length
        sha256=(Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
        observacao='Arquivo publico referenciado em assets/index.html do APK analisado; nao executado e sem dados de listas.'
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $folder 'origem-interface.json') -Encoding UTF8
    Write-Host "Baixado sem executar: $file"
    Write-Host ('Tamanho: {0:N2} MB' -f ($info.Length / 1MB))
    Write-Host 'Envie iptvplus-interface.js e origem-interface.json aqui no chat.'
    Write-Host 'O emulador, as listas e nosso aplicativo nao foram alterados.'
} catch {
    Write-Host 'Nao foi possivel baixar o JavaScript publico. Nao informe credenciais e nao altere protecoes do aplicativo.'
    throw
}
