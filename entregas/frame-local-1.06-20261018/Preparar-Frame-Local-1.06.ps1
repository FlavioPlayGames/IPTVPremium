param([string]$Projeto = (Get-Location).Path, [switch]$NaoCompilar, [switch]$Verificar)
$ErrorActionPreference = 'Stop'
$Projeto = [System.IO.Path]::GetFullPath($Projeto)
if (-not (Test-Path -LiteralPath (Join-Path $Projeto 'gradlew.bat'))) { throw 'Execute na raiz do projeto ou informe -Projeto.' }
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ('IPTV-frame-local-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
$archive = Join-Path $temp 'correcao.zip'
$payload = @'
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
2jawAgudYuim8oYHBcx4BZ1Wv+VOfchE6TqhaJiEgLSnUx19GEa5YjOh9cj8/D6xxYiji182rJjQozcmqLWAPF4syawzPaRHZ+yPIW8rn3HXiKKFG9XwTCQv
o6tLhdtD4eSwp8tT3Y/TgNoblmuBZnqAUleFaOpyXxpBQnk0Rt0SYmiGi9OD6Xv5CE8szGBwpGFK7YyJtUdGWy5Nu2NjXAJ/8/ipBunMih+Od7JiBW6+xlaZ
tB4qRw4yt/Vm0va3gxHCR9AYQw/0eiEN3bl0iGiAQRG95lqqGm+NsHFxy/GuyjWeFjxd8wOcaPw8Im3vVnKE2qI4oJoaB9BlD9cKr5lwLFeoCQ3dHDjW4Bgc
aR00jT7FTW3fkZiAjWtALvw7E65uTAOGnUB3mxfadp0vCf7kvzypti+vbqBP8Atut6+ieH52/PKKzoOdg0cTeo0TQLv7SUBPkdUzGSmbaEbUipgC56Be2D6K
lgtSVEvbvkXvGtZk9D9QSwMEFAAAAAgAFCpSXUISI5oFCQAAoR4AAGAAAABhcnF1aXZvcy9hcHAvc3JjL21haW4vamF2YS9ici9jb20vZmxhdmlvdG9ycmVz
L2lwdHZwcmVtaXVtL2RhdGEvcmVwb3NpdG9yeS9FcGlzb2RlRnJhbWVMb2FkZXIua3TFWW1z27gR/u5fgdOkN6RHoRU38bS6c1zHSVpn7DgTJ+10bm40MAlK
iEmCBUDFupz/e58FX0RJpOTk0ilm/CISWOw+u9h9Fsp5eMungt3oIFRpECd8LpVVWgsTyNzOcy1SWaRBxC0PtMiVkXi72NuTaa60ZTyLtJIRFmdWZDY4o793
dv31VPN8JkMTvJA25fmO16956DZZm5WKSPLgkn6/hDrXqtCh2DLpUlhOer8XVksxF3pt7h2ZDItEUMilCucp8OhUs3s+Nx0rdsEZKi0CgxdYWe74UprbMx7O
GoNulU1kRntqVViZQcL+lndmkYXBtYAOM8jeNfGztLN3Qqey8dQnPueBVMFrmYiVZ0aEhZZ2AUyNcYpOhbF7ewf7++xjYmXKGU+s0Bm3cs5ZJFiRspDriBmR
Mkm2pT/RM4SF5jCZQTabi9+GbkKe8IXQjEcylCrjScD2D/Zk5gQmTN18EqFlr3JpVCRea56KC8UjLPiyxzByjT2tYHPMNYmy7Jg1GHhP/I05MZdJAWdgHnC5
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
ZcBM5fFjrle/QSMeukX9l6xIPBS6PnvWtFeRvDdeNaSwMcklA8pBUollgoiWPHBwDUepOfqYNGR23mawihtTX6w21xTkmcr3PzLJQBYMepCOENhq73PIktEw
clBsnQQwDMlBVpaMAk1KsyIfMvOP9Wl1ZzKvhfYtfa4X37uTuZX1B5ntrutudxsIvHExgkKQNkSXpPMUpGoSeB+1bbTTlIJsFn4puKyNWlDbnGNMXjaXbpe5
eTh9AVBLAQIUAxQAAAAIAEgqUl1q7BZNzwUAAIMPAAALAAAAAAAAAAAAAACAgQAAAABBcGxpY2FyLnBzMVBLAQIUAxQAAAAIAEgqUl2y1vn74gUAAJ0LAAAK
AAAAAAAAAAAAAACAgfgFAABMRUlBLU1FLm1kUEsBAhQDFAAAAAgAFCpSXUISI5oFCQAAoR4AAGAAAAAAAAAAAAAAAICBAgwAAGFycXVpdm9zL2FwcC9zcmMv
bWFpbi9qYXZhL2JyL2NvbS9mbGF2aW90b3JyZXMvaXB0dnByZW1pdW0vZGF0YS9yZXBvc2l0b3J5L0VwaXNvZGVGcmFtZUxvYWRlci5rdFBLAQIUAxQAAAAI
AAwqUl1uC1la9AEAADkEAABgAAAAAAAAAAAAAACAgYUVAABhcnF1aXZvcy9hcHAvc3JjL21haW4vamF2YS9ici9jb20vZmxhdmlvdG9ycmVzL2lwdHZwcmVt
aXVtL2RhdGEvcmVwb3NpdG9yeS9FcGlzb2RlUHJldmlld0ZpbGUua3RQSwECFAMUAAAACABIKlJdSKpEWjIBAAAJAwAADQAAAAAAAAAAAAAAgIH3FwAAbWFu
aWZlc3QuanNvblBLBQYAAAAABQAFAMgBAABUGQAAAAA=
'@
[System.IO.File]::WriteAllBytes($archive, [Convert]::FromBase64String($payload))
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne 'c9d9a8f97c944f6c59ac182901d95cb64b8409d9d5d8bfffb3ff3d6bc96b0c78') { throw 'Pacote incorreto. Nada foi aplicado.' }
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
if ($currentCode -gt 8) { throw 'O projeto ja tem numero interno maior que 8. Nada foi alterado; envie a versao atual.' }
$targetCode = 8L
$targetName = '1.06'
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
if ($Verificar) {
    & $executor -NoProfile -ExecutionPolicy Bypass -File (Join-Path $temp 'Aplicar.ps1') -Projeto $Projeto -Verificar
    if ($LASTEXITCODE -ne 0) { throw 'Verificacao dos fontes falhou. Nada foi alterado ou publicado.' }
    exit 0
}
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
            if ($apk.versionCode -ne 8 -or $apk.versionName -cne '1.06' -or -not (Test-Path -LiteralPath (Join-Path $apkFolder $apk.outputFile) -PathType Leaf)) { throw 'Versao ou arquivo de APK inesperado. Nao publique.' }
        }
        Write-Host 'Conferido: todos os cinco APKs estao na versao 1.06, numero interno 8.'
        Invoke-Item $apkFolder
    } finally { Pop-Location }
}
Write-Host 'Confira os episodios sem foto e os registros EpisodePreview antes de publicar. Instale por cima sem limpar dados.'
