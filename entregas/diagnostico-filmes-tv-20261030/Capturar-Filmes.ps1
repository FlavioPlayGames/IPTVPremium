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
$out=Join-Path ([Environment]::GetFolderPath('Desktop')) ('diagnostico-filmes-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt')
function Protect-LogLine([string]$Line){
 $safe=[regex]::Replace($Line,'(?i)\b(?:https?|rtsp|rtmp)://[^\s"<>]+','[URL removida]')
 $safe=[regex]::Replace($safe,'(?i)\b(?:authorization|cookie|set-cookie)\s*[:=].*','[credencial removida]')
 [regex]::Replace($safe,'(?i)\b(username|password|token|api_key)\s*[:=]\s*[^\s,;]+','$1=[removido]')
}
Write-Host ('Registro: '+$out)
Write-Host 'Abra um filme afetado desde a capa, espere a tela de detalhes, clique em Assistir e aguarde o erro. Depois pressione Ctrl+C e envie o TXT e o nome do filme.'
& $adb -s $Dispositivo logcat -T 1 -v time 'NavigationPerf:D' 'MovieDetailsImages:D' 'TmdbEnrichment:D' 'SeriesArtwork:D' 'PosterDownloadResult:D' 'PosterLoadPerf:D' 'PlayerLoadPerf:I' 'VlcNativePerf:I' 'VlcReleaseStepPerf:I' 'PlayerReleasePerf:I' 'ExoPlayerImplInternal:E' 'AndroidRuntime:E' '*:S' | ForEach-Object { Protect-LogLine ([string]$_) } | Tee-Object -FilePath $out
