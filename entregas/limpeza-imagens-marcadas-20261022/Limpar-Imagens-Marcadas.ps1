param([Parameter(Mandatory=$true)][string]$Projeto,[string]$PacoteLocal,[switch]$Testar)
$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'gradlew.bat'))) { throw 'Projeto Android nao encontrado.' }
$tempDir=Join-Path $env:TEMP ('import-timing-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null
if ($PacoteLocal) { $zip=(Resolve-Path -LiteralPath $PacoteLocal).Path } else {
 $zip=Join-Path $tempDir 'package.zip'
 Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/limpeza-imagens-marcadas-20261022/limpeza-imagens.zip' -OutFile $zip
}
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ne '0592d2c50d5c51f8060e02b641f6a60f5dfeb19a41c803a92a89d0665103e706') { throw 'Hash do pacote diferente.' }
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
 if ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $entry.sha) { throw 'Arquivo corrompido.' }
 if (Test-Path -LiteralPath $target) {
  $hash=TextHash $target
  if ($hash -ne $entry.before -and $hash -ne $entry.sha) { throw ('Arquivo mudou desde o ZIP enviado: '+$entry.path+'. Nenhum arquivo foi alterado.') }
 } elseif (!$entry.allowMissing) { throw ('Arquivo ausente: '+$entry.path) }
}
$names=@(Get-Content -LiteralPath (Join-Path $tempDir 'remover.txt') | Where-Object { $_ })
$replacement=@{}
foreach($entry in $manifest) { $replacement[(Join-Path $projectRoot $entry.path)]=[IO.File]::ReadAllText((Join-Path $tempDir $entry.path)) }
# Check real source and XML references, including source sets absent from the uploaded ZIP.
Get-ChildItem -LiteralPath (Join-Path $projectRoot 'app/src') -Recurse -File | Where-Object { $_.Extension -in '.kt','.java','.xml' } | ForEach-Object {
 $text=if($replacement.ContainsKey($_.FullName)){$replacement[$_.FullName]}else{[IO.File]::ReadAllText($_.FullName)}
 foreach($name in $names) {
  $pattern='(?:R\.drawable\.|@drawable/|["''])(?:'+[regex]::Escape($name)+')(?![A-Za-z0-9_])'
  if($text -match $pattern) { throw ('Referencia ainda existente: '+$name+' em '+$_.FullName+'. Nenhum arquivo foi alterado.') }
 }
}
$drawable=Join-Path $projectRoot 'app/src/main/res/drawable-nodpi'
$images=@(Get-ChildItem -LiteralPath $drawable -File | Where-Object { $_.BaseName -in $names -and $_.Extension -in '.png','.webp','.jpg','.jpeg','.xml' })
$backup=Join-Path $projectRoot ('.image-cleanup-backups/'+[guid]::NewGuid().ToString('N'))
foreach($entry in $manifest) {
 $target=Join-Path $projectRoot $entry.path
 if (Test-Path -LiteralPath $target) {
  $saved=Join-Path $backup $entry.path
  New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
  Copy-Item -LiteralPath $target -Destination $saved
 }
 New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
 Copy-Item -LiteralPath (Join-Path $tempDir $entry.path) -Destination $target -Force
}
foreach($image in $images) {
 $saved=Join-Path $backup ('drawable-nodpi/'+$image.Name)
 New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
 Move-Item -LiteralPath $image.FullName -Destination $saved
}
Write-Host ("Limpeza concluida: "+$images.Count+" imagens removidas da pasta de recursos. Backup: "+$backup)
if ($Testar) {
 Push-Location $projectRoot
 try { & .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 assembleDebug; if($LASTEXITCODE -ne 0){throw 'Compilacao falhou; envie a saida.'} }
 finally { Pop-Location }
}
