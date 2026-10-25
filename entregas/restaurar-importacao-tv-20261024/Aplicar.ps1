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
 $target=Join-Path $root $e.path
 if($e.action -eq 'replace'){
  $source=Join-Path $PSScriptRoot $e.path
  if(!(Test-Path -LiteralPath $source) -or (TextHash $source) -ne $e.sha){throw ('Pacote corrompido: '+$e.path+'. Nada foi alterado.')}
  if(!(Test-Path -LiteralPath $target)){throw ('Arquivo ausente: '+$e.path+'. Nada foi alterado.')}
 }elseif($e.action -eq 'remove'){
  if(!(Test-Path -LiteralPath $target)){continue}
 }else{throw 'Acao desconhecida. Nada foi alterado.'}
 $hash=TextHash $target
 if($e.before -notcontains $hash -and $hash -ne $e.sha){throw ('Arquivo diferente da versao conhecida: '+$e.path+'. Nada foi alterado.')}
}
$changes=@($entries | Where-Object {
 $target=Join-Path $root $_.path
 if($_.action -eq 'remove'){Test-Path -LiteralPath $target}
 else{(TextHash $target) -ne $_.sha}
})
if($changes.Count -gt 0){
 $backup=Join-Path $root ('.restaurar-importacao-backups/'+[guid]::NewGuid().ToString('N'))
 $savedEntries=@()
 foreach($e in $changes){
  $target=Join-Path $root $e.path
  $saved=Join-Path $backup $e.path
  New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
  Copy-Item -LiteralPath $target -Destination $saved
  $savedEntries+=@{path=$e.path;action=$e.action;beforeSha=(Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant();afterSha=$e.sha}
 }
 $savedEntries | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'backup.json') -Encoding UTF8
 foreach($e in $changes){
  $target=Join-Path $root $e.path
  if($e.action -eq 'remove'){Remove-Item -LiteralPath $target -Force}
  else{Copy-Item -LiteralPath (Join-Path $PSScriptRoot $e.path) -Destination $target -Force}
 }
 Write-Host "Importacao completa restaurada. Backup dos fontes: $backup"
}else{Write-Host 'Restauracao ja aplicada.'}
Write-Host 'O banco avanca para versao 6, preservando dados e recriando os indices do arquivo enviado. Catalogos parciais serao recarregados.'
if($Testar){
 Push-Location $root
 try{& .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 :app:assembleDesempenho;if($LASTEXITCODE -ne 0){throw 'Compilacao falhou. O backup foi preservado; envie a saida.'}}
 finally{Pop-Location}
}
