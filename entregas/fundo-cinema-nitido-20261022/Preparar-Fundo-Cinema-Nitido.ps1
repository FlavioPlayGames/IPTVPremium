param([Parameter(Mandatory=$true)][string]$Projeto,[string]$PacoteLocal,[switch]$Testar)
$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'gradlew.bat'))) { throw 'Projeto Android nao encontrado.' }
$tempDir=Join-Path $env:TEMP ('import-timing-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null
if ($PacoteLocal) { $zip=(Resolve-Path -LiteralPath $PacoteLocal).Path } else {
 $zip=Join-Path $tempDir 'package.zip'
 Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/fundo-cinema-nitido-20261022/fundo-cinema-nitido.zip' -OutFile $zip
}
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ne '1ae410dcadfefc84bf7c4abc3c231bdcf4536dfd482b121d2e64e50f405ed426') { throw 'Hash do pacote diferente.' }
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
$artSource=Join-Path $tempDir 'app/src/main/res/drawable-nodpi/cinema_details_fallback.webp'
if((Get-FileHash -LiteralPath $artSource -Algorithm SHA256).Hash -ne '9e8fbfe91845262d3051213735fd0494188e11bdfcc3cceeecd546056d3918e7') { throw 'Imagem diferente do pacote.' }
$resRoot=Join-Path $projectRoot 'app/src/main/res'
$oldImages=@(Get-ChildItem -LiteralPath $resRoot -Directory | Where-Object { $_.Name -match '^drawable(?:-.*)?$' } | ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -File } | Where-Object { $_.BaseName -eq 'cinema_details_fallback' -and $_.Extension -in '.png','.webp','.jpg','.jpeg','.xml' })
$backup=Join-Path $projectRoot ('.cinema-backups/'+[guid]::NewGuid().ToString('N'))
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
foreach($oldImage in $oldImages) {
 $relative=$oldImage.FullName.Substring($projectRoot.TrimEnd('\','/').Length).TrimStart('\','/')
 $saved=Join-Path $backup $relative
 New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
 Move-Item -LiteralPath $oldImage.FullName -Destination $saved
}
$artTarget=Join-Path $projectRoot 'app/src/main/res/drawable-nodpi/cinema_details_fallback.webp'
New-Item -ItemType Directory -Force -Path (Split-Path $artTarget) | Out-Null
Copy-Item -LiteralPath $artSource -Destination $artTarget -Force
Write-Host "Fundo nitido aplicado em WebP. Backup: $backup"
if ($Testar) {
 Push-Location $projectRoot
 try { & .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 assembleDebug; if($LASTEXITCODE -ne 0){throw 'Compilacao falhou; envie a saida.'} }
 finally { Pop-Location }
}
