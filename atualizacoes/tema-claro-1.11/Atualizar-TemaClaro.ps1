# IPTVPremium: Versao 1.11: branco acinzentado somente no tema claro. Windows PowerShell 5.1 / PowerShell 7.
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
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/components/CatalogStyle.kt'; Binary = $false; Allowed = @('29240EF28CF3CFEE68322EBDB7A0FC91D581E6DD6CD5F076E0641F6C9C696516'); MayCreate = $false; After = '26883F591C4813DB858459B869621B4C4082290305FDE30D585040A324B7DA7A'; PayloadHash = '26883F591C4813DB858459B869621B4C4082290305FDE30D585040A324B7DA7A'; Data = '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' },
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/theme/MarbleArtwork.kt'; Binary = $false; Allowed = @('7667193D5267669CBECD2B7DB0D4FB617456DD1C8B8A780D756A66C947DBFCD5'); MayCreate = $false; After = '57E0C5D6EE48DB6369B0ECE222D9EEBBD967DC4DCD525100673E0EEB767A77CB'; PayloadHash = '57E0C5D6EE48DB6369B0ECE222D9EEBBD967DC4DCD525100673E0EEB767A77CB'; Data = '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' },
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/core/device/DeviceIdentityCard.kt'; Binary = $false; Allowed = @('E5B9CE30477683D00B7B64B749B094FD0E11699A7B1BF7AEE8CF7F4FE8F0DD41'); MayCreate = $false; After = 'AFE3170534A9F12FC592E75D232028EAF75B2659F59E9B7B1DF1426369CA2E61'; PayloadHash = 'AFE3170534A9F12FC592E75D232028EAF75B2659F59E9B7B1DF1426369CA2E61'; Data = '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' }
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
# Planeja somente as chaves de versao; nenhuma configuracao privada e substituida.
$gradlePath = Join-Path $root 'app/build.gradle.kts'
$gradleOriginal = [IO.File]::ReadAllBytes($gradlePath)
$gradleBom = $gradleOriginal.Length -ge 3 -and $gradleOriginal[0] -eq 239 -and $gradleOriginal[1] -eq 187 -and $gradleOriginal[2] -eq 191
$gradleText = [Text.Encoding]::UTF8.GetString($gradleOriginal).TrimStart([char]0xFEFF)
$codePattern = '(versionCode\s*=\s*localProperties\.getProperty\("APP_VERSION_CODE",\s*")(\d+)("\))'
$namePattern = '(versionName\s*=\s*localProperties\.getProperty\("APP_VERSION_NAME",\s*")([^"\r\n]+)("\))'
$codeMatches = [regex]::Matches($gradleText, $codePattern)
$nameMatches = [regex]::Matches($gradleText, $namePattern)
if ($codeMatches.Count -ne 1 -or $nameMatches.Count -ne 1) { throw 'Formato da versao no Gradle diferente do esperado. Nada foi alterado.' }
$currentCode = [long]$codeMatches[0].Groups[2].Value
$currentName = $nameMatches[0].Groups[2].Value
$localPath = Join-Path $root 'local.properties'
$localPresent = Test-Path -LiteralPath $localPath -PathType Leaf
$localOriginal = if ($localPresent) { [IO.File]::ReadAllBytes($localPath) } else { $null }
$localBom = $localPresent -and $localOriginal.Length -ge 3 -and $localOriginal[0] -eq 239 -and $localOriginal[1] -eq 187 -and $localOriginal[2] -eq 191
$localText = if ($localPresent) { [Text.Encoding]::UTF8.GetString($localOriginal).TrimStart([char]0xFEFF) } else { '' }
$localCodePattern = '(?m)^[ \t]*APP_VERSION_CODE[ \t]*[=:][ \t]*([^\r\n]*)(?:\r?$)'
$localNamePattern = '(?m)^[ \t]*APP_VERSION_NAME[ \t]*[=:][ \t]*([^\r\n]*)(?:\r?$)'
$localCodes = [regex]::Matches($localText, $localCodePattern)
$localNames = [regex]::Matches($localText, $localNamePattern)
if ($localCodes.Count -gt 1 -or $localNames.Count -gt 1) { throw 'Chaves de versao duplicadas em local.properties. Nada foi alterado.' }
if ($localCodes.Count -eq 1) {
    $parsedCode = 0L
    if (![long]::TryParse($localCodes[0].Groups[1].Value.Trim(), [ref]$parsedCode)) { throw 'APP_VERSION_CODE invalido. Nada foi alterado.' }
    $currentCode = $parsedCode
}
if ($localNames.Count -eq 1) { $currentName = $localNames[0].Groups[1].Value.Trim() }
if ($currentCode -lt 2 -or $currentCode -gt 2100000000) { throw 'Numero interno fora do intervalo permitido. Nada foi alterado.' }
$targetCode = if ($currentName -eq '1.11' -and $currentCode -ge 11) { $currentCode } else { [Math]::Max(11L, $currentCode + 1L) }
if ($targetCode -gt 2100000000) { throw 'Nao e possivel incrementar o numero interno. Nada foi alterado.' }
$gradleNew = [regex]::Replace($gradleText, $codePattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) $m.Groups[1].Value + $targetCode + $m.Groups[3].Value })
$gradleNew = [regex]::Replace($gradleNew, $namePattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) $m.Groups[1].Value + '1.11' + $m.Groups[3].Value })
$eol = if ($localText.Contains("`r`n")) { "`r`n" } else { "`n" }
$localNew = $localText
foreach ($setting in @(@{ Pattern=$localCodePattern; Line="APP_VERSION_CODE=$targetCode" }, @{ Pattern=$localNamePattern; Line='APP_VERSION_NAME=1.11' })) {
    if ([regex]::IsMatch($localNew, $setting.Pattern)) {
        $line = $setting.Line
        $localNew = [regex]::Replace($localNew, $setting.Pattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) $line + $(if($m.Value.EndsWith("`r")){"`r"}else{''}) })
    } else {
        if ($localNew.Length -gt 0 -and !$localNew.EndsWith("`n")) { $localNew += $eol }
        $localNew += $setting.Line + $eol
    }
}
foreach ($versionFile in @(
    @{ Relative='app/build.gradle.kts'; Path=$gradlePath; Original=$gradleOriginal; Present=$true; Text=$gradleNew; Bom=$gradleBom },
    @{ Relative='local.properties'; Path=$localPath; Original=$localOriginal; Present=$localPresent; Text=$localNew; Bom=$localBom }
)) {
    $text = if ($versionFile.Bom) { [string][char]0xFEFF + $versionFile.Text } else { $versionFile.Text }
    $bytes = [Text.Encoding]::UTF8.GetBytes($text)
    if (!$versionFile.Present -or (Get-BytesHash $versionFile.Original) -ne (Get-BytesHash $bytes)) {
        $pending += @{ Path=$versionFile.Path; Relative=$versionFile.Relative; Bytes=$bytes; Original=$versionFile.Original; Present=$versionFile.Present; Hash=(Get-BytesHash $bytes) }
    }
}

if ($pending.Count -eq 0) {
    Write-Host 'Esta atualizacao ja esta aplicada.' -ForegroundColor Green
    return
}
$backup = Join-Path $root ('.navigation-backups/tema-claro-1.11-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
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
Write-Host 'Atualizacao aplicada: tema claro em branco acinzentado e versao 1.11.' -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host 'Compile e execute pelo Android Studio para validar no seu aparelho.'
