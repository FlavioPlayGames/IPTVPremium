param(
 [string]$Dispositivo,
 [ValidateRange(30,900)][int]$DuracaoSegundos=180,
 [string]$Arquivo=(Join-Path $env:USERPROFILE ('Downloads/validacao-capas-cache-rotativo-tv-{0}.txt' -f (Get-Date -Format 'yyyyMMdd-HHmmss')))
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
$info=& $adb -s $Dispositivo shell pm list packages -U br.com.flaviotorres.iptvpremium
if($LASTEXITCODE -ne 0){throw 'Falha ao consultar o aplicativo.'}
$iptvUid=[regex]::Match(($info -join "`n"),'uid:\s*(\d+)').Groups[1].Value
if(!$iptvUid){throw 'Aplicativo nao encontrado no aparelho selecionado.'}
$parent=Split-Path -Parent $Arquivo
if($parent){New-Item -ItemType Directory -Force -Path $parent | Out-Null}
$Arquivo=[IO.Path]::GetFullPath($Arquivo)
Write-Host "Captura por $DuracaoSegundos segundos. Navegue por filmes e series e abra detalhes. Nao atualize a lista nem limpe o cache. Mantenha o aplicativo visivel."
Write-Host "Arquivo: $Arquivo"
$logArgs=@('-s',$Dispositivo,'logcat','-b','main,system,crash','-v','threadtime',"--uid=$iptvUid",'-T','1')
$capture=Start-Process -FilePath $adb -ArgumentList $logArgs -RedirectStandardOutput $Arquivo -RedirectStandardError "$Arquivo.adb.txt" -NoNewWindow -PassThru
try {
 $until=(Get-Date).AddSeconds($DuracaoSegundos)
 while((Get-Date) -lt $until -and !$capture.HasExited){Start-Sleep -Seconds 1;$capture.Refresh()}
 if($capture.HasExited -and $capture.ExitCode -ne 0){throw "ADB encerrou a captura. Veja: $Arquivo.adb.txt"}
} finally {
 if(!$capture.HasExited){$capture.Kill();$capture.WaitForExit()}
}
Write-Host "Captura concluida: $Arquivo"
Write-Host 'Envie o arquivo capturado. O aplicativo continua funcionando; somente a captura foi encerrada.'
