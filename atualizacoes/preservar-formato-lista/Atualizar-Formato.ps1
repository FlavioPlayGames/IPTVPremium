# IPTVPremium: Listas: preservar o formato de importacao definido no cadastro. Windows PowerShell 5.1 / PowerShell 7.
[CmdletBinding()]
param([string]$Projeto = (Get-Location).Path)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
function Get-BytesHash([byte[]]$Bytes) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($Bytes))).Replace('-', '') }
    finally { $sha.Dispose() }
}
function Get-TextHash([byte[]]$Bytes) {
    $text = [Text.Encoding]::UTF8.GetString($Bytes).TrimStart([char]0xFEFF).Replace("`r`n", "`n")
    return Get-BytesHash ([Text.Encoding]::UTF8.GetBytes($text))
}
$files = @(
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/data/sync/M3uSourceSynchronizer.kt'; Binary = $false; Allowed = @('50D2913EC1D70EDB17C2086AFCADCB5A020C14A886E38A4207DAD5ECD37E0164'); MayCreate = $false; After = 'B106A76EF96705BBE75B625C5FA2BEAEC971C7FBB787B4F4B15F586B8DF5CFD8'; PayloadHash = 'B106A76EF96705BBE75B625C5FA2BEAEC971C7FBB787B4F4B15F586B8DF5CFD8'; Data = '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' }
)
$root = (Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath (Join-Path $root 'app/build.gradle.kts'))) {
    throw 'Abra o terminal na raiz de IPTVPremium (pasta com app e gradlew), ou informe -Projeto.'
}
# Verifica a versao de todos os arquivos antes de escrever qualquer mudanca.
$pending = @()
foreach ($file in $files) {
    $path = Join-Path $root $file.Path
    $present = Test-Path -LiteralPath $path -PathType Leaf
    $original = $null
    if ($present) {
        $original = [IO.File]::ReadAllBytes($path)
        $current = if ($file.Binary) { Get-BytesHash $original } else { Get-TextHash $original }
        if ($current -eq $file.After) { continue }
        if ($file.Allowed -notcontains $current) {
            throw "Arquivo com mudancas diferentes da versao enviada: $($file.Path). Nada foi alterado; suas mudancas foram preservadas."
        }
    } elseif (!$file.MayCreate) {
        throw "Arquivo necessario ausente: $($file.Path). Nada foi alterado."
    }
    $packed = [IO.MemoryStream]::new([Convert]::FromBase64String($file.Data))
    $gzip = [IO.Compression.GZipStream]::new($packed, [IO.Compression.CompressionMode]::Decompress)
    $plain = [IO.MemoryStream]::new()
    try { $gzip.CopyTo($plain); $bytes = $plain.ToArray() }
    finally { $gzip.Dispose(); $plain.Dispose(); $packed.Dispose() }
    if ((Get-BytesHash $bytes) -ne $file.PayloadHash) { throw 'Pacote com integridade invalida. Nada foi alterado.' }
    $pending += @{ Path = $path; Relative = $file.Path; Bytes = $bytes; Original = $original; Present = $present; Hash = $file.PayloadHash }
}
if ($pending.Count -eq 0) {
    Write-Host 'Esta atualizacao ja esta aplicada.' -ForegroundColor Green
    return
}
$backup = Join-Path $root ('.navigation-backups/preservar-formato-lista-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
[IO.Directory]::CreateDirectory($backup) | Out-Null
foreach ($file in $pending) {
    if ($file.Present) {
        $copy = Join-Path $backup $file.Relative
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($copy)) | Out-Null
        [IO.File]::Copy($file.Path, $copy, $false)
        if ((Get-FileHash -LiteralPath $copy -Algorithm SHA256).Hash -ne (Get-BytesHash $file.Original)) {
            throw 'Backup nao passou na verificacao. Nenhum arquivo do projeto foi alterado.'
        }
    }
}
@($pending | ForEach-Object { @{ path = $_.Relative; existed = $_.Present } }) |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'manifest.json') -Encoding UTF8
try {
    foreach ($file in $pending) {
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($file.Path)) | Out-Null
        [IO.File]::WriteAllBytes($file.Path, $file.Bytes)
        if ((Get-FileHash -LiteralPath $file.Path -Algorithm SHA256).Hash -ne $file.Hash) { throw "Falha ao gravar $($file.Relative)." }
    }
} catch {
    foreach ($file in $pending) {
        if ($file.Present) { [IO.File]::WriteAllBytes($file.Path, $file.Original) }
        elseif (Test-Path -LiteralPath $file.Path -PathType Leaf) { [IO.File]::Delete($file.Path) }
    }
    throw
}
Write-Host 'Atualizacao aplicada: Xtream permanece Xtream e M3U permanece M3U.' -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host 'Compile e execute pelo Android Studio para validar no seu aparelho.'
