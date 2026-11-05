param([Parameter(Mandatory=$true)][string]$Projeto,[switch]$Testar)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath $Projeto).Path
if(!(Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))){throw 'Projeto Android nao encontrado.'}
function TextHash([string]$Path,[bool]$Binary=$false){
 if($Binary){return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()}
 $s=[IO.File]::ReadAllText($Path).Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $sha=[Security.Cryptography.SHA256]::Create()
 try{return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
}
$entries=ConvertFrom-Json -InputObject ([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'manifest.json')))
foreach($e in $entries){
 if([IO.Path]::IsPathRooted($e.path) -or ($e.path -split '[/\\]') -contains '..'){throw 'Caminho invalido no manifesto.'}
 $source=Join-Path $PSScriptRoot $e.path
 $target=Join-Path $root $e.path
 if(!$e.remove -and (!(Test-Path -LiteralPath $source) -or (TextHash $source $e.binary) -ne $e.sha)){throw ('Pacote corrompido: '+$e.path)}
 if(!(Test-Path -LiteralPath $target)){if($e.allowMissing){continue}else{throw ('Arquivo ausente: '+$e.path+'. Nada foi alterado.')}}
 $hash=TextHash $target $e.binary
 if($e.before -notcontains $hash -and ($e.remove -or $hash -ne $e.sha)){throw ('Arquivo diferente do java.zip enviado: '+$e.path+'. Nada foi alterado.')}
}
$changes=@($entries | Where-Object {
 $target=Join-Path $root $_.path
 if($_.remove){Test-Path -LiteralPath $target}else{!(Test-Path -LiteralPath $target) -or (TextHash $target $_.binary) -ne $_.sha}
})
if($changes.Count -gt 0){
 $backup=Join-Path $root ('.kids-posicao-entrada-backups/'+[guid]::NewGuid().ToString('N'))
 $rollback=@()
 foreach($e in $changes){
  $target=Join-Path $root $e.path
  $saved=Join-Path $backup $e.path
  New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
  $existed=Test-Path -LiteralPath $target
  $beforeSha=$null
  if($existed){Copy-Item -LiteralPath $target -Destination $saved;$beforeSha=(Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant()}
  $rollback+=@{path=$e.path;afterSha=$e.sha;binary=$e.binary;beforeSha=$beforeSha;existed=$existed;removed=$e.remove}
 }
 $rollback | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'rollback.json') -Encoding UTF8
 try {
  foreach($e in $changes){
   $target=Join-Path $root $e.path
   if($e.remove){Remove-Item -LiteralPath $target -Force}
   else{New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null;Copy-Item -LiteralPath (Join-Path $PSScriptRoot $e.path) -Destination $target -Force}
  }
 } catch {
  $failure=$_
  foreach($e in $rollback){
   $target=Join-Path $root $e.path
   if($e.existed){Copy-Item -LiteralPath (Join-Path $backup $e.path) -Destination $target -Force}
   elseif(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target -Force}
  }
  throw $failure
 }
 Write-Host "Player atualizado. Originais, inclusive backups antigos, guardados em: $backup"
}else{Write-Host 'Atualizacao ja aplicada.'}
if($Testar){Push-Location $root;try{& .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 :app:assembleDesempenho;if($LASTEXITCODE -ne 0){throw 'Compilacao falhou. O backup foi preservado; envie a saida.'}}finally{Pop-Location}}
