[CmdletBinding()]
param(
    [string]$Apk = '',
    [string]$PastaSaida = '',
    [ValidateRange(1,24)][int]$TamanhoMB = 24
)
$ErrorActionPreference = 'Stop'
if (-not $Apk) { $Apk = Join-Path $env:USERPROFILE 'Downloads\iptv-plus.apk' }
$Apk = (Resolve-Path -LiteralPath $Apk).Path
if (-not (Test-Path -LiteralPath $Apk -PathType Leaf)) { throw 'Arquivo APK nao encontrado.' }
if (-not $PastaSaida) { $PastaSaida = Join-Path (Split-Path -Parent $Apk) ('Analise-IPTVPlus-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,6)) }
if (Test-Path -LiteralPath $PastaSaida) { throw 'A pasta de saida ja existe. Use outra pasta para nao substituir arquivos.' }
$apkInfo = Get-Item -LiteralPath $Apk
if ($apkInfo.Length -lt 1) { throw 'O APK esta vazio.' }
$fullHash = (Get-FileHash -LiteralPath $Apk -Algorithm SHA256).Hash.ToLowerInvariant()
Add-Type -AssemblyName System.IO.Compression
New-Item -ItemType Directory -Path $PastaSaida | Out-Null
$buffer = New-Object byte[] ($TamanhoMB * 1024 * 1024)
$parts = [Collections.Generic.List[object]]::new()
$inputStream = [IO.File]::OpenRead($Apk)
try {
    $number = 0
    while ($true) {
        $count = 0
        while ($count -lt $buffer.Length) {
            $read = $inputStream.Read($buffer, $count, $buffer.Length - $count)
            if ($read -eq 0) { break }
            $count += $read
        }
        if ($count -eq 0) { break }
        $number++
        $name = 'iptv-plus-parte-{0:D3}.zip' -f $number
        $entryName = 'apk.parte.{0:D3}' -f $number
        $path = Join-Path $PastaSaida $name
        $hash = [Security.Cryptography.SHA256]::Create()
        try { $chunkHash = [BitConverter]::ToString($hash.ComputeHash($buffer, 0, $count)).Replace('-','').ToLowerInvariant() } finally { $hash.Dispose() }
        $file = [IO.File]::Open($path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        $archive = [IO.Compression.ZipArchive]::new($file, [IO.Compression.ZipArchiveMode]::Create, $false)
        try {
            $entry = $archive.CreateEntry($entryName, [IO.Compression.CompressionLevel]::NoCompression)
            $outputStream = $entry.Open()
            try { $outputStream.Write($buffer, 0, $count) } finally { $outputStream.Dispose() }
        } finally { $archive.Dispose(); $file.Dispose() }
        if ((Get-Item -LiteralPath $path).Length -ge 32MB) { throw 'Uma parte passou do limite. Rode novamente com -TamanhoMB 16.' }
        $parts.Add([pscustomobject]@{ ordem=$number; arquivo=$name; entrada=$entryName; bytes=$count; sha256Dados=$chunkHash; sha256Zip=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() })
        Write-Host ("Pronta: {0} ({1:N1} MB)" -f $name, ($count / 1MB))
    }
} finally { $inputStream.Dispose() }
[ordered]@{
    protocolo='apk-partes-v1'; arquivoOriginal=[IO.Path]::GetFileName($Apk)
    bytesOriginal=$apkInfo.Length; sha256Original=$fullHash; partes=@($parts.ToArray())
} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $PastaSaida 'manifesto-apk.json') -Encoding UTF8
Write-Host ''
Write-Host "Concluido. Pasta: $PastaSaida"
Write-Host 'Envie aqui todos os ZIPs dessa pasta e o manifesto-apk.json. Pode enviar em mais de uma mensagem.'
Write-Host 'O APK original e o emulador nao foram alterados.'
