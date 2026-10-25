#requires -Version 5.1
param([Parameter(Mandatory=$true)][string]$Pasta)
. (Join-Path $PSScriptRoot 'Comum.ps1')
$meta=Read-Json (Join-Path $Pasta 'execucao.json')
$appFile=Join-Path $Pasta 'logcat-app-bruto.txt';$sysFile=Join-Path $Pasta 'logcat-sistema-bruto.txt'
$appLines=@();$sysLines=@();$samples=@()
if(Test-Path $appFile){$appLines=@([IO.File]::ReadAllLines($appFile))}
if(Test-Path $sysFile){$sysLines=@([IO.File]::ReadAllLines($sysFile))}
if(Test-Path (Join-Path $Pasta 'amostras.jsonl')){foreach($line in [IO.File]::ReadLines((Join-Path $Pasta 'amostras.jsonl'))){try{$samples+=ConvertFrom-Json -InputObject $line.TrimStart([char]0xFEFF)}catch{}}}
$year=(Get-Date).Year
$yearMatch=[regex]::Match([string]$meta.deviceInfo,'\b(20\d{2})-\d{2}-\d{2}T')
if($yearMatch.Success){$year=[int]$yearMatch.Groups[1].Value}
function Log-Time([string]$Line){
 $m=[regex]::Match($Line,'^(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})')
 if($m.Success){return [DateTime]::ParseExact(($year.ToString()+'-'+$m.Groups[1].Value),'yyyy-MM-dd HH:mm:ss.fff',[Globalization.CultureInfo]::InvariantCulture)}
 return $null
}
function Log-Pid([string]$Line){$m=[regex]::Match($Line,'^\d{2}-\d{2} \S+\s+(\d+)\s+\d+\s+[VDIWEF]\s');if($m.Success){return $m.Groups[1].Value};return ''}
if(!$meta.uidLogSupported){$appLines=@($appLines | Where-Object {$meta.observedPids -contains (Log-Pid $_)})}
$appLines | Set-Content (Join-Path $Pasta 'logcat-app-processos.txt') -Encoding UTF8
$stagePattern='StartupPerf|M3uImportPerf|XtreamEndpointPerf|XtreamDirectPerf|XtreamWritePerf|M3uSourceSync|PlaylistConnection|CacheGeneration|DbPerf|StorageMaintenancePerf|OnDemandCatalogPerf|CatalogViewportPerf|PosterLoadPerf|IPTVMeasure'
@($appLines+$sysLines | Where-Object {$_ -match $stagePattern}) | Set-Content (Join-Path $Pasta 'etapas.txt') -Encoding UTF8
$signals='GC freed|concurrent copying|paused |Unable to resolve host|UnknownHostException|[Tt]imeout|[Ee]xception|FATAL EXCEPTION|ANR|am_anr|OutOfMemory|onTrimMemory|Skipped \d+ frames|database is locked|SQLite.*lock|long.*transaction|lowmemorykiller|lmkd|am_kill|am_proc_died'
@($appLines+$sysLines | Where-Object {$_ -match $signals -or $_ -match '^\s+at\s'}) | Set-Content (Join-Path $Pasta 'eventos-e-erros.txt') -Encoding UTF8
$offset=$null
foreach($line in $sysLines){
 if($line -match 'CALIBRATION hostUtc=(\S+)'){
  $hostAt=[DateTimeOffset]::Parse($Matches[1]).UtcDateTime;$deviceAt=Log-Time $line
  if($deviceAt){$offset=$deviceAt-$hostAt;break}
 }
}
function Section([string]$Text,[string]$Start,[string]$End){
 $a=$Text.IndexOf($Start);if($a -lt 0){return ''};$a+=$Start.Length
 $b=if($End){$Text.IndexOf($End,$a)}else{-1};if($b -lt 0){$b=$Text.Length};return $Text.Substring($a,$b-$a)
}
function Match-Number([string]$Text,[string]$Pattern){$m=[regex]::Match($Text,$Pattern);if($m.Success){return [double]::Parse($m.Groups[1].Value,[Globalization.CultureInfo]::InvariantCulture)};return $null}
function Heap-Values([string]$Text){
 $java=Match-Number $Text '(?m)^\s*Java Heap:\s*(\d+)';if($null -eq $java){$java=Match-Number $Text '(?m)^\s*Dalvik Heap\s+(\d+)'}
 $native=Match-Number $Text '(?m)^\s*Native Heap:\s*(\d+)';if($null -eq $native){$native=Match-Number $Text '(?m)^\s*Native Heap\s+(\d+)'}
 $gfx=Match-Number $Text '(?m)^\s*Graphics:\s*(\d+)'
 $total=Match-Number $Text 'TOTAL PSS:\s*(\d+)';if($null -eq $total){$total=Match-Number $Text '(?m)^\s*TOTAL\s+(\d+)'}
 $allocJava=$null;$allocNative=$null
 foreach($type in @('Dalvik','Native')){
  $m=[regex]::Match($Text,('(?m)^\s*'+$type+' Heap\s+([^\r\n]+)'))
  if($m.Success){$n=@([regex]::Matches($m.Groups[1].Value,'\d+') | ForEach-Object {[double]$_.Value});if($n.Count -ge 8){if($type -eq 'Dalvik'){$allocJava=$n[-2]}else{$allocNative=$n[-2]}}}
 }
 [pscustomobject]@{javaPssKiB=$java;nativePssKiB=$native;graphicsPssKiB=$gfx;totalPssKiB=$total;javaAllocatedKiB=$allocJava;nativeAllocatedKiB=$allocNative}
}
$normalized=@();$cpuBefore=$null;$cpuObserved=@();$diskObserved=@();$procBefore=@{}
foreach($sample in $samples){
 $deviceAt=$null;if($null -ne $offset){$deviceAt=[DateTime]::SpecifyKind(([DateTimeOffset]::Parse($sample.utc).UtcDateTime+$offset),[DateTimeKind]::Unspecified)}
 $cores=@{}
 foreach($m in [regex]::Matches($sample.system.stdout,'(?m)^(cpu\d*|cpu)\s+([0-9 ]+)')){
  $values=@($m.Groups[2].Value.Trim() -split '\s+' | ForEach-Object {[double]$_})
  if($values.Count -ge 5){$total=0.0;for($j=0;$j -lt [Math]::Min(8,$values.Count);$j++){$total+=$values[$j]};$cores[$m.Groups[1].Value]=@{total=$total;idle=$values[3];iowait=$values[4]}}
 }
 if($cpuBefore){foreach($core in $cores.Keys){if($cpuBefore.ContainsKey($core)){$delta=$cores[$core].total-$cpuBefore[$core].total;if($delta -gt 0){$cpuObserved+=[pscustomobject]@{utc=$sample.utc;core=$core;busyPercent=[Math]::Round(100*(1-(($cores[$core].idle-$cpuBefore[$core].idle)+($cores[$core].iowait-$cpuBefore[$core].iowait))/$delta),2);ioWaitPercent=[Math]::Round(100*($cores[$core].iowait-$cpuBefore[$core].iowait)/$delta,2)}}}}}
 $systemDelta=$null
 if($cpuBefore -and $cpuBefore.ContainsKey('cpu') -and $cores.ContainsKey('cpu')){$systemDelta=$cores.cpu.total-$cpuBefore.cpu.total}
 $coreCount=@($cores.Keys | Where-Object {$_ -ne 'cpu'}).Count
 $cpuBefore=$cores
 $available=Match-Number $sample.system.stdout '(?m)^MemAvailable:\s*(\d+)'
 foreach($process in $sample.processes){
  $heap=Heap-Values $process.meminfo.stdout
  $processAt=$null;if($null -ne $offset){$processAt=[DateTime]::SpecifyKind(([DateTimeOffset]::Parse($process.meminfo.utc).UtcDateTime+$offset),[DateTimeKind]::Unspecified)}
  $processCpu=$null
  $stat=Section $process.proc.stdout '__PSTAT__' '__STATUS__'
  $close=$stat.LastIndexOf(')')
  if($close -gt 0){$parts=@($stat.Substring($close+1).Trim() -split '\s+');if($parts.Count -ge 13){
   $ticks=[double]$parts[11]+[double]$parts[12];$previous=$procBefore[[string]$process.pid]
   if($null -ne $previous -and $systemDelta -gt 0 -and $coreCount -gt 0){$processCpu=[Math]::Round([Math]::Max(0,100*($ticks-$previous)/$systemDelta*$coreCount),2)}
   $procBefore[[string]$process.pid]=$ticks
  }}
  $normalized+=[pscustomobject]@{batchUtc=$sample.utc;utc=$process.meminfo.utc;deviceAt=$processAt;ioUtc=$process.proc.utc;pid=[string]$process.pid;javaPssKiB=$heap.javaPssKiB;nativePssKiB=$heap.nativePssKiB;graphicsPssKiB=$heap.graphicsPssKiB;totalPssKiB=$heap.totalPssKiB;javaAllocatedKiB=$heap.javaAllocatedKiB;nativeAllocatedKiB=$heap.nativeAllocatedKiB;memAvailableKiB=$available;readBytes=(Match-Number $process.proc.stdout '(?m)^read_bytes:\s*(\d+)');writeBytes=(Match-Number $process.proc.stdout '(?m)^write_bytes:\s*(\d+)');syscw=(Match-Number $process.proc.stdout '(?m)^syscw:\s*(\d+)');processCpuOneCorePercent=$processCpu;sampleMs=$sample.durationMs}
 }
 if($sample.disk){foreach($m in [regex]::Matches($sample.disk.stdout,'(?m)^([^\r\n|]+)\|(\d+)\s*$')){$diskObserved+=[pscustomobject]@{utc=$sample.utc;deviceAt=$deviceAt;file=$m.Groups[1].Value;bytes=[double]$m.Groups[2].Value}}}
}
$appTotals=@()
foreach($group in @($normalized | Group-Object batchUtc)){
 $g=@($group.Group | Sort-Object utc);$last=$g[-1]
 $row=[ordered]@{utc=$last.utc;deviceAt=$last.deviceAt;pid=($g.pid -join '+');processCount=$g.Count}
 foreach($field in @('javaPssKiB','nativePssKiB','graphicsPssKiB','totalPssKiB','javaAllocatedKiB','nativeAllocatedKiB')){
  $reported=@($g | Where-Object {$null -ne $_.$field})
  $row[$field]=$null;if($reported.Count -eq $g.Count){$row[$field]=($reported | ForEach-Object {$_.$field} | Measure-Object -Sum).Sum}
 }
 $appTotals+=[pscustomobject]$row
}
$appTotals | Export-Csv (Join-Path $Pasta 'memoria-total-app.csv') -NoTypeInformation -Encoding UTF8
$normalized | Export-Csv (Join-Path $Pasta 'memoria-io.csv') -NoTypeInformation -Encoding UTF8
$cpuObserved | Export-Csv (Join-Path $Pasta 'cpu-nucleos.csv') -NoTypeInformation -Encoding UTF8
$diskObserved | Export-Csv (Join-Path $Pasta 'banco-wal.csv') -NoTypeInformation -Encoding UTF8
function Peak($Rows,[string]$Field){
 $p=@($Rows | Where-Object {$null -ne $_.$Field} | Sort-Object -Property $Field -Descending | Select-Object -First 1)
 if($p.Count){return @{value=$p[0].$Field;utc=$p[0].utc;pid=$p[0].pid}}
 return $null
}
function Io-Delta($Rows,[string]$Field){
 $sum=0.0;$seen=$false
 foreach($group in @($Rows | Group-Object pid)){
  $v=@($group.Group | Where-Object {$null -ne $_.$Field} | Sort-Object utc)
  if($v.Count -ge 2){$sum+=[Math]::Max(0,$v[-1].$Field-$v[0].$Field);$seen=$true}
 }
 if($seen){return $sum};return $null
}
function GC-Stats($Lines){
 $gc=@($Lines | Where-Object {$_ -match 'GC freed|concurrent copying.*freed|GC.*freed'})
 $pause=0.0;$recognized=0
 foreach($line in $gc){
  $p=[regex]::Match($line,'paused\s+(.*?)\s+total')
  if($p.Success){$recognized++;foreach($m in [regex]::Matches($p.Groups[1].Value,'([0-9.]+)\s*(ms|us|µs|ns|s)')){
   $v=[double]::Parse($m.Groups[1].Value,[Globalization.CultureInfo]::InvariantCulture)
   switch($m.Groups[2].Value){'us'{$v/=1000};'µs'{$v/=1000};'ns'{$v/=1000000};'s'{$v*=1000}}
   $pause+=$v
  }}
 }
 @{events=$gc.Count;recognizedPauses=$recognized;pauseMs=if(!$gc.Count){0}elseif($recognized){[Math]::Round($pause,3)}else{$null}}
}
$threadRows=@()
foreach($file in Get-ChildItem -LiteralPath $Pasta -Filter 'threads-*.json'){
 $data=Read-Json $file.FullName;$lines=@($data.stdout -split "`r?`n");$header=$null
 foreach($line in $lines){
  if($line -match '^\s*(PID|TID)\s+'){$header=@(($line.Replace('[',' ').Replace(']',' ').Trim()) -split '\s+');continue}
  if(!$header -or $line -notmatch '^\s*\d+\s+'){continue}
  $v=@($line.Trim() -split '\s+');$cpuIndex=[Array]::IndexOf($header,'%CPU');if($cpuIndex -lt 0){$cpuIndex=[Array]::IndexOf($header,'CPU%')}
  $idIndex=[Array]::IndexOf($header,'TID');if($idIndex -lt 0){$idIndex=0}
  $nameIndex=[Array]::IndexOf($header,'ARGS');if($nameIndex -lt 0){$nameIndex=[Array]::IndexOf($header,'NAME')};if($nameIndex -lt 0){$nameIndex=$header.Count-1}
  $timeIndex=[Array]::IndexOf($header,'TIME+')
  if($cpuIndex -ge 0 -and $v.Count -gt [Math]::Max($cpuIndex,$nameIndex) -and $v[$cpuIndex] -match '^[0-9.]+$'){
   $cpuTime=$null
   if($timeIndex -ge 0 -and $v.Count -gt $timeIndex){$m=[regex]::Match($v[$timeIndex],'^(?:(\d+):)?(\d+):(\d+(?:\.\d+)?)$');if($m.Success){$h=0;if($m.Groups[1].Success){$h=[double]$m.Groups[1].Value};$cpuTime=3600*$h+60*[double]$m.Groups[2].Value+[double]::Parse($m.Groups[3].Value,[Globalization.CultureInfo]::InvariantCulture)}}
   $threadRows+=[pscustomobject]@{utc=$data.utc;source=$file.Name;tid=$v[$idIndex];name=($v[$nameIndex..($v.Count-1)] -join ' ');cpuPercent=[double]::Parse($v[$cpuIndex],[Globalization.CultureInfo]::InvariantCulture);cpuSeconds=$cpuTime}
  }
 }
}
$threadRank=@($threadRows | Group-Object tid,name | ForEach-Object {
 $g=@($_.Group | Sort-Object utc);$delta=$null;if($g.Count -ge 2 -and $null -ne $g[0].cpuSeconds -and $null -ne $g[-1].cpuSeconds){$delta=[Math]::Max(0,$g[-1].cpuSeconds-$g[0].cpuSeconds)}
 [pscustomobject]@{thread=$_.Name;observations=$g.Count;peakPercent=($g.cpuPercent | Measure-Object -Maximum).Maximum;meanPercent=($g.cpuPercent | Measure-Object -Average).Average;cpuSecondsObserved=$delta;firstUtc=$g[0].utc;lastUtc=$g[-1].utc}
} | Sort-Object peakPercent -Descending)
$threadRows | Export-Csv (Join-Path $Pasta 'threads-amostras.csv') -NoTypeInformation -Encoding UTF8
$threadRank | Export-Csv (Join-Path $Pasta 'threads-ranking.csv') -NoTypeInformation -Encoding UTF8
$sysTimed=@($sysLines | ForEach-Object {$at=Log-Time $_;if($at){[pscustomobject]@{at=$at;line=$_}}})
$appTimed=@($appLines | ForEach-Object {$at=Log-Time $_;if($at){[pscustomobject]@{at=$at;line=$_}}})
$syncRows=@($appTimed | Where-Object {$_.line -match 'SYNC_RESUMO tipo='})
$runs=@();$previousEnd=$null;$index=0
foreach($sync in $syncRows){
 $index++;$line=$sync.line;$syncMs=Number-Field $line 'duracaoMs'
 if($null -eq $syncMs){continue}
 $start=$sync.at.AddMilliseconds(-$syncMs);$end=$sync.at
 $inWindow=@($appTimed | Where-Object {$_.at -ge $start.AddMilliseconds(-100) -and $_.at -le $end.AddMilliseconds(100)})
 $before=@($appTimed | Where-Object {$_.at -le $start -and (!$previousEnd -or $_.at -gt $previousEnd)})
 $click=@($sysTimed | Where-Object {$_.line -match 'USER_IMPORT_CLICK' -and $_.at -le $start -and (!$previousEnd -or $_.at -gt $previousEnd)} | Select-Object -Last 1)
 $startSource='SYNC_RESUMO menos duracaoMs; nao e o clique'
 $clickAt=$null
 if($click.Count){$clickAt=$click[0].at;$startSource='Marco manual I (aproximacao do clique)'}
 else {$c=@($before | Where-Object {$_.line -match 'StartupPerf.*etapa=selecao_catalogo'} | Select-Object -Last 1);if($c.Count){$clickAt=$c[0].at;$startSource='StartupPerf selecao_catalogo (proxy do clique)'}}
 $nextStart=@($syncRows | Where-Object {$_.at -gt $end} | Select-Object -First 1)
 $nextLimit=$end.AddSeconds(60);if($nextStart.Count){$nextLimit=$nextStart[0].at.AddMilliseconds(-(Number-Field $nextStart[0].line 'duracaoMs'))}
 $visible=@($sysTimed | Where-Object {$_.line -match 'USER_CATALOG_VISIBLE' -and $_.at -ge $end -and $_.at -lt $nextLimit} | Select-Object -First 1)
 $visibleAt=$null;$visibleSource='Nao registrado'
 if($visible.Count){$visibleAt=$visible[0].at;$visibleSource='Marco manual V'}
 else {$v=@($appTimed | Where-Object {$_.line -match 'StorageMaintenancePerf.*phase=signal reason=home_ready' -and $_.at -ge $end -and $_.at -lt $nextLimit} | Select-Object -First 1);if($v.Count){$visibleAt=$v[0].at;$visibleSource='home_ready (proxy da Home, nao prova grade desenhada)'}}
 $totalObserved=$null;if($clickAt -and $visibleAt){$totalObserved=($visibleAt-$clickAt).TotalMilliseconds}
 $post=$null;if($visibleAt){$post=($visibleAt-$end).TotalMilliseconds}
 $overlap=$false;$stages=@();$write=$null;$entries=$null;$parse=$null;$bodyBytes=0.0;$hasBytes=$false
 $m3u=@($inWindow | Where-Object {$_.line -match 'M3U processada:'} | Select-Object -Last 1)
 if($m3u.Count){
  $write=Number-Field $m3u[0].line 'gravacaoRoomMs';$aggregate=Number-Field $m3u[0].line 'redeEParsingMs';$entries=Number-Field $m3u[0].line 'entradas';$processed=Number-Field $m3u[0].line 'tempoMs'
  if($null -ne $aggregate){$stages+=@{name='Rede + parsing (agregados)';ms=$aggregate}}
  if($null -ne $write){$stages+=@{name='Transacoes Room (incluem indices)';ms=$write}}
  if($null -ne $processed){$stages+=@{name='Outros dentro da sincronizacao';ms=[Math]::Max(0,$syncMs-$processed)}}
 } else {
  $endpoints=@($inWindow | Where-Object {$_.line -match 'XtreamEndpointPerf:' -and $_.line -notmatch 'endpoint=get_vod_info'})
  $writeSum=0.0;$entrySum=0.0;$parseSum=0.0
  $lastEndpointEnd=$null
  foreach($e in $endpoints){
   $name=[regex]::Match($e.line,'endpoint=(\S+)').Groups[1].Value;$ms=Number-Field $e.line 'totalMs'
   if($null -ne $ms){
    $stages+=@{name=$name;ms=$ms}
    if($lastEndpointEnd -and $e.at.AddMilliseconds(-$ms) -lt $lastEndpointEnd.AddMilliseconds(-5)){$overlap=$true}
    $lastEndpointEnd=$e.at
   }
   $w=Number-Field $e.line 'gravacaoMs';if($null -ne $w){$writeSum+=$w}
   $p=Number-Field $e.line 'parsingMs';if($null -ne $p){$parseSum+=$p}
   if($name -in @('get_vod_streams','get_series','get_live_streams')){$n=Number-Field $e.line 'aceitos';if($null -ne $n){$entrySum+=$n}}
  }
  if($endpoints.Count){
   $write=$writeSum;$entries=$entrySum;$parse=$parseSum
   $endpointSum=($stages | ForEach-Object {$_.ms} | Measure-Object -Sum).Sum
   if(!$overlap){$stages+=@{name='Outros entre endpoints';ms=[Math]::Max(0,$syncMs-$endpointSum)}}
  }
 }
 foreach($e in $inWindow){if($e.line -match 'COMPACTACAO endpoint='){$n=Number-Field $e.line 'bytesRede';if($null -ne $n){$hasBytes=$true;$bodyBytes+=$n}}}
 $ioWindow=@();$memWindow=@();if($null -ne $offset){$memWindow=@($appTotals | Where-Object {$_.deviceAt -ge $start -and $_.deviceAt -le $(if($visibleAt){$visibleAt}else{$end})})}
 if($null -ne $offset){$ioWindow=@($normalized | Where-Object {$_.deviceAt -ge $start -and $_.deviceAt -le $(if($visibleAt){$visibleAt}else{$end})})}
 $gcs=GC-Stats @($inWindow | ForEach-Object {$_.line})
 $denominator=$syncMs;if($null -ne $post){$denominator+=$post;$stages+=@{name='Fim sync ate Home/visivel';ms=$post}}
 $rank=@($stages | Sort-Object ms -Descending | ForEach-Object {[pscustomobject]@{name=$_.name;ms=$_.ms;percent=[Math]::Round(100*$_.ms/[Math]::Max(1,$denominator),2)}})
 $result=[regex]::Match($line,'resultado=(\S+)').Groups[1].Value;$kind=[regex]::Match($line,'tipo=(\S+)').Groups[1].Value
 $alerts=@()
 $systemWindow=@($sysTimed | Where-Object {$_.at -ge $start -and $_.at -le $(if($visibleAt){$visibleAt}else{$end})})
 $pidPattern='\b(?:'+(@($meta.observedPids) -join '|')+')\b'
 foreach($signal in $systemWindow){
  if($signal.line -match [regex]::Escape($meta.package) -or ($meta.observedPids.Count -gt 0 -and $signal.line -match $pidPattern)){
   if($signal.line -match 'ANR|am_anr'){$alerts+='ANR de sistema associado ao app'}
   if($signal.line -match 'lmkd|lowmemorykiller|am_kill|am_proc_died'){$alerts+='Morte/pressao de memoria do processo: confira eventos-e-erros.txt'}
  }
 }
 if($gcs.pauseMs -gt 200){$alerts+='Pausas GC reconhecidas acima de 200 ms'}
 if(@($inWindow | Where-Object {$_.line -match 'Unable to resolve host|UnknownHostException'}).Count){$alerts+='Falha DNS'}
 if(@($inWindow | Where-Object {$_.line -match 'database is locked|SQLite.*locked'}).Count){$alerts+='Banco bloqueado'}
 if(@($inWindow | Where-Object {$_.line -match 'OutOfMemory|FATAL EXCEPTION|ANR'}).Count){$alerts+='OOM/crash/ANR no app'}
 if(@($inWindow | Where-Object {$_.line -match 'Skipped \d+ frames'}).Count){$alerts+='Frames perdidos reportados no app'}
 if($result -ne 'SUCESSO'){$alerts+='Sincronizacao nao concluiu com sucesso'}
 $rate=$null;if($null -ne $entries -and $syncMs -gt 0){$rate=[Math]::Round($entries*1000/$syncMs,2)}
 $runs+=[pscustomobject]@{execution=$index;kind=$kind;result=$result;start=$start.ToString('o');end=$end.ToString('o');startSource=$startSource;visibleSource=$visibleSource;clickToVisibleMs=$totalObserved;syncMs=$syncMs;postSyncMs=$post;writeRoomMs=$write;parsingMs=$parse;indicesSeparateMs=$null;entries=$entries;entriesPerSecond=$rate;wireBytes=if($hasBytes){$bodyBytes}else{$null};gc=$gcs;javaPssPeak=(Peak $memWindow 'javaPssKiB');nativePssPeak=(Peak $memWindow 'nativePssKiB');graphicsPssPeak=(Peak $memWindow 'graphicsPssKiB');totalPssPeak=(Peak $memWindow 'totalPssKiB');javaAllocatedPeak=(Peak $memWindow 'javaAllocatedKiB');nativeAllocatedPeak=(Peak $memWindow 'nativeAllocatedKiB');diskWriteBytesObserved=(Io-Delta $ioWindow 'writeBytes');diskReadBytesObserved=(Io-Delta $ioWindow 'readBytes');syscwObserved=(Io-Delta $ioWindow 'syscw');phases=$rank;phaseOverlap=$overlap;progressUpdatesPerSecond=$null;alerts=@($alerts | Select-Object -Unique);reduceSyncTo17Ms=[Math]::Max(0,$syncMs-17000);reduceSyncTo15Ms=[Math]::Max(0,$syncMs-15000)}
 $previousEnd=$end
}
$gfx='';$gfxPath=Join-Path $Pasta 'gfx-fim.txt';if(Test-Path $gfxPath){$gfx=[IO.File]::ReadAllText($gfxPath)}
$render=@{totalFrames=(Match-Number $gfx 'Total frames rendered:\s*(\d+)');jankyFrames=(Match-Number $gfx 'Janky frames:\s*(\d+)');p50Ms=(Match-Number $gfx '50th percentile:\s*(\d+)');p90Ms=(Match-Number $gfx '90th percentile:\s*(\d+)');p95Ms=(Match-Number $gfx '95th percentile:\s*(\d+)');p99Ms=(Match-Number $gfx '99th percentile:\s*(\d+)')}
$cpuSummary=@($cpuObserved | Group-Object core | ForEach-Object {[pscustomobject]@{core=$_.Name;meanBusyPercent=($_.Group.busyPercent | Measure-Object -Average).Average;peakBusyPercent=($_.Group.busyPercent | Measure-Object -Maximum).Maximum;peakIoWaitPercent=($_.Group.ioWaitPercent | Measure-Object -Maximum).Maximum}})
$diskSummary=@($diskObserved | Group-Object file | ForEach-Object {$g=@($_.Group | Sort-Object utc);[pscustomobject]@{file=$_.Name;firstBytes=$g[0].bytes;lastBytes=$g[-1].bytes;peakBytes=($g.bytes | Measure-Object -Maximum).Maximum}})
$captureAlerts=@()
if($meta.perfettoRequested -and !$meta.perfettoCollected){$captureAlerts+='Perfetto solicitado, mas nenhum trace confirmado. Confira perfetto-status/perfetto-inicio.'}
if($meta.perfettoFileBytes -ge 33500000){$captureAlerts+='Trace atingiu limite de tamanho e pode nao conter a importacao inteira.'}
$slowSamples=@($samples | Where-Object {$_.durationMs -gt ([double]$meta.intervalTargetSeconds*1000)}).Count
if($slowSamples){$captureAlerts+='Coleta excedeu a cadencia em '+$slowSamples+' amostras: veja duracoes e compare com -SoLogcat.'}
if($render.totalFrames -gt 0 -and $render.jankyFrames/$render.totalFrames -gt 0.1){$captureAlerts+='Mais de 10% de frames janky na captura (estatistica cumulativa desde reset).'}
if(@($cpuSummary | Where-Object {$_.peakIoWaitPercent -gt 10}).Count){$captureAlerts+='Iowait do sistema acima de 10% em amostra; nao e exclusivo do app.'}
$summary=[ordered]@{schema=1;metadata=$meta;executions=$runs;renderingCapture=$render;cpuCapture=$cpuSummary;diskCapture=$diskSummary;captureAlerts=$captureAlerts;slowSampleCount=$slowSamples;topThreadsCapture=@($threadRank | Select-Object -First 12);capturePeaks=@{javaPss=(Peak $appTotals 'javaPssKiB');nativePss=(Peak $appTotals 'nativePssKiB');graphics=(Peak $appTotals 'graphicsPssKiB');totalPss=(Peak $appTotals 'totalPssKiB')};clockAligned=($null -ne $offset);notes=@('Picos somam os processos encontrados no ciclo (leituras sequenciais); veja memoria-total-app.csv e os valores por PID. Se faltar um processo/valor, o agregado fica N/D.',
'Picos sao amostrados, nao picos absolutos. CPU/proc/run-as podem ser negados por Android/SELinux.','bytesRede do app mede corpo HTTP (comprimido quando houver), nao inclui todo overhead TCP/TLS.',
'proc/net/dev e do sistema/namespace, NAO bytes exclusivos do aplicativo. WireBytes vem dos logs do app e soma os endpoints na janela.','Tempo DNS/TCP e custo separado dos indices nao sao inferidos. Rede+parsing M3U so podem ser separados com instrumentacao adicional.','HTTP ate cabecalhos inclui DNS, conexao, TLS e servidor: nao representa apenas primeiro byte da rede.','Comparar duracoes exige mesma lista/build/aparelho e separar frio/quente e coleta com/sem Perfetto.','O total SYNC nao equivale ao clique ate catalogo visivel. Indisponivel fica null; nunca vira zero.','A coleta adiciona trabalho na TV. Cadencia real esta em amostras.jsonl; nenhuma amostra sobrepoe a anterior.','Dados por tabela dependem de sqlite3/dbstat e run-as; veja banco-final.txt.','Frequencia de atualizacao do progresso fica N/D sem marcador especifico do app. CPU por processo usa deltas de /proc/stat e /proc/pid/stat (100% = um nucleo).',
'GC total concorrente nao e pausa: a soma usa somente os valores depois de paused.','Home pronta e um proxy; marcador manual V pode ser usado para registrar a percepcao visual.')}
$summary | ConvertTo-Json -Depth 14 | Set-Content (Join-Path $Pasta 'resumo.json') -Encoding UTF8
$runs | Select-Object execution,kind,result,syncMs,clickToVisibleMs,postSyncMs,writeRoomMs,parsingMs,entries,entriesPerSecond,wireBytes,diskWriteBytesObserved,reduceSyncTo17Ms,reduceSyncTo15Ms | Export-Csv (Join-Path $Pasta 'execucoes.csv') -NoTypeInformation -Encoding UTF8
function Show($Value){if($null -eq $Value){return 'N/D'};return [string]$Value}
$txt=New-Object 'System.Collections.Generic.List[string]'
$txt.Add('DIAGNOSTICO DE IMPORTACAO - '+$meta.package)
$deviceParts=@(([string]$meta.deviceInfo).Trim() -split "`r?`n");if($deviceParts.Count -ge 2){$txt.Add('Modelo: '+$deviceParts[0]+' '+$deviceParts[1])}
$txt.Add('Aparelho/identificador: '+$meta.device+' | Build app: '+$meta.appBuild+' | '+$meta.inicio+' | '+$meta.operacao)
$txt.Add('Dados privados de disco/I-O dependem de run-as/proc; ausencia de acesso permanece N/D. Relogios sao alinhados por marcador ADB com incerteza de transporte.');
$txt.Add('Execucoes encontradas: '+$runs.Count+' | Amostras: '+$samples.Count+' | Alinhamento de relogio: '+($null -ne $offset))
foreach($run in $runs){
 $txt.Add('');$txt.Add(('EXECUCAO {0}: {1} / {2}' -f $run.execution,$run.kind,$run.result))
 $txt.Add('SYNC: '+(Show $run.syncMs)+' ms | Clique ate visivel: '+(Show $run.clickToVisibleMs)+' ms | Pos-sync: '+(Show $run.postSyncMs)+' ms')
 $txt.Add('Inicio: '+$run.startSource+' | Final: '+$run.visibleSource)
 $txt.Add('Entradas: '+(Show $run.entries)+' | entradas/s: '+(Show $run.entriesPerSecond)+' | bytes de rede reportados: '+(Show $run.wireBytes))
 $txt.Add('Gravacao Room: '+(Show $run.writeRoomMs)+' ms | indices separados: N/D | Parsing separado: '+(Show $run.parsingMs)+' ms')
 $txt.Add('GCs: '+$run.gc.events+' | pausas reconhecidas: '+(Show $run.gc.pauseMs)+' ms')
 foreach($field in @('javaPssPeak','nativePssPeak','graphicsPssPeak','totalPssPeak','javaAllocatedPeak','nativeAllocatedPeak')){$peak=$run.$field;$txt.Add($field+': '+(Show $peak.value)+' KiB em '+(Show $peak.utc))}
 $txt.Add('Escrita fisica observada (/proc/io): '+(Show $run.diskWriteBytesObserved)+' bytes')
 $txt.Add('Reducao do SYNC para 17s: '+$run.reduceSyncTo17Ms+' ms; para 15s: '+$run.reduceSyncTo15Ms+' ms')
 if($run.phaseOverlap){$txt.Add('ATENCAO: endpoints sobrepostos; tempos de fases nao sao aditivos.')}
 $txt.Add('Ranking (percentual do sync + pos-sync; submetricas parsing/escrita Xtream nao sao somadas):')
 foreach($phase in $run.phases){$txt.Add(('  {0}: {1} ms ({2}%)' -f $phase.name,$phase.ms,$phase.percent))}
 foreach($alert in $run.alerts){$txt.Add('ALERTA: '+$alert)}
}
$txt.Add('');foreach($alert in $captureAlerts){$txt.Add('ALERTA COLETA: '+$alert)}
$txt.Add('');$txt.Add('RENDERIZACAO DA CAPTURA: '+($render | ConvertTo-Json -Compress))
foreach($t in @($threadRank | Select-Object -First 10)){$txt.Add(('THREAD {0}: pico {1}%; media {2}%; CPU observada {3}s' -f $t.thread,$t.peakPercent,$t.meanPercent,$t.cpuSecondsObserved))}
if(!$runs.Count){$txt.Add('Nenhuma linha SYNC_RESUMO encontrada. Logs e amostras preservados. Faca uma importacao/atualizacao durante a coleta.')}
$txt.Add('');foreach($note in $summary.notes){$txt.Add('NOTA: '+$note)}
$txt | Set-Content (Join-Path $Pasta 'resumo.txt') -Encoding UTF8
Write-Host ('Resumo: '+(Join-Path $Pasta 'resumo.txt'))
