#requires -Version 5.1
param(
 [string]$Dispositivo,
 [string]$AdbPath=(Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'),
 [string]$Pasta=(Join-Path $env:USERPROFILE 'Downloads/diagnostico-iptv'),
 [ValidatePattern('^[a-zA-Z0-9_.]+$')][string]$Pacote='br.com.flaviotorres.iptvpremium',
 [ValidateSet('Frio','Quente','NaoClassificado')][string]$Inicio='NaoClassificado',
 [ValidateSet('Importacao','Atualizacao','Navegacao')][string]$Operacao='Atualizacao',
 [ValidateRange(10,1800)][int]$DuracaoSegundos=180,
 [ValidateRange(1,10)][double]$IntervaloSegundos=2,
 [ValidatePattern('^[a-zA-Z0-9.-]*$')][string]$HostDns='',
 [switch]$Perfetto,[switch]$ReiniciarApp,[switch]$SoLogcat
)
. (Join-Path $PSScriptRoot 'Comum.ps1')
$script:Adb=$AdbPath
if(!(Test-Path -LiteralPath $script:Adb)){throw "ADB nao encontrado: $AdbPath"}
if(!$Dispositivo){
 $devices=& $AdbPath devices
 $available=@($devices | Where-Object {$_ -match '^\S+\s+device\s*$'} | ForEach-Object {($_ -split '\s+')[0]})
 if($available.Count -ne 1){throw 'Conecte apenas uma TV ou informe -Dispositivo com o identificador de adb devices.'}
 $Dispositivo=[string]$available[0]
}
$script:Device=$Dispositivo
$stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
$script:RunFolder=Join-Path $Pasta ($stamp+'-'+$Inicio+'-'+$Operacao+'-'+[guid]::NewGuid().ToString('N').Substring(0,6))
New-Item -ItemType Directory -Force -Path $script:RunFolder | Out-Null
$packageInfo=Save-Adb 'pacote' @('shell','dumpsys','package',$Pacote)
$uidInfo=Run-Adb @('shell','pm','list','packages','-U',$Pacote)
$uidMatch=[regex]::Match($uidInfo.stdout,('(?m)^package:'+ [regex]::Escape($Pacote)+'\s+uid:\s*(\d+)'))
if(!$uidMatch.Success){throw 'Aplicativo nao encontrado na TV.'};$appUid=$uidMatch.Groups[1].Value
$deviceInfo=Save-Adb 'aparelho' @('shell','getprop ro.product.manufacturer; getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.version.sdk; getprop ro.build.fingerprint; getprop ro.product.cpu.abilist; date +%Y-%m-%dT%H:%M:%S%z; cat /proc/sys/kernel/random/boot_id')
$logHelp=Run-Adb @('logcat','--help');$uidSupported=$logHelp.stdout.Contains('--uid') -or $logHelp.stderr.Contains('--uid')
$meta=[ordered]@{schema=1;hostStartUtc=[DateTime]::UtcNow.ToString('o');device=$Dispositivo;package=$Pacote;uid=$appUid;inicio=$Inicio;operacao=$Operacao;reiniciarApp=[bool]$ReiniciarApp;intervalTargetSeconds=$IntervaloSegundos;durationLimitSeconds=$DuracaoSegundos;uidLogSupported=$uidSupported;perfettoRequested=[bool]$Perfetto;logcatOnly=[bool]$SoLogcat;appBuild=([regex]::Match($packageInfo.stdout,'versionName=([^\s]+)').Groups[1].Value);deviceInfo=$deviceInfo.stdout;hostDns=$HostDns}
$versionCode=[regex]::Match($packageInfo.stdout,'versionCode=(\d+)').Groups[1].Value
$installTime=[regex]::Match($packageInfo.stdout,'lastUpdateTime=([^\r\n]+)').Groups[1].Value
$meta.appBuild=$meta.appBuild+' #'+$versionCode+' instalado '+$installTime
$meta | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $script:RunFolder 'execucao.json') -Encoding UTF8
$cleared=Save-Adb 'limpar-logcat' @('logcat','-b','all','-c')
$logArgs=@('logcat','-b','main,system,crash','-v','threadtime')
if($uidSupported){$logArgs+=('--uid='+$appUid)}
$appLog=Start-AdbLog 'logcat-app-bruto' $logArgs
$systemLog=Start-AdbLog 'logcat-sistema-bruto' @('logcat','-b','main,system,crash,events','-v','threadtime','-s','ActivityManager:I','Choreographer:W','AndroidRuntime:E','lowmemorykiller:I','lmkd:I','WindowManager:W','SQLiteLog:W','SQLiteConnectionPool:W','DEBUG:I','am_anr:I','am_kill:I','am_proc_died:I','IPTVMeasure:I')
$tracePid=$null;$traceKey='iptv-'+$stamp+'-'+[guid]::NewGuid().ToString('N').Substring(0,6);$traceRemote='/data/misc/perfetto-traces/'+$traceKey+'.perfetto-trace'
$traceConfigRemote='/data/local/tmp/'+$traceKey+'.pbtxt'
$traceLogRemote='/data/local/tmp/'+$traceKey+'-perfetto.log'
$started=[Diagnostics.Stopwatch]::StartNew();$nextSample=0.0;$nextTop=0.0;$nextDisk=0.0;$dnsChecked=$false;$sampleCount=0;$allPids=New-Object 'System.Collections.Generic.HashSet[string]'
try {
 if($Perfetto){
  $config=@"
buffers { size_kb: 8192 fill_policy: RING_BUFFER }
duration_ms: $($DuracaoSegundos*1000)
write_into_file: true
file_write_period_ms: 1000
max_file_size_bytes: 33500000
data_sources { config { name: "linux.ftrace" ftrace_config {
 ftrace_events: "sched/sched_switch"
 ftrace_events: "sched/sched_wakeup"
 ftrace_events: "power/cpu_frequency"
 ftrace_events: "block/block_rq_issue"
 ftrace_events: "block/block_rq_complete"
 atrace_categories: "am"
 atrace_categories: "gfx"
 atrace_categories: "view"
 atrace_categories: "dalvik"
 atrace_apps: "$Pacote"
}}}
data_sources { config { name: "linux.process_stats" process_stats_config { proc_stats_poll_ms: 2000 scan_all_processes_on_start: true record_thread_names: true }}}
data_sources { config { name: "linux.sys_stats" sys_stats_config { stat_period_ms: 1000 meminfo_period_ms: 1000 }}}
"@
  $localConfig=Join-Path $script:RunFolder 'perfetto.pbtxt';[IO.File]::WriteAllText($localConfig,$config,[Text.Encoding]::ASCII)
  [void](Save-Adb 'perfetto-push' @('push',$localConfig,$traceConfigRemote))
  $launch=Save-Adb 'perfetto-inicio' @('shell',"perfetto --txt -c $traceConfigRemote -o $traceRemote --detach=$traceKey >$traceLogRemote 2>&1; cat $traceLogRemote")
  $pidMatch=[regex]::Match($launch.stdout,'(?im)(?:^\s*|\bPID[=: ]+)(\d+)\s*$')
  if($pidMatch.Success){$tracePid=$pidMatch.Groups[1].Value}
 }
 [void](Save-Adb 'cpu-inicio' @('shell','dumpsys','cpuinfo'))
 [void](Save-Adb 'gfx-reset' @('shell','dumpsys','gfxinfo',$Pacote,'reset'))
 [void](Save-Adb 'netstats-inicio' @('shell','dumpsys','netstats','detail') 15000)
 if($ReiniciarApp){
  [void](Run-Adb @('shell','am','force-stop',$Pacote))
  [void](Save-Adb 'abrir-app' @('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.LEANBACK_LAUNCHER','-p',$Pacote))
 }
 Write-Host "Salvando em: $script:RunFolder"
 Write-Host 'Faca UMA importacao/atualizacao. I marca o clique; V marca a tela visivel; Q encerra com relatorio.'
 Write-Host "Amostras alvo: ${IntervaloSegundos}s; duracao maxima: ${DuracaoSegundos}s. Nao limpe cache/dados."
 $calibrationUtc=[DateTime]::UtcNow.ToString('o')
 $calibration=Run-Adb @('shell','log','-p','i','-t','IPTVMeasure',('CALIBRATION hostUtc='+$calibrationUtc))
 $meta.calibrationRoundTripMs=$calibration.ms
 $started.Restart()
 while($started.Elapsed.TotalSeconds -lt $DuracaoSegundos){
  try {
   if(![Console]::IsInputRedirected -and [Console]::KeyAvailable){
    $key=[Console]::ReadKey($true).Key.ToString()
    if($key -eq 'Q'){break}
    if($key -eq 'I' -or $key -eq 'V'){
     $mark=if($key -eq 'I'){'USER_IMPORT_CLICK'}else{'USER_CATALOG_VISIBLE'}
     $r=Run-Adb @('shell','log','-p','i','-t','IPTVMeasure',$mark)
     @{utc=[DateTime]::UtcNow.ToString('o');mark=$mark;deviceLogExit=$r.exit} | ConvertTo-Json -Compress | Add-Content (Join-Path $script:RunFolder 'marcas.jsonl') -Encoding UTF8
     Write-Host "Marco: $mark"
    }
   }
  } catch [InvalidOperationException] { }
  if($SoLogcat){Start-Sleep -Milliseconds 100;continue}
  if($started.Elapsed.TotalSeconds -lt $nextSample){Start-Sleep -Milliseconds 100;continue}
  $at=[DateTime]::UtcNow.ToString('o');$cycle=[Diagnostics.Stopwatch]::StartNew()
  $pidResult=Run-Adb @('shell','pidof',$Pacote) 3000
  $appPids=@([regex]::Matches($pidResult.stdout,'\b\d+\b') | ForEach-Object {$_.Value})
  $processList=Run-Adb @('shell','ps','-A','-o','PID,ARGS') 3000
  foreach($match in [regex]::Matches($processList.stdout,('(?m)^\s*(\d+)\s+'+[regex]::Escape($Pacote)+'(?::[a-zA-Z0-9_.-]+)?(?:\s|$)'))){$appPids+=$match.Groups[1].Value}
  $appPids=@($appPids | Select-Object -Unique)
  foreach($appPid in $appPids){[void]$allPids.Add($appPid)}
  $sample=[ordered]@{utc=$at;elapsedSeconds=$started.Elapsed.TotalSeconds;pids=$appPids;pidLookup=$pidResult;processList=$processList;processes=@()}
  $sample.system=Run-Adb @('shell','echo __MEMINFO__; cat /proc/meminfo; echo __STAT__; cat /proc/stat; echo __NETDEV__; cat /proc/net/dev; echo __UPTIME__; cat /proc/uptime') 5000
  foreach($appPid in $appPids){
   $mem=Run-Adb @('shell','dumpsys','meminfo',$appPid) 5000
   $procScript='echo __IO__; cat /proc/'+$appPid+'/io; echo __PSTAT__; cat /proc/'+$appPid+'/stat; echo __STATUS__; cat /proc/'+$appPid+'/status'
   $proc=Run-Adb @('shell',$procScript) 3000
   if($proc.stdout -notmatch '(?m)^read_bytes:'){
    $proc=Run-Adb @('shell',('run-as '+$Pacote+' sh -c '+"'"+$procScript+"'")) 3000
   }
   $sample.processes+=@{pid=$appPid;meminfo=$mem;proc=$proc}
   if($started.Elapsed.TotalSeconds -ge $nextTop){[void](Save-Adb ("threads-"+$sampleCount+'-'+$appPid) @('shell','top','-H','-b','-n','1','-p',$appPid) 5000)}
  }
  if($started.Elapsed.TotalSeconds -ge $nextTop){$nextTop=$started.Elapsed.TotalSeconds+6}
  if($started.Elapsed.TotalSeconds -ge $nextDisk){
   $diskScript=@'
run-as __PKG__ sh -c 'for f in databases/* cache/*.tmp files/*.tmp; do if [ -f "$f" ]; then stat -c "%n|%s" "$f"; fi; done; df -k .'
'@
   $sample.disk=Run-Adb @('shell',$diskScript.Replace('__PKG__',$Pacote)) 5000
   $nextDisk=$started.Elapsed.TotalSeconds+6
  }
  $sample.durationMs=$cycle.ElapsedMilliseconds
  $sample | ConvertTo-Json -Depth 12 -Compress | Add-Content (Join-Path $script:RunFolder 'amostras.jsonl') -Encoding UTF8
  $sampleCount++;$nextSample=[Math]::Max($sample.elapsedSeconds+$IntervaloSegundos,$started.Elapsed.TotalSeconds+0.1)
  if($HostDns -and !$dnsChecked){
   $tail=(Get-Content (Join-Path $script:RunFolder 'logcat-app-bruto.txt') -Tail 100 -ErrorAction SilentlyContinue) -join "`n"
   if($tail -match 'Unable to resolve host|UnknownHostException'){
    [void](Save-Adb 'dns-na-falha' @('shell',"getent hosts $HostDns; toybox nslookup $HostDns; ping -c 1 -W 1 $HostDns") 5000);$dnsChecked=$true
   }
  }
 }
} finally {
 foreach($p in @($appLog,$systemLog)){if($p -and !$p.HasExited){try{$p.Kill();[void]$p.WaitForExit(2000)}catch{}}}
 [void](Save-Adb 'cpu-fim' @('shell','dumpsys','cpuinfo'))
 [void](Save-Adb 'gfx-fim' @('shell','dumpsys','gfxinfo',$Pacote,'framestats'))
 [void](Save-Adb 'netstats-fim' @('shell','dumpsys','netstats','detail') 15000)
 $filesScript=@'
run-as __PKG__ sh -c 'find databases -type f -exec stat -c "%n|%s" {} \;; command -v sqlite3; if command -v sqlite3 >/dev/null; then sqlite3 -readonly databases/playlist_cache.db "PRAGMA user_version; PRAGMA page_count; PRAGMA page_size; PRAGMA freelist_count; SELECT name,sum(pgsize) FROM dbstat GROUP BY name;"; fi'
'@
 [void](Save-Adb 'banco-final' @('shell',$filesScript.Replace('__PKG__',$Pacote)) 10000)
 if($HostDns -and !$dnsChecked){[void](Save-Adb 'dns-final' @('shell',"getent hosts $HostDns; toybox nslookup $HostDns; ping -c 1 -W 1 $HostDns") 5000)}
 if($Perfetto){
  $stopped=Save-Adb 'perfetto-stop-session' @('shell','perfetto',('--attach='+$traceKey),'--stop') 7000
  if($stopped.exit -ne 0 -and $tracePid){[void](Save-Adb 'perfetto-parar' @('shell','kill','-INT',$tracePid))}
  Start-Sleep -Milliseconds 1500
  [void](Save-Adb 'perfetto-status' @('shell',"cat $traceLogRemote; ls -l $traceRemote"))
  $pulled=Save-Adb 'perfetto-pull' @('pull',$traceRemote,(Join-Path $script:RunFolder 'importacao.perfetto-trace')) 30000
  if($pulled.exit -eq 0){[void](Run-Adb @('shell',"rm -f $traceConfigRemote $traceLogRemote $traceRemote"))}
 }
 $tracePath=Join-Path $script:RunFolder 'importacao.perfetto-trace'
 $meta.perfettoCollected=$false;$meta.perfettoFileBytes=$null
 if(Test-Path -LiteralPath $tracePath){$meta.perfettoFileBytes=(Get-Item -LiteralPath $tracePath).Length;$meta.perfettoCollected=($meta.perfettoFileBytes -gt 0)}
 $meta.hostEndUtc=[DateTime]::UtcNow.ToString('o');$meta.sampleCount=$sampleCount;$meta.observedPids=@($allPids | ForEach-Object {$_});$meta.elapsedSeconds=$started.Elapsed.TotalSeconds
 $meta | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $script:RunFolder 'execucao.json') -Encoding UTF8
 & (Join-Path $PSScriptRoot 'Relatorio.ps1') -Pasta $script:RunFolder
 $zip=$script:RunFolder+'.zip'
 Compress-Archive -Path (Join-Path $script:RunFolder '*') -DestinationPath $zip -Force
 Write-Host "Concluido: $zip"
}
