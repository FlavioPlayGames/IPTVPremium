param(
    [ValidateSet('Secoes','Completa')][string]$Modo = 'Secoes',
    [string]$Dispositivo = 'emulator-5554',
    [string]$PastaSaida = (Get-Location).Path,
    [string]$AdbPath = '',
    [switch]$AbrirAplicativo
)
$ErrorActionPreference = 'Stop'
if (-not $AdbPath) { $AdbPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe' }
if (-not (Test-Path -LiteralPath $AdbPath)) { throw 'ADB nao encontrado.' }
if ($Dispositivo -notmatch '^[A-Za-z0-9_.:-]+$') { throw 'Serial invalido.' }
& $AdbPath -s $Dispositivo get-state
if ($LASTEXITCODE -ne 0) { throw 'Emulador indisponivel.' }
& $AdbPath -s $Dispositivo shell am force-stop 'br.com.flaviotorres.iptvpremium'
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel fechar o aplicativo.' }
$enabled = if ($Modo -eq 'Secoes') { 'true' } else { 'false' }
$result = & $AdbPath -s $Dispositivo shell am instrument -w -e class 'br.com.flaviotorres.iptvpremium.data.sync.SectionCatalogDeviceTest#configureMode' -e catalog_sections_enabled $enabled 'br.com.flaviotorres.iptvpremium.test/androidx.test.runner.AndroidJUnitRunner' 2>&1
$code = $LASTEXITCODE
New-Item -ItemType Directory -Force -Path $PastaSaida | Out-Null
$report = Join-Path $PastaSaida 'diagnostico-modo-catalogo.txt'
$result | Out-File -LiteralPath $report -Encoding utf8
$result | Write-Host
$expected = if ($Modo -eq 'Secoes') { 'ATIVADO' } else { 'DESATIVADO' }
$text = $result | Out-String
if ($code -ne 0 -or $text -notmatch 'OK \(1 test\)' -or $text -notmatch ('CATALOGO_POR_SECOES=' + $expected) -or $text -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') { throw "Configuracao nao confirmada. Envie $report" }
& $AdbPath -s $Dispositivo shell am force-stop 'br.com.flaviotorres.iptvpremium'
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel concluir a configuracao.' }
Write-Host "Modo $Modo confirmado. Listas, favoritos e historico preservados."
if ($AbrirAplicativo) {
    & $AdbPath -s $Dispositivo shell am start -W -n 'br.com.flaviotorres.iptvpremium/.MainActivity'
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel abrir o aplicativo.' }
}
