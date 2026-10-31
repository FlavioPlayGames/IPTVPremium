param([string]$Dispositivo)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if(!(Test-Path -LiteralPath $adb)){throw 'ADB nao encontrado.'}
if(!$Dispositivo){
 $connected=@((& $adb devices) | Where-Object {$_ -match '^\S+\s+device\s*$'} | ForEach-Object {($_ -split '\s+')[0]})
 if($connected.Count -ne 1){throw 'Conecte um aparelho ou informe -Dispositivo.'}
 $Dispositivo=[string]$connected[0]
}
$appProcessId=((& $adb -s $Dispositivo shell pidof br.com.flaviotorres.iptvpremium) -join '').Trim()
if($appProcessId -notmatch '^\d+$'){throw 'Abra o aplicativo na TV antes de iniciar a captura.'}
$arquivo=Join-Path $env:USERPROFILE ('Downloads/fluidez-kids-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt')
Write-Host "Registro: $arquivo"
Write-Host 'Entre em Filmes e Series do Kids e navegue pelas capas. Pressione Ctrl+C e envie o TXT. Nao limpa logs nem dados.'
& $adb -s $Dispositivo logcat "--pid=$appProcessId" -T 1 -v time 'UiStallPerf:I' 'Choreographer:I' 'art:I' 'dalvikvm:I' '*:S' 2>&1 | Tee-Object -FilePath $arquivo
