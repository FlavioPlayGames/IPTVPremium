param([string]$Dispositivo)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if(!(Test-Path -LiteralPath $adb)){throw 'ADB nao encontrado.'}
if(!$Dispositivo){
 $connected=@((& $adb devices) | Where-Object {$_ -match '^\S+\s+device\s*$'} | ForEach-Object {($_ -split '\s+')[0]})
 if($connected.Count -ne 1){throw 'Conecte um aparelho ou informe -Dispositivo.'}
 $Dispositivo=[string]$connected[0]
}
$arquivo=Join-Path $env:USERPROFILE ('Downloads/diagnostico-canais-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt')
Write-Host "Registro: $arquivo"
Write-Host 'Abra e troque canais. Aguarde a pausa, pressione Ctrl+C e envie o TXT. Nao limpa o log do aparelho.'
& $adb -s $Dispositivo logcat -T 1 -v time 'LiveLoadPerf:I' '*:S' 2>&1 | Tee-Object -FilePath $arquivo
