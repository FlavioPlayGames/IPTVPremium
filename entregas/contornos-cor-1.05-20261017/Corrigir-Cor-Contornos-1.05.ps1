param([string]$Projeto = (Get-Location).Path, [switch]$NaoCompilar)
$ErrorActionPreference = 'Stop'
$Projeto = [System.IO.Path]::GetFullPath($Projeto)
if (-not (Test-Path -LiteralPath (Join-Path $Projeto 'gradlew.bat'))) { throw 'Execute na raiz do projeto ou informe -Projeto.' }
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ('IPTV-contornos-cor-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
$archive = Join-Path $temp 'correcao.zip'
$payload = @'
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
'@
[System.IO.File]::WriteAllBytes($archive, [Convert]::FromBase64String($payload))
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne '31b1c7c0ea6cdb4b5a83d2772695eb6b1e1a41958f87ec5c9f5fd01bc784e22e') { throw 'Pacote incorreto. Nada foi aplicado.' }
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
Write-Host 'Confira a cor dos contornos nos sete temas antes de publicar. Instale por cima sem limpar dados.'
