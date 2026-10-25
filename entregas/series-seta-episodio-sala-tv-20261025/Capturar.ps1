param([string]$Dispositivo,[string]$Arquivo=(Join-Path $env:USERPROFILE 'Downloads/validacao-series-seta-sala-tv.txt'))
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if(!(Test-Path -LiteralPath $adb)){throw "ADB nao encontrado: $adb"}
if(!$Dispositivo){
 $lines=& $adb devices
 if($LASTEXITCODE -ne 0){throw 'Falha ao consultar dispositivos.'}
 $connected=@($lines | Where-Object {$_ -match '^\S+\s+device\s*$'} | ForEach-Object {($_ -split '\s+')[0]})
 if($connected.Count -ne 1){throw 'Conecte apenas o aparelho do teste ou informe -Dispositivo com seu identificador em adb devices.'}
 $Dispositivo=[string]$connected[0]
}

$info=& $adb -s $Dispositivo shell pm list packages -U br.com.flaviotorres.iptvpremium
if($LASTEXITCODE -ne 0){throw 'Falha ao consultar o aplicativo.'}
$iptvUid=[regex]::Match(($info -join "`n"),'uid:\s*(\d+)').Groups[1].Value
if(!$iptvUid){throw 'Aplicativo nao encontrado no aparelho selecionado.'}
New-Item -ItemType Directory -Force -Path (Split-Path $Arquivo) | Out-Null
Write-Host "Capturando em: $Arquivo"
Write-Host 'Entre em Series, abra uma serie com historico, teste Assistir -> Baixo -> Temporada -> Baixo -> ultimo episodio. Reproduza e volte. Confira a sala de carregamento em outras series. Finalize com Ctrl+C.'
& $adb -s $Dispositivo logcat -b main,system,crash -v threadtime "--uid=$iptvUid" -T 1 | Out-File -LiteralPath $Arquivo -Encoding utf8
