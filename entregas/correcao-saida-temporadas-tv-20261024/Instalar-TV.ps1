param([Parameter(Mandatory=$true)][string]$Projeto,[string]$Dispositivo)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if(!(Test-Path -LiteralPath $adb)){throw "ADB nao encontrado: $adb"}
if(!$Dispositivo){
 $lines=& $adb devices
 if($LASTEXITCODE -ne 0){throw 'Falha ao consultar dispositivos.'}
 $connected=@($lines | Where-Object {$_ -match '^\S+\s+device\s*$'} | ForEach-Object {($_ -split '\s+')[0]})
 if($connected.Count -ne 1){throw 'Conecte apenas a TV ou informe -Dispositivo com o identificador exibido por adb devices.'}
 $Dispositivo=[string]$connected[0]
}
$apk=Join-Path (Resolve-Path -LiteralPath $Projeto).Path 'app/build/outputs/apk/debug/app-universal-debug.apk'
if(!(Test-Path -LiteralPath $apk)){throw 'APK universal nao encontrado. Execute Aplicar.ps1 -Testar primeiro.'}
& $adb -s $Dispositivo shell am force-stop br.com.flaviotorres.iptvpremium
if($LASTEXITCODE -ne 0){throw 'Nao foi possivel parar o aplicativo na TV.'}
& $adb -s $Dispositivo install -r $apk
if($LASTEXITCODE -ne 0){throw 'Instalacao falhou. Nenhum dado foi apagado pelo script.'}
Write-Host 'APK instalado sem Run/Debug. Inicie a captura e abra o aplicativo pelo controle da TV.'
