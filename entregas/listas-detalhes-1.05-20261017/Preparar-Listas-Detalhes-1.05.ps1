param([string]$Projeto = (Get-Location).Path, [switch]$NaoCompilar)
$ErrorActionPreference = 'Stop'
$Projeto = [System.IO.Path]::GetFullPath($Projeto)
if (-not (Test-Path -LiteralPath (Join-Path $Projeto 'gradlew.bat'))) { throw 'Execute na raiz do projeto ou informe -Projeto.' }
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ('IPTV-listas-detalhes-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
$archive = Join-Path $temp 'correcao.zip'
$payload = @'
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
qkn0TRGtO9NzlnG3NTGmoZMKKLRZnRQqcJHKPlqaniTPGgPCMVzcSiP0OuZs4K+wvI70VVmeboLW7nxZPj4OfovnJ9zO/7vPS3QDUqOKJGQQrivAwmX2Lk14
oHPKIxNTcmtDkubWw8ukItZSe2/2nC4E9WFiOTXEJCrZM3kBcquKwEmsxf0Ss4BjHYo7Ny6tRl8j9FwrmWnyKVOkqtG0FI7SPd5FU8w25+iWIMoubKSM5iQY
5hvbdpizyCuVEvxFSEYYTcWuLY7SJpfRKecaPx1hE3boYeOavWAhZxlcHMPREv7w7mg/vfn+L1BLAQIUAxQAAAAIAC6xUV2TnAWK0AUAAIcPAAALAAAAAAAA
AAAAAACAgQAAAABBcGxpY2FyLnBzMVBLAQIUAxQAAAAIAC6xUV0rO73+tgMAAA4HAAAKAAAAAAAAAAAAAACAgfkFAABMRUlBLU1FLm1kUEsBAhQDFAAAAAgA
eq9RXQa2QG9nBQAAUBIAAFgAAAAAAAAAAAAAAICB1wkAAGFycXVpdm9zL2FwcC9zcmMvbWFpbi9qYXZhL2JyL2NvbS9mbGF2aW90b3JyZXMvaXB0dnByZW1p
dW0vdWkvY29tcG9uZW50cy9Ib21lTWVudUNhcmQua3RQSwECFAMUAAAACAB6r1FdCudAb5YIAABCKgAAUAAAAAAAAAAAAAAAgIG0DwAAYXJxdWl2b3MvYXBw
L3NyYy9tYWluL2phdmEvYnIvY29tL2ZsYXZpb3RvcnJlcy9pcHR2cHJlbWl1bS91aS9ob21lL0hvbWVTY3JlZW4ua3RQSwECFAMUAAAACAARsVFdn/AXVykO
AACZQAAAXAAAAAAAAAAAAAAAgIG4GAAAYXJxdWl2b3MvYXBwL3NyYy9tYWluL2phdmEvYnIvY29tL2ZsYXZpb3RvcnJlcy9pcHR2cHJlbWl1bS91aS9wbGF5
bGlzdC9QbGF5bGlzdExpc3RTY3JlZW4ua3RQSwECFAMUAAAACAB6r1FdksI6eS0LAAC0JAAAUwAAAAAAAAAAAAAAgIFbJwAAYXJxdWl2b3MvYXBwL3NyYy9t
YWluL2phdmEvYnIvY29tL2ZsYXZpb3RvcnJlcy9pcHR2cHJlbWl1bS91aS90aGVtZS9UaGVtZUFydHdvcmsua3RQSwECFAMUAAAACAAusVFdQ1HvqoMCAACl
BwAADQAAAAAAAAAAAAAAgIH5MgAAbWFuaWZlc3QuanNvblBLBQYAAAAABwAHALsCAACnNQAAAAA=
'@
[System.IO.File]::WriteAllBytes($archive, [Convert]::FromBase64String($payload))
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne 'bacfa0d2dc4a7b29fa280354242b32ba944ac52259d3da24fd40e348a55b6c41') { throw 'Pacote incorreto. Nada foi aplicado.' }
Expand-Archive -LiteralPath $archive -DestinationPath $temp
# Prepare only the two version properties; all other local bytes are preserved.
$localFile = Join-Path $Projeto 'local.properties'
if (-not (Test-Path -LiteralPath $localFile -PathType Leaf)) { throw 'local.properties nao encontrado. Execute na raiz Android correta.' }
$localEncoding = [System.Text.Encoding]::GetEncoding(28591)
$localBytes = [System.IO.File]::ReadAllBytes($localFile)
$localText = $localEncoding.GetString($localBytes)
$codePattern = '(?m)^[ \t]*APP_VERSION_CODE(?:[ \t]*[=:][ \t]*|[ \t]+)([^\r\n]*)'
$namePattern = '(?m)^[ \t]*APP_VERSION_NAME(?:[ \t]*[=:][ \t]*|[ \t]+)([^\r\n]*)'
$codes = [regex]::Matches($localText, $codePattern)
$currentCode = 2L
if ($codes.Count -gt 0) {
    $number = $codes[$codes.Count - 1].Groups[1].Value.Trim()
    if ($number -notmatch '^\d+$' -or -not [long]::TryParse($number, [ref]$currentCode) -or $currentCode -lt 2 -or $currentCode -gt 2100000000) { throw 'APP_VERSION_CODE invalido em local.properties. Nada foi alterado.' }
}
$names = [regex]::Matches($localText, $namePattern)
if ($currentCode -gt 7) { throw 'O projeto ja tem numero interno maior que 7. Nada foi alterado; envie a versao atual.' }
$targetCode = 7L
$targetName = '1.05'
if ([string]::IsNullOrWhiteSpace($targetName) -or $targetName.Length -gt 40) { throw 'APP_VERSION_NAME invalido. Nada foi alterado.' }
$updatedLocal = $localText
# Only version assignments change; unknown properties keep their original bytes.
$codeLine = 'APP_VERSION_CODE=' + $targetCode
$nameLine = 'APP_VERSION_NAME=' + $targetName
if ($codes.Count -gt 0) { $updatedLocal = [regex]::Replace($updatedLocal, $codePattern, [System.Text.RegularExpressions.MatchEvaluator]{param($match) $codeLine}) }
else { $updatedLocal += "`r`n" + $codeLine + "`r`n" }
if ($names.Count -gt 0) { $updatedLocal = [regex]::Replace($updatedLocal, $namePattern, [System.Text.RegularExpressions.MatchEvaluator]{param($match) $nameLine}) }
else { $updatedLocal += "`r`n" + $nameLine + "`r`n" }

$executor = (Get-Process -Id $PID).Path
& $executor -NoProfile -ExecutionPolicy Bypass -File (Join-Path $temp 'Aplicar.ps1') -Projeto $Projeto
if ($LASTEXITCODE -ne 0) { throw 'Aplicacao interrompida. Envie a mensagem do instalador.' }
if ($updatedLocal -cne $localText) {
    $backup = Join-Path $Projeto ('.iptv-backups/versao-app-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
    New-Item -ItemType Directory -Path $backup -Force | Out-Null
    [System.IO.File]::WriteAllBytes((Join-Path $backup 'local.properties'), $localBytes)
    $temporary = Join-Path $Projeto ('local.properties.' + [guid]::NewGuid().ToString('N') + '.tmp')
    try {
        [System.IO.File]::WriteAllBytes($temporary, $localEncoding.GetBytes($updatedLocal))
        [System.IO.File]::Replace($temporary, $localFile, (Join-Path $backup 'local.properties'))
    } finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
}
Write-Host ('Versao preparada: ' + $targetName + ' | numero interno: ' + $targetCode)

if (-not $NaoCompilar) {
    $javaCandidates = @($env:JAVA_HOME)
    if ($env:ProgramFiles) { $javaCandidates += (Join-Path $env:ProgramFiles 'Android/Android Studio/jbr') }
    if ($env:LOCALAPPDATA) { $javaCandidates += (Join-Path $env:LOCALAPPDATA 'Programs/Android Studio/jbr') }
    $javaFound = $false
    foreach ($candidate in $javaCandidates) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'bin/java.exe') -PathType Leaf)) {
            $env:JAVA_HOME = $candidate
            $env:Path = (Join-Path $candidate 'bin') + [System.IO.Path]::PathSeparator + $env:Path
            $javaFound = $true
            break
        }
    }
    if (-not $javaFound -and -not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java nao encontrado. Informe o JAVA_HOME do Android Studio e execute este mesmo script novamente; o pacote ja aplicado sera reconhecido.' }
    Push-Location $Projeto
    try {
        & (Join-Path $Projeto 'gradlew.bat') assembleDebug
        if ($LASTEXITCODE -ne 0) { throw 'Compilacao falhou. Envie o erro.' }
        $apkFolder = Join-Path $Projeto 'app/build/outputs/apk/debug'
        $metadata = Get-Content -LiteralPath (Join-Path $apkFolder 'output-metadata.json') -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($metadata.applicationId -ne 'br.com.flaviotorres.iptvpremium' -or @($metadata.elements).Count -ne 5) { throw 'Saidas da compilacao inesperadas. Nao publique os APKs.' }
        foreach ($apk in $metadata.elements) {
            if ($apk.versionCode -ne 7 -or $apk.versionName -cne '1.05' -or -not (Test-Path -LiteralPath (Join-Path $apkFolder $apk.outputFile) -PathType Leaf)) { throw 'Versao ou arquivo de APK inesperado. Nao publique.' }
        }
        Write-Host 'Conferido: todos os cinco APKs estao na versao 1.05, numero interno 7.'
        Invoke-Item $apkFolder
    } finally { Pop-Location }
}
Write-Host 'Confira Minhas Listas: selecao, usar lista, setas e confirmacao de exclusao. Instale por cima sem limpar dados.'
