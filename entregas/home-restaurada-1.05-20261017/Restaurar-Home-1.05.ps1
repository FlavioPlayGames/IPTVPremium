param([string]$Projeto = (Get-Location).Path, [switch]$NaoCompilar)
$ErrorActionPreference = 'Stop'
$Projeto = [System.IO.Path]::GetFullPath($Projeto)
if (-not (Test-Path -LiteralPath (Join-Path $Projeto 'gradlew.bat'))) { throw 'Execute na raiz do projeto ou informe -Projeto.' }
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ('IPTV-home-restaurada-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
$archive = Join-Path $temp 'correcao.zip'
$payload = @'
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
PpNlUl5eWj28Tqlttum0R0LICYpXUFcQfUQTy5oIgvdYQ+z14CAlKk1ykwbWojZPCTUowN/WL/c3dOKxoznz+SaT2famDlzr/SZ5VzofLbUGUxRHnrw1DeCI
XCtCwNQB5XwyQrVkLSHH5J0539VwhbxYUV/QQ80SBC1WdiqQnTGy9rxxxBQoQHefyZUMBt5JLMS5lvAO5OdH63i+n+Knaf41Tk83hf+f9m/x9yW2Yq5QdUzc
hIoLASqYeBRxEfpTbaE/0MgxIAZMVvuZ1TkrjfAK//6l1JPzrIIRFLKkhgoJKNZEjbOX5muQ1C/omFttFjO6llgkQ7vw7/Hh7uUPUEsBAhQDFAAAAAgACrBR
Xa5mLDDIBQAAhw8AAAsAAAAAAAAAAAAAAICBAAAAAEFwbGljYXIucHMxUEsBAhQDFAAAAAgACrBRXYCajbOqAQAAqgIAAAoAAAAAAAAAAAAAAICB8QUAAExF
SUEtTUUubWRQSwECFAMUAAAACAB6r1FdBrZAb2cFAABQEgAAWAAAAAAAAAAAAAAAgIHDBwAAYXJxdWl2b3MvYXBwL3NyYy9tYWluL2phdmEvYnIvY29tL2Zs
YXZpb3RvcnJlcy9pcHR2cHJlbWl1bS91aS9jb21wb25lbnRzL0hvbWVNZW51Q2FyZC5rdFBLAQIUAxQAAAAIAHqvUV0K50BvlggAAEIqAABQAAAAAAAAAAAA
AACAgaANAABhcnF1aXZvcy9hcHAvc3JjL21haW4vamF2YS9ici9jb20vZmxhdmlvdG9ycmVzL2lwdHZwcmVtaXVtL3VpL2hvbWUvSG9tZVNjcmVlbi5rdFBL
AQIUAxQAAAAIAHqvUV2Swjp5LQsAALQkAABTAAAAAAAAAAAAAACAgaQWAABhcnF1aXZvcy9hcHAvc3JjL21haW4vamF2YS9ici9jb20vZmxhdmlvdG9ycmVz
L2lwdHZwcmVtaXVtL3VpL3RoZW1lL1RoZW1lQXJ0d29yay5rdFBLAQIUAxQAAAAIAAqwUV1KTCVcugEAAJcEAAANAAAAAAAAAAAAAACAgUIiAABtYW5pZmVz
dC5qc29uUEsFBgAAAAAGAAYAMQIAACckAAAAAA==
'@
[System.IO.File]::WriteAllBytes($archive, [Convert]::FromBase64String($payload))
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne 'beb1b6711af3f79e9a9fc197bb11ea70e60c33c01c6800bf77332c4649006ddb') { throw 'Pacote incorreto. Nada foi aplicado.' }
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
Write-Host 'Confira a Home sem caixas e os fundos originais de todos os temas. Instale por cima sem limpar dados.'
