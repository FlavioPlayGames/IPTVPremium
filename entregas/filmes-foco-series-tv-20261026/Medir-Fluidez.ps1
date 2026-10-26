param(
 [string]$Dispositivo,
 [ValidateRange(60,1800)][int]$DuracaoSegundos=900,
 [ValidateRange(10,60)][int]$IntervaloSegundos=15,
 [string]$Pasta=(Join-Path $env:USERPROFILE ('Downloads/fluidez-memoria-{0}' -f (Get-Date -Format 'yyyyMMdd-HHmmss')))
)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$package='br.com.flaviotorres.iptvpremium'
if(!(Test-Path -LiteralPath $adb)){throw 'ADB nao encontrado no Android SDK padrao.'}
if(!$Dispositivo){
 $devices=& $adb devices
 if($LASTEXITCODE-ne 0){throw 'Falha ao consultar dispositivos.'}
 $connected=@($devices|Where-Object{$_-match '^\S+\s+device\s*$'}|ForEach-Object{($_-split '\s+')[0]})
 if($connected.Count-ne 1){throw 'Conecte apenas a TV ou informe -Dispositivo.'}
 $Dispositivo=[string]$connected[0]
}
$info=& $adb -s $Dispositivo shell pm list packages -U $package
if($LASTEXITCODE-ne 0){throw 'Falha ao consultar aplicativo.'}
$appUid=[regex]::Match(($info-join "`n"),'uid:\s*(\d+)').Groups[1].Value
if(!$appUid){throw 'Aplicativo nao encontrado.'}
New-Item -ItemType Directory -Force -Path $Pasta|Out-Null
$Pasta=[IO.Path]::GetFullPath($Pasta)
$log=Join-Path $Pasta 'logcat.txt';$rawMemory=Join-Path $Pasta 'memoria-bruta.txt';$csv=Join-Path $Pasta 'amostras.csv'
$metadata=[ordered]@{package=$package;startedAt=(Get-Date -Format 'o');durationSeconds=$DuracaoSegundos;sampleIntervalSeconds=$IntervaloSegundos;model=((& $adb -s $Dispositivo shell getprop ro.product.model)-join '').Trim();android=((& $adb -s $Dispositivo shell getprop ro.build.version.release)-join '').Trim();uid=$appUid}
$metadata|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $Pasta 'aparelho.json') -Encoding UTF8
& $adb -s $Dispositivo shell dumpsys cpuinfo | Set-Content -LiteralPath (Join-Path $Pasta 'cpu-inicio.txt') -Encoding UTF8
# Reset only diagnostic frame counters; does not clear data, cache, logcat or stop the app.
& $adb -s $Dispositivo shell dumpsys gfxinfo $package reset | Set-Content -LiteralPath (Join-Path $Pasta 'frames-inicio.txt') -Encoding UTF8
$process=Start-Process -FilePath $adb -ArgumentList @('-s',$Dispositivo,'logcat','-b','main,system,crash','-v','threadtime',"--uid=$appUid",'-T','1') -RedirectStandardOutput $log -RedirectStandardError (Join-Path $Pasta 'adb-logcat.txt') -NoNewWindow -PassThru
$samples=[Collections.Generic.List[object]]::new();$watch=[Diagnostics.Stopwatch]::StartNew()
Write-Host "Captura por $DuracaoSegundos segundos. Pasta: $Pasta"
Write-Host 'Medicao em andamento: navegue em Filmes, com movimentos lentos e rapidos, e volte a algumas fileiras anteriores. Nao importe, entre em Kids/player nem limpe cache. Mantenha o app visivel na TV.'
function Match-Number([string]$Text,[string]$Pattern){$m=[regex]::Match($Text,$Pattern);if($m.Success){return [long]$m.Groups[1].Value};return $null}
try{
 while($watch.Elapsed.TotalSeconds-lt $DuracaoSegundos){
  $at=Get-Date;$sampleStart=$watch.ElapsedMilliseconds
  $pidText=(& $adb -s $Dispositivo shell pidof $package)-join ' '
  $memLines=& $adb -s $Dispositivo shell dumpsys meminfo $package
  $memOk=($LASTEXITCODE-eq 0);$mem=$memLines-join "`n"
  $systemLines=& $adb -s $Dispositivo shell cat /proc/meminfo
  $systemOk=($LASTEXITCODE-eq 0);$system=$systemLines-join "`n"
  Add-Content -LiteralPath $rawMemory -Encoding UTF8 -Value ("`n=== "+$at.ToString('o')+" elapsedMs="+$sampleStart+" pid="+$pidText.Trim()+" ===`n"+$mem+"`nSYSTEM`n"+$system)
  $pss=Match-Number $mem '(?m)^\s*TOTAL PSS:\s*(\d+)'
  if($null-eq $pss){$pss=Match-Number $mem '(?m)^\s*TOTAL\s+(\d+)'}
  $sample=[pscustomobject]@{timestamp=$at.ToString('o');elapsedMs=$sampleStart;pid=$pidText.Trim();meminfoOk=$memOk;systemOk=$systemOk;pssKb=$pss;javaPssKb=(Match-Number $mem '(?m)^\s*Java Heap:\s*(\d+)');nativePssKb=(Match-Number $mem '(?m)^\s*Native Heap:\s*(\d+)');graphicsPssKb=(Match-Number $mem '(?m)^\s*Graphics:\s*(\d+)');systemAvailableKb=(Match-Number $system '(?m)^MemAvailable:\s*(\d+)');sampleDurationMs=($watch.ElapsedMilliseconds-$sampleStart)}
  $samples.Add($sample);$sample|Export-Csv -LiteralPath $csv -NoTypeInformation -Encoding UTF8 -Append
  $process.Refresh();if($process.HasExited){throw 'A captura logcat encerrou antes do previsto. Consulte adb-logcat.txt.'}
  $next=$sampleStart+($IntervaloSegundos*1000)
  while($watch.ElapsedMilliseconds-lt $next-and $watch.Elapsed.TotalSeconds-lt $DuracaoSegundos){Start-Sleep -Milliseconds 250}
 }
}finally{
 if(!$process.HasExited){$process.Kill();$process.WaitForExit()}
}
& $adb -s $Dispositivo shell dumpsys gfxinfo $package | Set-Content -LiteralPath (Join-Path $Pasta 'frames-final.txt') -Encoding UTF8
& $adb -s $Dispositivo shell dumpsys cpuinfo | Set-Content -LiteralPath (Join-Path $Pasta 'cpu-final.txt') -Encoding UTF8
function Peak([string]$Property){
 $valid=@($samples|Where-Object{$null-ne $_.$Property})
 if(!$valid.Count){return $null}
 $best=$valid|Sort-Object -Property $Property -Descending|Select-Object -First 1
 return [ordered]@{kb=$best.$Property;timestamp=$best.timestamp;elapsedMs=$best.elapsedMs}
}
$text=if(Test-Path -LiteralPath $log){[IO.File]::ReadAllText($log)}else{''}
$summary=[ordered]@{samples=$samples.Count;elapsedSeconds=[math]::Round($watch.Elapsed.TotalSeconds,1);peaks=[ordered]@{pss=(Peak 'pssKb');java=(Peak 'javaPssKb');native=(Peak 'nativePssKb');graphics=(Peak 'graphicsPssKb')};distinctPids=@($samples|ForEach-Object{$_.pid}|Where-Object{$_}|Select-Object -Unique);minimumSystemAvailableKb=(@($samples|Where-Object{$null-ne $_.systemAvailableKb}|Measure-Object -Property systemAvailableKb -Minimum).Minimum);alerts=[ordered]@{fatalExceptions=[regex]::Matches($text,'FATAL EXCEPTION').Count;oomMessages=[regex]::Matches($text,'OutOfMemoryError').Count;skippedFrameWarnings=[regex]::Matches($text,'Skipped \d+ frames').Count;cacheBudgetRefusals=[regex]::Matches($text,'outcome=cache_budget').Count;gcEvents=[regex]::Matches($text,'GC freed').Count};note='PSS de Java/Native/Graphics nao equivale a limite de heap nem armazenamento em disco. Picos amostrados podem perder eventos entre amostras. Avisos ausentes nao comprovam fluidez. frames-final.txt preserva dados de renderizacao; logcat inclui etapas do app. Captura e consultas ADB podem acrescentar alguma carga.'}
$summary|ConvertTo-Json -Depth 7|Set-Content -LiteralPath (Join-Path $Pasta 'resumo.json') -Encoding UTF8
Compress-Archive -Path (Join-Path $Pasta '*') -DestinationPath ($Pasta+'.zip') -Force
Write-Host ('Concluido. Envie: '+$Pasta+'.zip')
