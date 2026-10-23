param([Parameter(Mandatory=$true)][string]$Projeto,[string]$PacoteLocal,[switch]$Testar)
$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'gradlew.bat'))) { throw 'Projeto Android nao encontrado.' }
$tempDir=Join-Path $env:TEMP ('import-timing-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null
if ($PacoteLocal) { $zip=(Resolve-Path -LiteralPath $PacoteLocal).Path } else {
 $zip=Join-Path $tempDir 'package.zip'
 Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/remover-fanart-20261023/remover-fanart.zip' -OutFile $zip
}
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ne '3889abd8c3c796bad31332c9b204ece1809b76a2c6cd664e3ef4fe40acf9ed64') { throw 'Hash do pacote diferente.' }
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
# Remove only this optional service key, preserving all other local settings.
$localPath=Join-Path $projectRoot 'local.properties'
$localChanged=$false
if(Test-Path -LiteralPath $localPath) {
 $localText=[IO.File]::ReadAllText($localPath)
 $cleanLocal=[regex]::Replace($localText,'(?m)^[\t ]*FANART_API_KEY(?:[\t ]*[:=][^\r\n]*|[\t ]+[^\r\n]*)?\r?(?:\n|$)','')
 $localChanged=$cleanLocal -cne $localText
}
$backup=Join-Path $projectRoot ('.remove-artwork-service-backups/'+[guid]::NewGuid().ToString('N'))
foreach($entry in $manifest) {
 $target=Join-Path $projectRoot $entry.path
 if (Test-Path -LiteralPath $target) {
  $saved=Join-Path $backup $entry.path
  New-Item -ItemType Directory -Force -Path (Split-Path $saved) | Out-Null
  Copy-Item -LiteralPath $target -Destination $saved
 }
 if ($entry.remove) {
  if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Force }
 } else {
  New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
  Copy-Item -LiteralPath (Join-Path $tempDir $entry.path) -Destination $target -Force
 }
}
if($localChanged) {
 $savedLocal=Join-Path $backup 'local.properties'
 New-Item -ItemType Directory -Force -Path (Split-Path $savedLocal) | Out-Null
 Copy-Item -LiteralPath $localPath -Destination $savedLocal
 $originalBytes=[IO.File]::ReadAllBytes($localPath)
 $withBom=$originalBytes.Length -ge 3 -and $originalBytes[0] -eq 239 -and $originalBytes[1] -eq 187 -and $originalBytes[2] -eq 191
 [IO.File]::WriteAllText($localPath,$cleanLocal,[Text.UTF8Encoding]::new($withBom))
}
Write-Host "Integracao removida. TMDB, TVmaze e fundos genericos preservados. Backup: $backup"
if ($Testar) {
 Push-Location $projectRoot
 try { & .\gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 assembleDebug; if($LASTEXITCODE -ne 0){throw 'Compilacao falhou; envie a saida.'} }
 finally { Pop-Location }
}
