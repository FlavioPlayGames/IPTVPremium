param(
 [string]$Log = (Join-Path $env:USERPROFILE 'Downloads\iptvplus-legendas.txt'),
 [string]$Destino = (Join-Path $env:USERPROFILE 'Downloads')
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Net.Http
if(!(Test-Path -LiteralPath $Log)){throw 'Arquivo iptvplus-legendas.txt nao encontrado. Informe -Log com o caminho dele.'}
$text=[IO.File]::ReadAllText((Resolve-Path -LiteralPath $Log).Path)
$videoUrl=$null
foreach($match in [regex]::Matches($text,'https?://[^\s"<>\)\]]+')) {
 $candidate=$null
 if([Uri]::TryCreate($match.Value,[UriKind]::Absolute,[ref]$candidate) -and $candidate.AbsolutePath -match '/(movie|series)/'){$videoUrl=$candidate;break}
}
if(!$videoUrl){throw 'Nao foi encontrado um endereco de filme ou episodio no registro informado.'}
New-Item -ItemType Directory -Force -Path $Destino | Out-Null
$work=Join-Path $Destino ('vod-diagnostico-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
$handler=New-Object System.Net.Http.HttpClientHandler
$client=New-Object System.Net.Http.HttpClient($handler)
$client.Timeout=[TimeSpan]::FromSeconds(45)
$report=[ordered]@{version=1;maximumBytes=12582912;headBytes=0;tailBytes=0;fileBytes=$null;headStatus=$null;tailStatus=$null;headFrom=$null;tailFrom=$null;error=$null}
function Read-VideoRange([long]$Start,[long]$End,[string]$Name) {
 $request=New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Get,$videoUrl)
 $response=$null;$inputStream=$null;$outputStream=$null
 $token=New-Object System.Threading.CancellationTokenSource
 $token.CancelAfter(45000)
 try {
  $request.Headers.Range=New-Object System.Net.Http.Headers.RangeHeaderValue($Start,$End)
  $response=$client.SendAsync($request,[System.Net.Http.HttpCompletionOption]::ResponseHeadersRead,$token.Token).GetAwaiter().GetResult()
  $status=[int]$response.StatusCode
  if($status -ne 200 -and $status -ne 206){return @{status=$status;bytes=0;from=$null;length=$null}}
  $range=$response.Content.Headers.ContentRange
  $from=$null;$length=$null
  if($range){$from=$range.From;$length=$range.Length}
  elseif($status -eq 200){$from=0;$length=$response.Content.Headers.ContentLength}
  # Never mistake a whole-file response or a mismatched range for its tail.
  if(($status -eq 206 -and ($null -eq $from -or $from -ne $Start)) -or ($Start -gt 0 -and $status -ne 206)) {
   return @{status=$status;bytes=0;from=$from;length=$length}
  }
  $inputStream=$response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
  $outputStream=[IO.File]::Create((Join-Path $work $Name))
  $buffer=New-Object byte[] 65536
  [long]$remaining=$End-$Start+1;[long]$written=0
  while($remaining -gt 0) {
   $wanted=[int][Math]::Min($buffer.Length,$remaining)
   $read=$inputStream.ReadAsync($buffer,0,$wanted,$token.Token).GetAwaiter().GetResult()
   if($read -eq 0){break}
   $outputStream.Write($buffer,0,$read);$written+=$read;$remaining-=$read
  }
  return @{status=$status;bytes=$written;from=$from;length=$length}
 } finally {
  if($outputStream){$outputStream.Dispose()}
  if($inputStream){$inputStream.Dispose()}
  if($response){$response.Dispose()}
  $request.Dispose();$token.Dispose()
 }
}
try {
 Write-Host 'Lendo ate 4 MiB do inicio do video...'
 $head=Read-VideoRange 0 4194303 'inicio.bin'
 $report.headStatus=$head.status;$report.headBytes=$head.bytes;$report.headFrom=$head.from;$report.fileBytes=$head.length
 if($head.bytes -gt 0 -and $null -ne $head.length -and $head.length -gt $head.bytes) {
  # Do not download overlapping bytes from a small file.
  [long]$start=[Math]::Max([long]$head.bytes,([long]$head.length-8388608))
  Write-Host 'Lendo ate 8 MiB do final do video...'
  $tail=Read-VideoRange $start ([long]$head.length-1) 'final.bin'
  $report.tailStatus=$tail.status;$report.tailBytes=$tail.bytes;$report.tailFrom=$tail.from
 }
} catch {
 # Exceptions may contain the account URL. Export only a fixed diagnostic label.
 $report.error='Falha ou tempo limite na leitura. Endereco e credenciais omitidos.'
} finally {$client.Dispose();$handler.Dispose()}
$report | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $work 'resultado.json') -Encoding UTF8
$out=Join-Path $Destino ('vod-diagnostico-'+[Guid]::NewGuid().ToString('N')+'.zip')
Compress-Archive -LiteralPath @((Get-ChildItem -LiteralPath $work -File).FullName) -DestinationPath $out
Write-Host ('Arquivo gerado: '+$out)
Write-Host 'Envie esse ZIP na conversa. O aplicativo e o projeto nao foram modificados.'
if($report.headBytes -eq 0){Write-Host ('Nao foi possivel ler o video. Status HTTP: '+$report.headStatus)}
