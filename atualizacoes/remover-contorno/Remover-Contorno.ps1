# IPTVPremium: desfazer apenas fluidez e contorno temporario. Windows PowerShell 5.1 / PowerShell 7.
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
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/components/CatalogGridNavigation.kt'; Delete = $false; MayCreate = $false; Allowed = @('5CCCA92346598AD051CAB556C476D2B5E9B2024067BCC019638F4CEAC4BD805F'); After = 'A650491237B635CD10973B7CD024A7BF021E7A2944C2588AA2759002DCAA9997'; PayloadHash = 'A650491237B635CD10973B7CD024A7BF021E7A2944C2588AA2759002DCAA9997'; Data = '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' },
    @{ Path = 'app/src/androidTest/java/br/com/flaviotorres/iptvpremium/data/repository/CatalogSmoothScrollDeviceTest.kt'; Delete = $false; MayCreate = $false; Allowed = @('50B1A3F0A433EAC9FBC8634D40BB239280D966FF708FB1C504574B62AE19FD35'); After = 'AAB0785D42B0FF3F1108AC7012F1018833070C46B64C86E23B53CF3C31C2DA5F'; PayloadHash = 'AAB0785D42B0FF3F1108AC7012F1018833070C46B64C86E23B53CF3C31C2DA5F'; Data = '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' },
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/components/CatalogMovingOutline.kt'; Delete = $true; Allowed = @('AD4A2DAD803134A7BA69F492D9A812DF459064ECE5A45A78FEA20A5BA2740EAB') }
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
    if ($file.Delete) {
        if (!$present) { continue }
        $original = [IO.File]::ReadAllBytes($path)
        if ($file.Allowed -notcontains (Get-TextHash $original)) {
            throw "Arquivo com mudancas locais: $($file.Path). Nada foi alterado."
        }
        $pending += @{ Path = $path; Relative = $file.Path; Original = $original; Present = $true; Delete = $true }
        continue
    }
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
    $pending += @{ Path = $path; Relative = $file.Path; Bytes = $bytes; Original = $original; Present = $present; Hash = $file.PayloadHash; Delete = $false }
}
if ($pending.Count -eq 0) {
    Write-Host 'Esta atualizacao ja esta aplicada.' -ForegroundColor Green
    return
}
$backup = Join-Path $root ('.navigation-backups/desfazer-fluidez-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
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
        if ($file.Delete) { [IO.File]::Delete($file.Path); continue }
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
Write-Host 'Contorno temporario removido. Ajustes de movimento e botao Voltar preservados.' -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host 'Compile e execute pelo Android Studio para validar no seu aparelho.'
