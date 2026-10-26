param(
 [string]$Dispositivo,
 [string]$Arquivo=(Join-Path $env:USERPROFILE ('Downloads/m3u-estado-{0}.txt' -f (Get-Date -Format 'yyyyMMdd-HHmmss')))
)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if(!(Test-Path -LiteralPath $adb)){throw "ADB nao encontrado: $adb"}
if(!$Dispositivo){
 $lines=& $adb devices
 if($LASTEXITCODE -ne 0){throw 'Falha ao consultar dispositivos.'}
 $connected=@($lines | Where-Object {$_ -match '^\S+\s+device\s*$'} | ForEach-Object {($_ -split '\s+')[0]})
 if($connected.Count -ne 1){throw 'Conecte apenas a TV ou informe -Dispositivo com o identificador de adb devices.'}
 $Dispositivo=[string]$connected[0]
}
$package='br.com.flaviotorres.iptvpremium'
$info=& $adb -s $Dispositivo shell pm list packages -U $package
if($LASTEXITCODE -ne 0){throw 'Falha ao consultar o aplicativo.'}
$iptvUid=[regex]::Match(($info -join "`n"),'uid:\s*(\d+)').Groups[1].Value
if(!$iptvUid){throw 'Aplicativo nao encontrado no aparelho selecionado.'}
$parent=Split-Path -Parent $Arquivo
if($parent){New-Item -ItemType Directory -Force -Path $parent | Out-Null}
"M3U - estado e historico disponivel. Coleta: $(Get-Date -Format o)" | Out-File -LiteralPath $Arquivo -Encoding utf8
'===== LOGCAT: HISTORICO ATUAL DO BUFFER =====' | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
& $adb -s $Dispositivo logcat -b main,system,crash -v threadtime "--uid=$iptvUid" -d 2>&1 | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
if($LASTEXITCODE -ne 0){throw "Falha ao coletar logcat. Veja $Arquivo"}
'===== PROCESSO ATUAL =====' | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
& $adb -s $Dispositivo shell pidof $package 2>&1 | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
'===== MEMORIA DO APLICATIVO =====' | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
& $adb -s $Dispositivo shell dumpsys meminfo $package 2>&1 | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
'===== CPU: ESTADO ATUAL =====' | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
& $adb -s $Dispositivo shell dumpsys cpuinfo 2>&1 | Out-File -LiteralPath $Arquivo -Encoding utf8 -Append
Write-Host "Coleta salva: $Arquivo"
Write-Host 'A coleta nao limpa o buffer, nao reinicia o aplicativo e nao inicia outra importacao.'
