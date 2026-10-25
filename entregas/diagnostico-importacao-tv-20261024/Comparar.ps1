#requires -Version 5.1
param([string]$Pasta=(Join-Path $env:USERPROFILE 'Downloads/diagnostico-iptv'))
. (Join-Path $PSScriptRoot 'Comum.ps1')
$rows=@()
foreach($file in Get-ChildItem -LiteralPath $Pasta -Filter resumo.json -Recurse){
 $report=Read-Json $file.FullName
 foreach($run in $report.executions){
  if($run.result -ne 'SUCESSO'){continue}
  $rows+=[pscustomobject]@{group=($report.metadata.device+'|'+$report.metadata.appBuild+'|'+$run.kind+'|'+$report.metadata.operacao+'|'+$report.metadata.inicio+'|'+$run.entries+'|perfetto='+$report.metadata.perfettoRequested+'|soLogcat='+$report.metadata.logcatOnly);syncMs=[double]$run.syncMs;clickMs=$run.clickToVisibleMs;file=$file.FullName}
 }
}
$result=@();$text=New-Object 'System.Collections.Generic.List[string]'
$text.Add('COMPARACAO - mesmo aparelho/build/tipo/operacao/frio-quente/entradas/Perfetto')
foreach($g in @($rows | Group-Object group)){
 $values=@($g.Group.syncMs);$mean=($values | Measure-Object -Average).Average;$variance=0.0
 foreach($v in $values){$variance+=[Math]::Pow($v-$mean,2)}
 $sd=$null;if($values.Count -ge 2){$sd=[Math]::Sqrt($variance/($values.Count-1))}
 $clickValues=@($g.Group | Where-Object {$null -ne $_.clickMs} | ForEach-Object {[double]$_.clickMs})
 $meanClick=$null;$sdClick=$null
 if($clickValues.Count){$meanClick=($clickValues | Measure-Object -Average).Average}
 if($clickValues.Count -ge 2){$squares=0.0;foreach($c in $clickValues){$squares+=[Math]::Pow($c-$meanClick,2)};$sdClick=[Math]::Sqrt($squares/($clickValues.Count-1))}
 $r=[pscustomobject]@{group=$g.Name;n=$values.Count;clickN=$clickValues.Count;meanClickMs=$meanClick;sampleStdDevClickMs=$sdClick;meanSyncMs=[Math]::Round($mean,2);sampleStdDevMs=if($null -ne $sd){[Math]::Round($sd,2)}else{$null};minSyncMs=($values | Measure-Object -Minimum).Minimum;maxSyncMs=($values | Measure-Object -Maximum).Maximum;reduceMeanTo17Ms=[Math]::Max(0,[Math]::Round($mean-17000,2));reduceMeanTo15Ms=[Math]::Max(0,[Math]::Round($mean-15000,2));readyForComparison=($values.Count -ge 3);files=@($g.Group.file)}
 $result+=$r
 $text.Add('');$text.Add($g.Name);$text.Add(('n={0}; media SYNC={1} ms; desvio amostral={2} ms; minimo={3}; maximo={4}' -f $r.n,$r.meanSyncMs,$r.sampleStdDevMs,$r.minSyncMs,$r.maxSyncMs))
 if($r.n -lt 3){$text.Add('FALTAM EXECUCOES: use pelo menos 3 por grupo. Nao misture inicio frio e quente.')}
 $text.Add(('Clique ate visivel: n={0}; media={1} ms; desvio={2} ms (vazio = N/D)' -f $r.clickN,$r.meanClickMs,$r.sampleStdDevClickMs))
 $text.Add('Media acima de 17s: '+$r.reduceMeanTo17Ms+' ms; acima de 15s: '+$r.reduceMeanTo15Ms+' ms')
}
$text.Add('');$text.Add('SYNC nao e clique ate tela visivel. Veja o relatorio individual para os marcadores. Contagem igual nao garante a mesma lista: confirme manualmente. Nao altere a lista/build entre os testes comparados.')
$result | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $Pasta 'comparacao.json') -Encoding UTF8
$text | Set-Content (Join-Path $Pasta 'comparacao.txt') -Encoding UTF8
$text | ForEach-Object {Write-Host $_}
