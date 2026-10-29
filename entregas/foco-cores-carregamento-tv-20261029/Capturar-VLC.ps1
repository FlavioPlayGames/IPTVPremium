param([string]$Dispositivo)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if(!(Test-Path -LiteralPath $adb)){throw 'ADB nao encontrado.'}
if(!$Dispositivo){
 $lines=& $adb devices
 if($LASTEXITCODE -ne 0){throw 'Falha ao consultar dispositivos.'}
 $connected=@($lines | Where-Object {$_ -match '^\S+\s+device\s*$'} | ForEach-Object {($_ -split '\s+')[0]})
 if($connected.Count -ne 1){throw 'Conecte apenas a TV ou informe -Dispositivo.'}
 $Dispositivo=[string]$connected[0]
}
$out=Join-Path ([Environment]::GetFolderPath('Desktop')) ('diagnostico-vlc-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt')
Write-Host ('Registro: '+$out)
Write-Host 'Abra o mesmo video no VLC. Se demorar, saia e tente abrir no Media3 sem esperar. Aguarde a abertura ou a mensagem de erro. Pressione Ctrl+C para encerrar e envie o arquivo.'
& $adb -s $Dispositivo logcat -T 1 -v time 'PlayerLoadPerf:I' 'VlcNativePerf:I' 'VlcReleaseStepPerf:I' 'VlcSurfacePerf:I' 'VlcFontPerf:I' 'PlayerReleasePerf:I' '*:S' | Tee-Object -FilePath $out
