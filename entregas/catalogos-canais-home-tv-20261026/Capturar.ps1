param(
 [ValidateSet('Filmes','Series')][string]$Tela='Filmes',
 [string]$Dispositivo,
 [ValidateRange(60,300)][int]$DuracaoSegundos=120,
 [ValidateRange(15,120)][int]$AquecimentoSegundos=45
)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$package='br.com.flaviotorres.iptvpremium'
if(!(Test-Path -LiteralPath $adb)){throw 'ADB nao encontrado no Android SDK padrao.'}
if(!$Dispositivo){
 $lines=& $adb devices
 if($LASTEXITCODE-ne 0){throw 'Falha ao consultar dispositivos.'}
 $devices=@($lines|Where-Object{$_-match '^\S+\s+device\s*$'}|ForEach-Object{($_-split '\s+')[0]})
 if($devices.Count-ne 1){throw 'Conecte apenas a TV ou informe -Dispositivo.'}
 $Dispositivo=[string]$devices[0]
}
$stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
$Pasta=Join-Path $env:USERPROFILE ('Downloads/catalogos-canais-home-{0}-{1}' -f $Tela,$stamp)
New-Item -ItemType Directory -Force -Path $Pasta|Out-Null
& $adb -s $Dispositivo shell am force-stop $package
if($LASTEXITCODE-ne 0){throw 'Nao foi possivel fechar o aplicativo.'}
& $adb -s $Dispositivo shell am start -W -n "$package/.MainActivity"
if($LASTEXITCODE-ne 0){throw 'Nao foi possivel abrir o aplicativo.'}
Write-Host "Volte para a TV e entre em ${Tela}. Ha $AquecimentoSegundos segundos de preparacao."
Write-Host "Depois navegue SOMENTE na grade de $Tela por $DuracaoSegundos segundos. Nao abra fichas, player ou outras abas. Nao limpe cache nem atualize listas."
for($second=0;$second-lt $AquecimentoSegundos;$second++){Start-Sleep -Seconds 1}
$token=[Guid]::NewGuid().ToString('N')
$remoteConfig="/data/local/tmp/iptv-trace-$token.txt"
$remoteTrace="/data/misc/perfetto-traces/iptv-$token.pftrace"
$durationMs=$DuracaoSegundos*1000
$config=@"
buffers { size_kb: 16384 fill_policy: RING_BUFFER }
duration_ms: $durationMs
write_into_file: true
file_write_period_ms: 5000
max_file_size_bytes: 67108864
data_sources {
 config {
  name: "linux.ftrace"
  ftrace_config {
   ftrace_events: "sched/sched_switch"
   ftrace_events: "sched/sched_waking"
   ftrace_events: "sched/sched_process_exit"
   ftrace_events: "power/cpu_frequency"
   ftrace_events: "power/cpu_idle"
   atrace_categories: "gfx"
   atrace_categories: "view"
   atrace_categories: "wm"
   atrace_categories: "am"
   atrace_categories: "dalvik"
   atrace_categories: "database"
   atrace_categories: "binder_driver"
   atrace_apps: "$package"
  }
 }
}
data_sources { config { name: "linux.process_stats" process_stats_config { scan_all_processes_on_start: true proc_stats_poll_ms: 1000 } } }
data_sources { config { name: "linux.sys_stats" sys_stats_config { stat_period_ms: 1000 meminfo_period_ms: 1000 cpufreq_period_ms: 1000 } } }
"@
$configPath=Join-Path $Pasta 'perfetto-config.txt'
[IO.File]::WriteAllText($configPath,$config,(New-Object Text.UTF8Encoding($false)))
function Invoke-TraceAdb([string[]]$Arguments,[string]$Log) {
 $ErrorActionPreference='Continue'
 try {
  $lines=& $adb @Arguments 2>&1
  $code=$LASTEXITCODE
  $lines|ForEach-Object { $_.ToString() }|Out-File -LiteralPath $Log -Encoding utf8
  return [int]$code
 } catch { $_|Out-String|Set-Content -LiteralPath $Log; return 1 }
}
$traceStarted=$false
if((Invoke-TraceAdb -Arguments @('-s',$Dispositivo,'shell','perfetto','--version') -Log (Join-Path $Pasta 'perfetto-disponibilidade.txt'))-eq 0){
 if((Invoke-TraceAdb -Arguments @('-s',$Dispositivo,'push',$configPath,$remoteConfig) -Log (Join-Path $Pasta 'perfetto-upload.txt'))-eq 0){
  $traceStarted=(Invoke-TraceAdb -Arguments @('-s',$Dispositivo,'shell','perfetto','--background','--txt','-c',$remoteConfig,'-o',$remoteTrace) -Log (Join-Path $Pasta 'perfetto-inicio.txt'))-eq 0
 }
}
# A ausencia de Perfetto nao interrompe a coleta de logcat, memoria e frames.
try {
 & (Join-Path $PSScriptRoot 'Medir-Fluidez.ps1') -Dispositivo $Dispositivo -DuracaoSegundos $DuracaoSegundos -IntervaloSegundos 15 -Pasta $Pasta
 if(!(Test-Path -LiteralPath (Join-Path $Pasta 'resumo.json'))){throw 'Coleta principal incompleta. Preserve a pasta gerada.'}
 [void](Invoke-TraceAdb -Arguments @('-s',$Dispositivo,'shell','dumpsys','gfxinfo',$package,'framestats') -Log (Join-Path $Pasta 'frames-detalhados.txt'))
 $tracePulled=$false
 if($traceStarted){
  $tracePulled=(Invoke-TraceAdb -Arguments @('-s',$Dispositivo,'pull',$remoteTrace,(Join-Path $Pasta 'navegacao.pftrace')) -Log (Join-Path $Pasta 'perfetto-download.txt'))-eq 0
  $tracePulled=$tracePulled-and(Test-Path -LiteralPath (Join-Path $Pasta 'navegacao.pftrace'))
  if($tracePulled){$tracePulled=(Get-Item -LiteralPath (Join-Path $Pasta 'navegacao.pftrace')).Length-gt 0}
 }
 [ordered]@{screenLabel=$Tela;screenVerifiedAutomatically=$false;durationSeconds=$DuracaoSegundos;warmupSeconds=$AquecimentoSegundos;traceStarted=$traceStarted;traceDownloaded=$tracePulled;apkChanged=$false;cacheCleared=$false;note='Rotulo manual; conferir eventos do app. Trace limitado a 64 MiB, com eventos de sistema/threads. Pode acrescentar carga; usar o mesmo coletor nas duas telas.'}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $Pasta 'condicao-teste.json') -Encoding UTF8
 Compress-Archive -Path (Join-Path $Pasta '*') -DestinationPath ($Pasta+'.zip') -Force
 Write-Host ('Envie este ZIP: '+$Pasta+'.zip')
 if(!$tracePulled){Write-Host 'Perfetto nao foi obtido nesta TV; os demais dados e o motivo foram preservados no ZIP.'}
} finally {
 # Somente nossos arquivos temporarios, identificados por GUID, sao removidos.
 # Se a captura falhar antes do fim, preserva o trace remoto para recuperacao.
 [void](Invoke-TraceAdb -Arguments @('-s',$Dispositivo,'shell','rm','-f',$remoteConfig) -Log (Join-Path $Pasta 'trace-config-remocao.txt'))
 if($tracePulled){[void](Invoke-TraceAdb -Arguments @('-s',$Dispositivo,'shell','rm','-f',$remoteTrace) -Log (Join-Path $Pasta 'trace-remocao.txt'))}
}
