param([Parameter(Mandatory=$true)][string]$Projeto,[string]$PacoteLocal,[switch]$Testar)
$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'gradlew.bat'))) { throw 'Projeto Android nao encontrado.' }
$tempDir=Join-Path $env:TEMP ('import-timing-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null
if ($PacoteLocal) { $zip=(Resolve-Path -LiteralPath $PacoteLocal).Path } else {
 $zip=Join-Path $tempDir 'package.zip'
 Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/correcao-xtream-t12-20261023/correcao-xtream.zip' -OutFile $zip
}
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ne '16db2108429e4cebdf38f08ee9dc1a1c6b5e722cf809b82e5c1dc7f04d1b4679') { throw 'Hash do pacote diferente.' }
Expand-Archive -LiteralPath $zip -DestinationPath $tempDir -Force
$manifest=Get-Content -Raw -LiteralPath (Join-Path $tempDir 'manifest.json') | ConvertFrom-Json
function TextHash([string]$Path) {
 $s=[IO.File]::ReadAllText($Path).Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $sha=[Security.Cryptography.SHA256]::Create()
 try { return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)))).Replace('-','').ToLowerInvariant() } finally { $sha.Dispose() }
}
foreach($entry in $manifest) {
 $target=Join-Path $projectRoot $entry.path
 $source=Join-Path $tempDir $entry.path
 if (!$entry.remove -and (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $entry.sha) { throw 'Arquivo corrompido.' }
 if (Test-Path -LiteralPath $target) {
  $hash=if($entry.binary){(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()}else{TextHash $target}
  if ($entry.before -notcontains $hash -and $hash -ne $entry.sha) { throw ('Arquivo mudou desde o ZIP enviado: '+$entry.path+'. Nenhum arquivo foi alterado.') }
 } elseif (!$entry.allowMissing) { throw ('Arquivo ausente: '+$entry.path) }
}
$changes=@($manifest | Where-Object {
 $target=Join-Path $projectRoot $_.path
 !(Test-Path -LiteralPath $target) -or (TextHash $target) -ne $_.sha
})
if ($changes.Count -gt 0) {
 $backup=Join-Path $projectRoot ('.native-xtream-fix-backups/'+[guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $backup -Force | Out-Null
 $rollback=@()
 foreach($entry in $changes) {
  $target=Join-Path $projectRoot $entry.path
  $existed=Test-Path -LiteralPath $target
  $beforeSha=$null
  if($existed) {
   $saved=Join-Path $backup $entry.path
   New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
   Copy-Item -LiteralPath $target -Destination $saved
   $beforeSha=(Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant()
  }
  $rollback+=@{path=$entry.path;existed=$existed;beforeSha=$beforeSha;afterSha=$entry.sha}
 }
 $rollback | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'rollback.json') -Encoding UTF8
 foreach($entry in $changes) {
  $target=Join-Path $projectRoot $entry.path
  New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
  Copy-Item -LiteralPath (Join-Path $tempDir $entry.path) -Destination $target -Force
 }
 Write-Host "Teste 1 completo aplicado: versao 1.12-T1.2. Backup para desfazer: $backup"
} else { Write-Host 'Teste 1 ja esta aplicado. Nenhum novo backup foi criado.' }
if ($Testar) {
 Push-Location $projectRoot
 try { & .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 assembleDebug; if($LASTEXITCODE -ne 0){throw 'Compilacao falhou; envie a saida.'} }
 finally { Pop-Location }
}
