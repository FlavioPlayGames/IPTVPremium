# IPTVPremium: SOMENTE Home: cinco icones 30% menores e carregamento inicial sem menu cinza. Windows PowerShell 5.1 / PowerShell 7.
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
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/home/MarbleHomeGeometry.kt'; Binary = $false; Allowed = @('C8A6486267DB07EF2DA455D9F6E7B4D1E207CD6FFC59AA7FB36BCA1766CA81A6'); MayCreate = $false; After = '7AEEF45E2211461DB96395A916EF6CB5D85EFCF5AD866BF5ACC77BAA2EBF2E77'; PayloadHash = '7AEEF45E2211461DB96395A916EF6CB5D85EFCF5AD866BF5ACC77BAA2EBF2E77'; Data = 'H4sIAAAAAAACA5VWbW7bOBD971MQATYQs4yib9nZKos0TVtj81GkKfZnwNiUxa1MGhTttHVznmIPsCfIxXZIWXbk2N6tAFs03/Bx5nFm6AkdfKYjhu6VO5BjNy/pjEstlWKVyyd6NlFszKdjd8rdQo5Zp3N0cIBuC4ZyPmNoTLlAfCBFhahiKPV+QTJHumBcoVyqMVOo4t/Yb4iKQSFVRdCATjS39mKICq7NOlqhStOvQPmFDV10cNThQjMlaIkMtUYzGF2e3ry+OL+7PO1f3fXPrq/uPp6dXpyjDLmpl68WDKmmaFDSqkKXVN2X7IaNYD/HUJQs18d9oYn5oeVkOVZ8VKyQe6m1HJufGM07CB4z+8CHukAjph2cWftDQ7eEIWSYW+A1wyFs0XlcuSbv/2IDvXDrPYj5jsGXVl8Xu6xitXtlfpIGa0C9S9aL/OXGpRzJrBVqnHokionve10SJjG2phPFZ1Qzu0SC/xw86hvirOSVvs4da2WeFlcSAVVIwjglSc/DpI2GvR4JezFJukkNbyZJgx6JooT0GrM27HtBF/CQ+EE32sXjh0EC7qTETyC0TUxRGpDYI34cgpHvbyWyEvWAJwrALsZ4KadN5qylkDumk74YMkhONOdmQJTlOTxZ0vPcschJFuMaRKys2OJkm8eebfZZ6pILYNWFq+RUDJ16hWuP/WBzomNXS8hIB78gLLYT1uny04wmsTeQNqwG/nUxrivhAR8F+Wa+lu5m5UI7d1EjBbF0D+3pFcdjZ/VtfGv6x+609YOApJ5HQvikUYBJC42irkWTsFujW9I2rs16kHEvSXwPcsjAfgBJtIPGD5PaGci62nCVbdAAT03/2x1NFEEBBiRMwZ8keFGEMG3g1EtreEs0ABsz32vs1quwMQi9aBeRxa1hEmx0yI/ihIRQzrHpQmG4vQqhBANThSC0sVvpUtJ7KJ9Glb0zKiiv9sjeW16OmRl8fPpbcTv6gw/N6wJMqRmc6ikt+TeqYHwmRc5HU0Wffjz9A9b4eTtmYjpu3RRvuIL+bEp3ji7O396Sm/6797fk0wfy5vrPK7Srk1/BrTmi9VobRD6FBiAowMO6MdjbRcCVSMvzev74tZQlowJn1gDBTeq5boL295HTMkTfv6NFcwnxkp0LrjktnW2ceRvBXt2QwiWBYF+0M5jCTS/qu2/YxH+8psdmvwlIpy/hD4BZnHnYvJ71O3OMVGeLDVxNP7N+Pl9qsiYGfvz9eGNEuE04o7w085kDAVL9KsagWVSHFoN62M15Cae0Y6MloWJ6qgR6KJhwlrHjtZa9JoVrEgMdnqw8cY0M1+pqWpZzrl9RDZFQvZPD5lWbJOfqGcvJ/2H59MFQNDLM60GA41qM5NG+/oPDJnbDYu6u5kyb84KI4v39nz81tL5708cfO/8Ctml943IKAAA=' },
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/home/MarbleLayeredHomeArtwork.kt'; Binary = $false; Allowed = @('3401A83E1E5EA00CA3FD5F8B524863FDF5230750B091FC9965FB446840AD79FF'); MayCreate = $false; After = '0D1DB9BF925C573D1ED43FA198C8ADF5A4D7244DC5A675ED1172F2AA044AADCF'; PayloadHash = '0D1DB9BF925C573D1ED43FA198C8ADF5A4D7244DC5A675ED1172F2AA044AADCF'; Data = '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' },
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/home/MarbledHomeScreen.kt'; Binary = $false; Allowed = @('A4FB3C8529A35BDECDF00B1FEF188E216429D8F8596CEB78AA9105EF7904E809'); MayCreate = $false; After = '7393DEC9A85A06838D549CED4D866D9979711C439083A64009C6F3D34DF601E7'; PayloadHash = '7393DEC9A85A06838D549CED4D866D9979711C439083A64009C6F3D34DF601E7'; Data = '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' }
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
$backup = Join-Path $root ('.navigation-backups/home-icones70-inicio-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
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
Write-Host 'Atualizacao aplicada: icones principais da Home 30% menores e abertura sem menu cinza.' -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host 'Compile e execute pelo Android Studio para validar no seu aparelho.'
