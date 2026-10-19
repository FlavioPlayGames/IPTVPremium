param(
    [ValidateSet('ATUAL','WAL','NORMAL','LOTES')][string]$Perfil = 'ATUAL',
    [ValidateSet('M3U','XTREAM')][string]$Formato = 'M3U',
    [string]$Dispositivo = 'emulator-5554',
    [string]$PastaSaida = (Get-Location).Path,
    [string]$AdbPath = '',
    [switch]$SomenteConfigurar
)
$ErrorActionPreference='Stop'
if (-not $AdbPath) { $AdbPath=Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe' }
if (-not (Test-Path -LiteralPath $AdbPath)) { throw 'ADB nao encontrado.' }
$state=& $AdbPath -s $Dispositivo get-state
if ($LASTEXITCODE -ne 0 -or ($state | Out-String).Trim() -ne 'device') { throw 'Abra o emulador e confira o serial em adb devices.' }
$medidor=Join-Path $PSScriptRoot 'Medir-Perfil-Importacao-1.07.ps1'
if (-not $SomenteConfigurar -and -not (Test-Path -LiteralPath $medidor)) { throw 'Medidor ausente. Mantenha os scripts na mesma pasta.' }
New-Item -ItemType Directory -Force -Path $PastaSaida | Out-Null
Write-Host 'Fecharemos o app para aplicar o perfil. Listas, historico e cache serao preservados.'
& $AdbPath -s $Dispositivo shell am force-stop 'br.com.flaviotorres.iptvpremium'
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel encerrar o aplicativo.' }
$result=& $AdbPath -s $Dispositivo shell am instrument -w -e class 'br.com.flaviotorres.iptvpremium.data.cache.ImportTuningDeviceTest#configureProfile' -e perfil $Perfil 'br.com.flaviotorres.iptvpremium.test/androidx.test.runner.AndroidJUnitRunner' 2>&1
$exit=$LASTEXITCODE
$log=Join-Path $PastaSaida ('diagnostico-configurar-'+$Perfil.ToLowerInvariant()+'.txt')
$result | Out-File -LiteralPath $log -Encoding utf8
$text=$result | Out-String
if ($exit -ne 0 -or $text -notmatch 'OK \(1 test\)' -or $text -notmatch ('PERFIL_CONFIGURADO='+$Perfil+'\b') -or $text -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') { throw ('Perfil nao confirmado. Envie '+$log) }
& $AdbPath -s $Dispositivo shell am force-stop 'br.com.flaviotorres.iptvpremium'
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel reiniciar o processo apos configurar.' }
Write-Host ('Perfil aplicado: '+$Perfil+'. Ele permanece selecionado ate voce trocar; release ignora esses testes.')
if ($SomenteConfigurar) { return }
& $medidor -Dispositivo $Dispositivo -PastaSaida $PastaSaida -Formato $Formato -Perfil $Perfil -AdbPath $AdbPath -AbrirAplicativo
