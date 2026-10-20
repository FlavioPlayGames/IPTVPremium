# IPTVPremium: Suave 25%, Medio 50% e Forte 80%. Windows PowerShell 5.1 / PowerShell 7.
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
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/PlaybackImageEnhancement.kt'; Allowed = @('6B2BE591874A77A327F9D9DD7FC80DD43CA847C7DF9D3ACC7BE0F23CC2FD600C', 'F3E7875EA613E7D07643068289CA88659F3731C5FDCCA214014F3E6409CE9F29'); MayCreate = $false; After = '1B0CAB57961962E3FFDD87A6D50BF5D7212D85983F1DE4BCEEB4687A41658BB7'; PayloadHash = '1B0CAB57961962E3FFDD87A6D50BF5D7212D85983F1DE4BCEEB4687A41658BB7'; Data = 'H4sIAAAAAAACA4VUS2/aQBC+8ytGlhrZresAEVKK2lRpKQEpFITT5rzgAba1d931mjya/KAccu69/LHO2gsxJKg+IO3O43vMLCmb/mRzhIkKpjIJZjFbcqmlUpgFPNXLVGHC84SCCoM0ZjeoajUUeQLTmGUZjOhqQj36CXUZyAjdJYtBcx1jG0KtuJh78LsG9A27XdfpYMY0X7KIOZ4P5/2z3oXrnOMS4e8fEFzzCG+Boqku0kxSNxw3XId+oQFvYPz5NDS5eJ2i4gkKzWLHq93vJUUkUMz14jkxH8wVS2QudBu6sWR6zTUcdolXmLOSWLP1yvEhaLZmxGfwpdP/NnCdweoh4tKEW/Ui3KqbcHgxHn49I8JS6aL4uIweU5RoRkyzl2h+EQsmpmgEuQUFwy0hQ9vPPYYPz+8CstffFGZWdPtlL3YbrO8Do7vm1WpcaFSCGsnJD5zq7eyRjPn0xjo1ywXkIsvT1OiN3D2UfVhEqg2fpIyRCR8ilTydika73xWPDP++0FSLfL7Q9jBLMzstkkGmrsf5kY5XCxSWmPmSwqw9bsHbExB5HG+yiZO5cwYYL6TiDKLNsrZhuXqMUEKqpMY5j2TgbOpI2b46EJjRFtjiXmd82JHx5Aa+84xLUemRsOvhzC00r+V6cAKNd8063N1BwsWL4fpxfQeaZ6kUq8cl0mbr1UORkkLYGVfB9tpi3hocHBiPqf1RPWjNiv7l66s2T5liVldGgkuwozp0R2EFCeMMt3y+3yyNzHWa65Dfort31AaQOF5W4vaq95RGCzBiXL0vwvRzUlkAhb9yriwCKaobcSWEOXmbRPNoYp5wfUlbVIWlfz5UU+wLt+EX43ihpvdUU/LaLqIJbBdlUxabR1xOtTnzLXSgZbHXrgeHYIddAmxF7PwtyKk+R5ZptzHzKrJ1rgRY2a9LQI+akD3ubmGR5IGW4Fpn/lNg8e0872v/AGcGWIVEBgAA' }
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
        $current = Get-TextHash $original
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
$backup = Join-Path $root ('.navigation-backups/nitidez-percentuais-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
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
Write-Host 'Atualizacao aplicada: Suave 25%, Medio 50% e Forte 80%.' -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host 'Compile e execute pelo Android Studio para validar no seu aparelho.'
