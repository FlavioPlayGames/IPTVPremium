param(
 [string]$Arquivo=(Join-Path $env:USERPROFILE ('Downloads/capacidade-fonte-{0}.json' -f (Get-Date -Format 'yyyyMMdd-HHmmss'))),
 [ValidateRange(5,90)][int]$TimeoutSegundos=30
)
$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
Add-Type -AssemblyName System.Web
Write-Host 'Cole o link M3U get.php. A entrada fica oculta e nao sera gravada no relatorio.'
$secret=Read-Host -AsSecureString 'Link'
$plain=[Net.NetworkCredential]::new('', $secret).Password
try {
 $clean=$plain.Trim().Replace('&amp;','&').Replace('\&','&')
 $uri=$null
 if(![Uri]::TryCreate($clean,[UriKind]::Absolute,[ref]$uri) -or $uri.Scheme -notin @('http','https') -or $uri.UserInfo -or $uri.AbsolutePath -notmatch '(?i)/get\.php/?$') {
  throw 'Formato nao reconhecido. Informe um link http/https get.php com username e password.'
 }
 $q=[System.Web.HttpUtility]::ParseQueryString($uri.Query)
 if(!$q['username'] -or !$q['password']){throw 'O link nao contem os dois parametros de conta exigidos.'}
 $base=$uri.GetLeftPart([UriPartial]::Authority)+($uri.AbsolutePath -replace '(?i)/get\.php/?$','')+'/player_api.php'
 $encoded='username='+[Uri]::EscapeDataString($q['username'])+'&password='+[Uri]::EscapeDataString($q['password'])
} finally { $plain=$null; $clean=$null; $secret.Dispose() }

function Invoke-Probe([string]$Action,[string]$Key='') {
 $watch=[Diagnostics.Stopwatch]::StartNew()
 $request=$null; $response=$null; $reader=$null; $text=$null; $data=$null
 $status=0; $characters=0L
 try {
  $endpoint=$base+'?'+$encoded
  if($Action -ne 'account'){$endpoint+='&action='+[Uri]::EscapeDataString($Action)}
  $request=[Net.HttpWebRequest]::Create($endpoint)
  $request.Method='GET'; $request.UserAgent='Mozilla/5.0'
  $request.Timeout=$TimeoutSegundos*1000; $request.ReadWriteTimeout=$TimeoutSegundos*1000
  $request.AutomaticDecompression=[Net.DecompressionMethods]::GZip -bor [Net.DecompressionMethods]::Deflate
  $response=$request.GetResponse(); $status=[int]$response.StatusCode
  $reader=[IO.StreamReader]::new($response.GetResponseStream(),[Text.Encoding]::UTF8,$true)
  $buffer=New-Object char[] 8192; $body=[Text.StringBuilder]::new()
  while(($read=$reader.Read($buffer,0,$buffer.Length)) -gt 0){
   $characters+=$read
   if($characters -gt 64000000){throw [IO.InvalidDataException]::new('response_limit')}
   if($watch.Elapsed.TotalSeconds -gt $TimeoutSegundos){throw [TimeoutException]::new()}
   [void]$body.Append($buffer,0,$read)
  }
  $text=$body.ToString();$body=$null
  $trimmed=$text.Trim()
  if($Action -eq 'account'){
   if(!$trimmed.StartsWith('{') -or !$trimmed.EndsWith('}')){throw [IO.InvalidDataException]::new('incomplete_object')}
  } elseif(!$trimmed.StartsWith('[') -or !$trimmed.EndsWith(']')){throw [IO.InvalidDataException]::new('incomplete_array')}
  $data=ConvertFrom-Json -InputObject $text -ErrorAction Stop
  if($Action -eq 'account') {
   $auth=([string]$data.user_info.auth -eq '1')
   $active=([string]$data.user_info.status -eq 'Active')
   return [pscustomobject]@{action=$Action;success=$true;httpStatus=$status;authenticated=$auth;active=$active;rows=$null;missingIds=$null;characters=$characters;elapsedMs=$watch.ElapsedMilliseconds;errorType=$null}
  }
  if(!$text.TrimStart().StartsWith('[')){throw [IO.InvalidDataException]::new('expected_array')}
  $rows=if($null -eq $data){@()}else{@($data)}
  $missing=0
  foreach($row in $rows){if($Key -and [string]::IsNullOrWhiteSpace([string]$row.$Key)){$missing++}}
  return [pscustomobject]@{action=$Action;success=$true;httpStatus=$status;authenticated=$null;active=$null;rows=$rows.Count;missingIds=$missing;characters=$characters;elapsedMs=$watch.ElapsedMilliseconds;errorType=$null}
 } catch {
  $errorObject=$_.Exception
  for($depth=0; $depth -lt 6; $depth++){
   if($errorObject.Response){try{$status=[int]$errorObject.Response.StatusCode}catch{}}
   if(!$errorObject.InnerException){break}
   $errorObject=$errorObject.InnerException
  }
  # Never emit exception messages: they can contain the credential-bearing request URL.
  return [pscustomobject]@{action=$Action;success=$false;httpStatus=$status;authenticated=$null;active=$null;rows=$null;missingIds=$null;characters=$characters;elapsedMs=$watch.ElapsedMilliseconds;errorType=$errorObject.GetType().Name}
 } finally {
  if($reader){$reader.Dispose()};if($response){$response.Dispose()};if($request){$request.Abort()}
  $endpoint=$null;$text=$null;$trimmed=$null;$data=$null
 }
}

$results=@()
Write-Host 'Verificando autenticacao...'
$account=Invoke-Probe 'account';$results+= $account
if($account.success -and $account.authenticated -and $account.active){
 foreach($probe in @(
  @('get_live_categories','category_id'), @('get_vod_categories','category_id'), @('get_series_categories','category_id'),
  @('get_live_streams','stream_id'), @('get_vod_streams','stream_id'), @('get_series','series_id')
 )) {
  Write-Host ('Verificando '+$probe[0]+'...')
  $result=Invoke-Probe $probe[0] $probe[1];$results+=$result
  if($result.success){Write-Host ('  Entradas: '+$result.rows+'; tempo: '+$result.elapsedMs+' ms')}
  else{Write-Host ('  Nao concluido. HTTP: '+$result.httpStatus+'; tipo: '+$result.errorType)}
 }
}else{Write-Host 'Conta ativa nao confirmada; nao consultaremos os catalogos.'}
$report=[ordered]@{
 version='capability-r1';capturedAt=(Get-Date -Format 'o');execution='PC';appChanged=$false
 authenticatedActive=([bool]($account.success -and $account.authenticated -and $account.active))
 coverageVerified=$false
 note='Contagens da API nao comprovam equivalencia item a item com a M3U. Nenhum transporte foi alterado. Nenhum episodio ou video foi solicitado.'
 probes=$results
}
$parent=Split-Path -Parent $Arquivo
if($parent){New-Item -ItemType Directory -Force -Path $parent | Out-Null}
$report|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $Arquivo -Encoding UTF8
$encoded=$null;$q=$null;$uri=$null;$base=$null
Write-Host ('Concluido. Envie somente este relatorio: '+$Arquivo)
