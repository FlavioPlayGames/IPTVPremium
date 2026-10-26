param(
 [ValidateSet('GradeLivre')][string]$Modo='GradeLivre',
 [string]$Dispositivo,
 [ValidateRange(60,900)][int]$DuracaoSegundos=180,
 [ValidateRange(0,120)][int]$AquecimentoSegundos=45,
 [string]$Pasta
)
$ErrorActionPreference='Stop'
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$package='br.com.flaviotorres.iptvpremium'
if(!(Test-Path -LiteralPath $adb)){throw 'ADB nao encontrado no Android SDK padrao.'}
if(!$Dispositivo){
 $lines=& $adb devices
 if($LASTEXITCODE-ne 0){throw 'Falha ao consultar dispositivos.'}
 $devices=@($lines|Where-Object{$_-match '^\S+\s+device\s*$'}|ForEach-Object{($_-split '\s+')[0]})
 if($devices.Count-ne 1){throw 'Conecte apenas a TV ou informe -Dispositivo.'}
 $Dispositivo=[string]$devices[0]
}
if(!$Pasta){$Pasta=Join-Path $env:USERPROFILE ('Downloads/grade-livre-series-{0}-{1}' -f $Modo,(Get-Date -Format 'yyyyMMdd-HHmmss'))}
Write-Host "Teste ${Modo}: o aplicativo sera fechado e reaberto, sem apagar dados ou cache."
& $adb -s $Dispositivo shell am force-stop $package
if($LASTEXITCODE-ne 0){throw 'Nao foi possivel fechar o aplicativo.'}
& $adb -s $Dispositivo shell am start -W -n "$package/.MainActivity"
if($LASTEXITCODE-ne 0){throw 'Nao foi possivel abrir o aplicativo.'}
Write-Host "Volte para a TV. Aquecimento por $AquecimentoSegundos segundos; depois medicao por $DuracaoSegundos segundos."
Write-Host 'Aquecimento: entre em Series e navegue pelas primeiras capas. Durante a medicao navegue em Series, alternando movimentos lentos e rapidos e retornando a fileiras anteriores.'
Write-Host 'Mantenha a mesma lista e categoria. Nao abra Kids/player, nao importe nem limpe o cache durante a medicao.'
for($second=0;$second-lt $AquecimentoSegundos;$second++){Start-Sleep -Seconds 1}
& (Join-Path $PSScriptRoot 'Medir-Fluidez.ps1') -Dispositivo $Dispositivo -DuracaoSegundos $DuracaoSegundos -IntervaloSegundos 15 -Pasta $Pasta
if(!(Test-Path -LiteralPath (Join-Path $Pasta 'resumo.json'))){throw 'Relatorio incompleto. Verifique os arquivos gerados.'}
[ordered]@{modeLabel=$Modo;modeVerifiedAutomatically=$false;warmupSeconds=$AquecimentoSegundos;durationSeconds=$DuracaoSegundos;appRestarted=$true;diskCacheCleared=$false;note='O rotulo descreve a versao que o usuario instalou. Nao prova automaticamente a versao do APK. A grade e identificada pelo rotulo do teste; arquivos brutos e picos estao no ZIP.'}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $Pasta 'condicao-teste.json') -Encoding UTF8
Compress-Archive -Path (Join-Path $Pasta '*') -DestinationPath ($Pasta+'.zip') -Force
Write-Host ('Envie este ZIP: '+$Pasta+'.zip')
