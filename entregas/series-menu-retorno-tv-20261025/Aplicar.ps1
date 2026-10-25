param([Parameter(Mandatory=$true)][string]$Projeto,[switch]$Testar)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath $Projeto).Path
if(!(Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))){throw 'Projeto Android nao encontrado.'}
function TextHash([string]$Path){
 $s=[IO.File]::ReadAllText($Path).Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $sha=[Security.Cryptography.SHA256]::Create()
 try{return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
}
$entries=ConvertFrom-Json -InputObject ([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'manifest.json')))
foreach($e in $entries){
 $source=Join-Path $PSScriptRoot $e.path
 $target=Join-Path $root $e.path
 if(!(Test-Path -LiteralPath $source) -or (TextHash $source) -ne $e.sha){throw ('Pacote corrompido: '+$e.path)}
 if(!(Test-Path -LiteralPath $target)){if($e.allowMissing){continue}else{throw ('Arquivo ausente: '+$e.path+'. Nenhum arquivo foi alterado.')}}
 $hash=TextHash $target
 if($e.before -notcontains $hash -and $hash -ne $e.sha){throw ('Arquivo diferente do projeto enviado: '+$e.path+'. Nenhum arquivo foi alterado.')}
}
$changes=@($entries | Where-Object {!(Test-Path -LiteralPath (Join-Path $root $_.path)) -or (TextHash (Join-Path $root $_.path)) -ne $_.sha})
if($changes.Count -gt 0){
 $backup=Join-Path $root ('.series-menu-retorno-backups/'+[guid]::NewGuid().ToString('N'))
 $rollback=@()
 foreach($e in $changes){
  $target=Join-Path $root $e.path
  $saved=Join-Path $backup $e.path
  New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
  $existed=Test-Path -LiteralPath $target
  $beforeSha=$null
  if($existed){
   Copy-Item -LiteralPath $target -Destination $saved
   $beforeSha=(Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant()
  }
  $rollback+=@{path=$e.path;afterSha=$e.sha;beforeSha=$beforeSha;existed=$existed}
 }
 $rollback | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'rollback.json') -Encoding UTF8
 foreach($e in $changes){
  $target=Join-Path $root $e.path
  New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
  Copy-Item -LiteralPath (Join-Path $PSScriptRoot $e.path) -Destination $target -Force
 }
 Write-Host "Correcao aplicada. Backup: $backup"
}else{Write-Host 'Correcao ja aplicada.'}
if($Testar){
 Push-Location $root
 try{& .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 :app:assembleDesempenho;if($LASTEXITCODE -ne 0){throw 'A compilacao falhou. Envie a saida; o backup foi preservado.'}}
 finally{Pop-Location}
}
