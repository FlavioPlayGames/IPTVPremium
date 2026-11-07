param(
 [string]$Log = (Join-Path $env:USERPROFILE 'Downloads\iptvplus-legendas.txt'),
 [string]$Destino = (Join-Path $env:USERPROFILE 'Downloads'),
 [string]$Servidor = 'http://infinitybr.top'
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Net.Http
if(!(Test-Path -LiteralPath $Log)){throw 'Registro iptvplus-legendas.txt nao encontrado. Informe -Log com o caminho dele.'}
$text=[IO.File]::ReadAllText((Resolve-Path -LiteralPath $Log).Path)
$identity=$null
foreach($match in [regex]::Matches($text,'https?://[^\s"<>\)\]]+')) {
 $candidate=$null
 if([Uri]::TryCreate($match.Value,[UriKind]::Absolute,[ref]$candidate)) {
  $parts=[regex]::Match($candidate.AbsolutePath,'/movie/([^/]+)/([^/]+)/(\d+)\.[A-Za-z0-9]+$')
  if($parts.Success){$identity=$parts;break}
 }
}
if(!$identity){throw 'Endereco de filme com identificador numerico nao encontrado no registro.'}
$base=$null
if(![Uri]::TryCreate($Servidor,[UriKind]::Absolute,[ref]$base) -or $base.Scheme -notin @('http','https') -or $base.UserInfo){throw 'Servidor invalido.'}
$accountName=[Uri]::UnescapeDataString($identity.Groups[1].Value)
$accountSecret=[Uri]::UnescapeDataString($identity.Groups[2].Value)
$vodId=$identity.Groups[3].Value
$privateValues=@($accountName,$accountSecret,[Uri]::EscapeDataString($accountName),[Uri]::EscapeDataString($accountSecret)) | Where-Object { $_.Length -gt 0 } | Select-Object -Unique
$api=$Servidor.TrimEnd('/')+'/player_api.php?username='+[Uri]::EscapeDataString($accountName)+'&password='+[Uri]::EscapeDataString($accountSecret)+'&action=get_vod_info&vod_id='+$vodId
New-Item -ItemType Directory -Force -Path $Destino | Out-Null
$work=Join-Path $Destino ('xtream-vod-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
$script:urls=New-Object 'System.Collections.Generic.List[object]'
$script:subtitleFields=New-Object 'System.Collections.Generic.List[string]'
function Protect-Text([string]$Value) {
 $Value=[regex]::Replace($Value,'https?://[^\s"<>]+',{
  param($m)
  $extension=$null;$address=$null
  if([Uri]::TryCreate($m.Value,[UriKind]::Absolute,[ref]$address)) {
   $e=[IO.Path]::GetExtension($address.AbsolutePath)
   if($e -match '^\.[A-Za-z0-9]{1,5}$'){$extension=$e.ToLowerInvariant()}
  }
  $id=$script:urls.Count+1
  $script:urls.Add([ordered]@{id=$id;extension=$extension})
  return ('[URL_'+$id+']')
 })
 if($Value -match '^(?:[/\\]|[A-Za-z][A-Za-z0-9+.-]*://)' -or $Value -match '\?[^\s]*=' -or $Value -match '(?i)https?%3a'){$Value='[ENDERECO_OCULTO]'}
 foreach($secret in $privateValues){$Value=$Value.Replace($secret,'[OCULTO]')}
 return $Value
}
function Protect-Node($Value,[string]$Key='',[string]$NodePath='$') {
 if($Key -match '(?i)subtitle|caption|legenda|(^|[_-])sub($|[_-])'){$script:subtitleFields.Add((Protect-Text $NodePath))}
 if($Key -match '(?i)password|username|token|secret|authorization|api.?key|cookie|credential|session'){return '[OCULTO]'}
 if($null -eq $Value){return $null}
 if($Value -is [string]){return (Protect-Text $Value)}
 if($Value -is [System.Collections.IDictionary]) {
  $result=[ordered]@{}
  foreach($k in $Value.Keys){$result[(Protect-Text ([string]$k))]=Protect-Node $Value[$k] ([string]$k) ($NodePath+'.'+$k)}
  return $result
 }
 if($Value -is [System.Management.Automation.PSCustomObject]) {
  $result=[ordered]@{}
  foreach($p in $Value.PSObject.Properties){$result[(Protect-Text $p.Name)]=Protect-Node $p.Value $p.Name ($NodePath+'.'+$p.Name)}
  return $result
 }
 if($Value -is [System.Collections.IEnumerable]) {
  $items=New-Object 'System.Collections.Generic.List[object]';$i=0
  foreach($v in $Value){$items.Add((Protect-Node $v '' ($NodePath+'['+$i+']')));$i++}
  return ,($items.ToArray())
 }
 return $Value
}
$report=[ordered]@{version=1;action='get_vod_info';status=$null;bytes=0;maximumBytes=4194304;parsed=$false;subtitleFields=@();urls=@();error=$null}
$handler=New-Object System.Net.Http.HttpClientHandler
$client=New-Object System.Net.Http.HttpClient($handler)
$client.Timeout=[TimeSpan]::FromSeconds(45)
$token=New-Object System.Threading.CancellationTokenSource
$token.CancelAfter(45000)
$response=$null;$stream=$null;$memory=New-Object IO.MemoryStream
try {
 Write-Host 'Consultando os metadados Xtream do filme registrado...'
 $response=$client.GetAsync($api,[System.Net.Http.HttpCompletionOption]::ResponseHeadersRead,$token.Token).GetAwaiter().GetResult()
 $report.status=[int]$response.StatusCode
 if($report.status -eq 200) {
  $stream=$response.Content.ReadAsStreamAsync().GetAwaiter().GetResult();$buffer=New-Object byte[] 65536
  while($memory.Length -le $report.maximumBytes) {
   $wanted=[int][Math]::Min($buffer.Length,($report.maximumBytes+1-$memory.Length))
   $n=$stream.ReadAsync($buffer,0,$wanted,$token.Token).GetAwaiter().GetResult()
   if($n -eq 0){break}
   $memory.Write($buffer,0,$n)
  }
  $report.bytes=$memory.Length
  if($memory.Length -gt $report.maximumBytes){$report.error='Resposta acima do limite de 4 MiB.'}
  else {
   $raw=[Text.Encoding]::UTF8.GetString($memory.ToArray()).TrimStart([char]0xFEFF)
   $parsed=ConvertFrom-Json -InputObject $raw
   $safe=Protect-Node $parsed
   ConvertTo-Json -InputObject $safe -Depth 80 | Set-Content -LiteralPath (Join-Path $work 'vod-info-sem-acesso.json') -Encoding UTF8
   $report.parsed=$true
  }
 } else {$report.error='Servidor recusou ou nao disponibilizou os metadados. Consulte o status HTTP.'}
} catch {$report.error='Falha de rede, tempo limite ou JSON invalido. Detalhes de acesso omitidos.'}
finally {
 if($stream){$stream.Dispose()}
 if($response){$response.Dispose()}
 $memory.Dispose();$token.Dispose();$client.Dispose();$handler.Dispose()
}
$report.subtitleFields=@($script:subtitleFields.ToArray());$report.urls=@($script:urls.ToArray())
$report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $work 'resultado.json') -Encoding UTF8
$out=Join-Path $Destino ('xtream-vod-diagnostico-'+[Guid]::NewGuid().ToString('N')+'.zip')
Compress-Archive -LiteralPath @((Get-ChildItem -LiteralPath $work -File).FullName) -DestinationPath $out
Write-Host ('Status HTTP: '+$report.status+'; JSON analisado: '+$report.parsed+'; campos relacionados a legendas: '+$report.subtitleFields.Count)
Write-Host ('Arquivo gerado: '+$out)
Write-Host 'Envie esse ZIP na conversa. Aplicativo e projeto nao foram alterados.'
