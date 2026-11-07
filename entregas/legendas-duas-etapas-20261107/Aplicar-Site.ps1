param([Parameter(Mandatory=$true)][Alias('Projeto')][string]$Site,[switch]$Testar)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath $Site).Path
if(!(Test-Path -LiteralPath (Join-Path $root 'worker-atualizacao-abi.js')) -or !(Test-Path -LiteralPath (Join-Path $root 'public/app.js')) -or !(Test-Path -LiteralPath (Join-Path $root 'wrangler.jsonc'))){throw 'Informe a pasta do site com Worker, public/app.js e wrangler.jsonc. O ZIP de configuracao sozinho nao contem esses fontes.'}
function HashText([string]$Value){
 $sha=[Security.Cryptography.SHA256]::Create()
 try{return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($Value.Replace("`r`n","`n").TrimStart([char]0xFEFF))))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
}
function CountText([string]$Value,[string]$Fragment){return [regex]::Matches($Value,[regex]::Escape($Fragment)).Count}
$manifestPath=Join-Path $PSScriptRoot 'site/alteracoes.json'
if((Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne 'ae6e6a2941f59cc246f0bec7a5965ae7e89c5038723b3649a9f658a683d927ab'){throw 'Manifesto do site corrompido. Nada foi alterado.'}
$manifest=ConvertFrom-Json -InputObject ([IO.File]::ReadAllText($manifestPath))
$changes=@()
foreach($e in $manifest.patches){
 if([IO.Path]::IsPathRooted($e.path) -or ($e.path -split '[/\\]') -contains '..'){throw 'Caminho invalido.'}
 $target=Join-Path $root $e.path
 if(!(Test-Path -LiteralPath $target)){throw ('Fonte do site ausente: '+$e.path+'. Nada foi alterado.')}
 $original=[IO.File]::ReadAllText($target).Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $value=$original
 foreach($op in $e.operations){
  if([string]::IsNullOrEmpty($op.before) -or [string]::IsNullOrEmpty($op.after)){throw 'Trecho invalido no manifesto.'}
  if((CountText $value $op.after) -eq 1){continue}
  if((CountText $value $op.before) -ne 1){throw ('Versao diferente em '+$e.path+'. Nada foi alterado. Envie o Worker e a pasta public atuais para compatibilizar.')}
  $value=$value.Replace($op.before,$op.after)
 }
 if($value -ne $original){$changes+=@{path=$e.path;text=$value;afterSha=(HashText $value);existed=$true}}
}
foreach($e in $manifest.newFiles){
 if([IO.Path]::IsPathRooted($e.path) -or ($e.path -split '[/\\]') -contains '..'){throw 'Caminho invalido.'}
 $source=Join-Path $PSScriptRoot ('site/referencia/'+$e.path)
 if(!(Test-Path -LiteralPath $source) -or (HashText ([IO.File]::ReadAllText($source))) -ne $e.sha){throw ('Arquivo do pacote corrompido: '+$e.path)}
 $target=Join-Path $root $e.path
 if(Test-Path -LiteralPath $target){
  if((HashText ([IO.File]::ReadAllText($target))) -ne $e.sha){throw ('Arquivo local diferente: '+$e.path+'. Nada foi alterado.')}
 }else{$changes+=@{path=$e.path;text=[IO.File]::ReadAllText($source);afterSha=$e.sha;existed=$false}}
}
if($changes.Count){
 $backup=Join-Path $root ('.legendas-duas-etapas-site-backups/'+[guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $backup | Out-Null
 $rollback=@()
 foreach($e in $changes){
  $target=Join-Path $root $e.path;$saved=Join-Path $backup $e.path;$beforeSha=$null
  if($e.existed){New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null;Copy-Item -LiteralPath $target -Destination $saved;$beforeSha=(Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant()}
  $rollback+=@{path=$e.path;afterSha=$e.afterSha;binary=$false;beforeSha=$beforeSha;existed=$e.existed;removed=$false}
 }
 $rollback | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'rollback.json') -Encoding UTF8
 try {
  foreach($e in $changes){$target=Join-Path $root $e.path;New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null;[IO.File]::WriteAllText($target,$e.text,(New-Object Text.UTF8Encoding($false)))}
 }catch{
  $failure=$_
  foreach($e in $rollback){$target=Join-Path $root $e.path;if($e.existed){Copy-Item -LiteralPath (Join-Path $backup $e.path) -Destination $target -Force}elseif(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target -Force}}
  throw $failure
 }
 Write-Host ('Site atualizado localmente. Backup: '+$backup)
}else{Write-Host 'Atualizacao do site ja aplicada.'}
if($Testar){
 Push-Location $root
 try{
  foreach($path in @('worker-atualizacao-abi.js','opensubtitles.js','public/app.js','public/subtitles.js')){& node --check $path;if($LASTEXITCODE -ne 0){throw 'Falha na verificacao JavaScript. Backup preservado.'}}
  & npx wrangler deploy --dry-run
  if($LASTEXITCODE -ne 0){throw 'Validacao Cloudflare falhou. Nada foi publicado; backup preservado.'}
 }finally{Pop-Location}
}
Write-Host 'Wrangler, D1, segredos e configuracoes de publicacao foram preservados. Nenhuma publicacao automatica.'
