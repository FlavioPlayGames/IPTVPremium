param([Parameter(Mandatory=$true)][string]$Projeto,[switch]$Testar)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath $Projeto).Path
if(!(Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))){throw 'Projeto Android nao encontrado.'}
function HashText([string]$Text){
 $s=$Text.Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $sha=[Security.Cryptography.SHA256]::Create()
 try{return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
}
function TextHash([string]$Path){return HashText ([IO.File]::ReadAllText($Path))}
$entries=ConvertFrom-Json -InputObject ([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'manifest.json')))
foreach($e in $entries){
 $source=Join-Path $PSScriptRoot $e.path
 $target=Join-Path $root $e.path
 if(!(Test-Path -LiteralPath $source) -or (TextHash $source) -ne $e.sha){throw ('Pacote corrompido: '+$e.path)}
 if(!(Test-Path -LiteralPath $target)){if($e.allowMissing){continue}else{throw ('Arquivo ausente: '+$e.path+'. Nenhum arquivo foi alterado.')}}
 $hash=TextHash $target
 if($e.before -notcontains $hash -and $hash -ne $e.sha){throw ('Arquivo diferente do projeto enviado: '+$e.path+'. Nenhum arquivo foi alterado.')}
}
$gradlePath=Join-Path $root 'app/build.gradle.kts'
if(!(Test-Path -LiteralPath $gradlePath)){throw 'app/build.gradle.kts nao encontrado. Nenhum arquivo foi alterado.'}
$gradleText=[IO.File]::ReadAllText($gradlePath)
$marker='apply(from = "tv-performance.gradle") // IPTV_TV_PERFORMANCE_VARIANT_V1'
$already=$gradleText.Contains($marker)
if(!$already -and ($gradleText.Contains('tv-performance.gradle') -or $gradleText.Contains('IPTV_TV_PERFORMANCE_VARIANT_V1'))){throw 'Configuracao de desempenho diferente da esperada. Nenhum arquivo foi alterado.'}
if([regex]::Matches($gradleText,[regex]::Escape($marker)).Count -gt 1){throw 'Configuracao de desempenho duplicada. Nenhum arquivo foi alterado.'}
if($gradleText -match '(?:create|register|getByName|named)\s*\(\s*["'']desempenho["'']' -or $gradleText -match '(?m)^\s*desempenho\s*\{'){
 throw 'Ja existe uma variante desempenho no arquivo principal. Nenhum arquivo foi alterado.'
}
$changes=@($entries | Where-Object {!(Test-Path -LiteralPath (Join-Path $root $_.path)) -or (TextHash (Join-Path $root $_.path)) -ne $_.sha})
$gradleNewBytes=$null
if(!$already){
 $newline=if($gradleText.Contains("`r`n")){"`r`n"}else{"`n"}
 $tail=$newline+$marker+$newline
 [byte[]]$gradleNewBytes=([IO.File]::ReadAllBytes($gradlePath))+([Text.Encoding]::UTF8.GetBytes($tail))
 $changes+=@([pscustomobject]@{path='app/build.gradle.kts';sha=(HashText ($gradleText+$tail));dynamic=$true})
}
if($changes.Count -gt 0){
 $backup=Join-Path $root ('.desempenho-tv-backups/'+[guid]::NewGuid().ToString('N'))
 $rollback=@()
 foreach($e in $changes){
  $target=Join-Path $root $e.path
  $saved=Join-Path $backup $e.path
  New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
  $existed=Test-Path -LiteralPath $target
  $beforeSha=$null
  if($existed){Copy-Item -LiteralPath $target -Destination $saved;$beforeSha=(Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant()}
  $rollback+=@{path=$e.path;afterSha=$e.sha;beforeSha=$beforeSha;existed=$existed}
 }
 $rollback | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'rollback.json') -Encoding UTF8
 foreach($e in $changes){
  $target=Join-Path $root $e.path
  New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
  if($e.dynamic){[IO.File]::WriteAllBytes($target,$gradleNewBytes)}
  else{Copy-Item -LiteralPath (Join-Path $PSScriptRoot $e.path) -Destination $target -Force}
 }
 Write-Host "Variante desempenho preparada. Backup: $backup"
}else{Write-Host 'Variante desempenho ja preparada.'}
if($Testar){
 Push-Location $root
 try{& .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 :app:assembleDesempenho;if($LASTEXITCODE -ne 0){throw 'Compilacao falhou. Envie a saida; o backup foi preservado.'}}
 finally{Pop-Location}
}
