param([Parameter(Mandatory=$true)][string]$Projeto,[string]$Backup,[switch]$Testar)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath $Projeto).Path
if(!(Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))){throw 'Projeto Android nao encontrado.'}
if(!$Backup){
 $folder=Get-ChildItem -LiteralPath (Join-Path $root '.xtream-sob-demanda-backups') -Directory | Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'rollback.json')} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
 if(!$folder){throw 'Backup catalogo sob demanda nao encontrado.'}
 $Backup=$folder.FullName
}
$entries=ConvertFrom-Json -InputObject ([IO.File]::ReadAllText((Join-Path $Backup 'rollback.json')))
function TextHash([string]$Path){
 $s=[IO.File]::ReadAllText($Path).Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $sha=[Security.Cryptography.SHA256]::Create()
 try{return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
}
$bridge=Join-Path $PSScriptRoot 'rollback/PlaylistCacheDatabase.kt'
$dbFile=Join-Path $root 'app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/PlaylistCacheDatabase.kt'
if(!(Test-Path -LiteralPath $bridge) -or (TextHash $bridge) -ne 'e83d49fb49b92844778ece8d50ec14e2a4ce7ad6d9a56dc0d5e19774986f4584'){throw 'Ponte de retorno ausente ou corrompida. Nada foi alterado.'}
if(!(Test-Path -LiteralPath $dbFile) -or (TextHash $dbFile) -ne '2f330cea44f99a8b8d451877aac4c728f7e86ea5d8545f674b3fe8eca5813e89'){throw 'Banco-fonte alterado depois do teste. Nada foi restaurado.'}
foreach($e in $entries){
 $target=Join-Path $root $e.path
 if(!(Test-Path -LiteralPath $target) -or (TextHash $target) -ne $e.afterSha){throw ('Arquivo alterado depois do teste: '+$e.path+'. Nada foi restaurado.')}
 if($e.existed){
 $saved=Join-Path $Backup $e.path
  if(!(Test-Path -LiteralPath $saved) -or (Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant() -ne $e.beforeSha){throw 'Backup ausente ou corrompido. Nada foi restaurado.'}
 }
}
foreach($e in $entries){
 $target=Join-Path $root $e.path
 if($e.existed){Copy-Item -LiteralPath (Join-Path $Backup $e.path) -Destination $target -Force}
 else{Remove-Item -LiteralPath $target -Force}
}
Copy-Item -LiteralPath $bridge -Destination $dbFile -Force
Write-Host 'Importador anterior restaurado com protecao para catalogo parcial. Na abertura seguinte a lista parcial sera recarregada. Gere e instale novamente o APK para usar os fontes anteriores. Nenhum dado do aparelho foi apagado.'
if($Testar){Push-Location $root;try{& .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 :app:assembleDesempenho;if($LASTEXITCODE -ne 0){throw 'Compilacao falhou.'}}finally{Pop-Location}}
