param([Parameter(Mandatory=$true)][string]$Projeto,[string]$Backup,[switch]$Testar)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath $Projeto).Path
if(!(Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))){throw 'Projeto Android nao encontrado.'}
if(!$Backup){
 $folder=Get-ChildItem -LiteralPath (Join-Path $root '.player-refino-backups') -Directory | Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'rollback.json')} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
 if(!$folder){throw 'Backup desta revisao nao encontrado.'};$Backup=$folder.FullName
}
$entries=ConvertFrom-Json -InputObject ([IO.File]::ReadAllText((Join-Path $Backup 'rollback.json')))
function TextHash([string]$Path,[bool]$Binary=$false){
 if($Binary){return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()}
 $s=[IO.File]::ReadAllText($Path).Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $sha=[Security.Cryptography.SHA256]::Create()
 try{return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
}
foreach($e in $entries){
 $target=Join-Path $root $e.path
 if($e.removed){if(Test-Path -LiteralPath $target){throw ('Arquivo recriado depois da revisao: '+$e.path+'. Nada foi restaurado.')}}
 elseif(!(Test-Path -LiteralPath $target) -or (TextHash $target $e.binary) -ne $e.afterSha){throw ('Arquivo alterado depois da revisao: '+$e.path+'. Nada foi restaurado.')}
 if($e.existed){$saved=Join-Path $Backup $e.path;if(!(Test-Path -LiteralPath $saved) -or (Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant() -ne $e.beforeSha){throw 'Backup ausente ou corrompido. Nada foi restaurado.'}}
}
foreach($e in $entries){
 $target=Join-Path $root $e.path
 if($e.existed){New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null;Copy-Item -LiteralPath (Join-Path $Backup $e.path) -Destination $target -Force}
 elseif(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target -Force}
}
Write-Host 'Fontes anteriores restaurados. Compile e instale novamente. Nenhum dado da TV foi apagado.'
if($Testar){Push-Location $root;try{& .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 :app:assembleDesempenho;if($LASTEXITCODE -ne 0){throw 'Compilacao falhou.'}}finally{Pop-Location}}
