param([Parameter(Mandatory=$true)][string]$Projeto,[string]$Dispositivo,[ValidateSet('desempenho','debug')][string]$Variante='desempenho')
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
$abiText=(& $adb -s $Dispositivo shell getprop ro.product.cpu.abilist) -join ''
if($LASTEXITCODE -ne 0){throw 'Nao foi possivel consultar a arquitetura do aparelho.'}
if([string]::IsNullOrWhiteSpace($abiText)){
 $abiText=(& $adb -s $Dispositivo shell getprop ro.product.cpu.abi) -join ''
 if($LASTEXITCODE -ne 0){throw 'Nao foi possivel consultar a arquitetura do aparelho.'}
}
$root=(Resolve-Path -LiteralPath $Projeto).Path
$apk=$null
$chosenAbi=$null
foreach($abi in ($abiText.Trim() -split ',')){
 if($abi -notmatch '^(arm64-v8a|armeabi-v7a|x86_64|x86)$'){continue}
 $candidate=Join-Path $root ('app/build/outputs/apk/{0}/app-{1}-{0}.apk' -f $Variante,$abi)
 if(Test-Path -LiteralPath $candidate){$apk=$candidate;$chosenAbi=$abi;break}
}
if(!$apk){throw 'Nenhum APK especifico compativel foi encontrado. Compile Aplicar.ps1 -Testar com os splits ABI do projeto. O APK universal nao foi instalado.'}
Write-Host ('Arquitetura: '+$chosenAbi+'; APK: '+[Math]::Round((Get-Item -LiteralPath $apk).Length/1000000,1)+' MB')
& $adb -s $Dispositivo shell am force-stop br.com.flaviotorres.iptvpremium
if($LASTEXITCODE -ne 0){throw 'Nao foi possivel parar o aplicativo.'}
& $adb -s $Dispositivo install -r $apk
if($LASTEXITCODE -ne 0){throw 'Instalacao falhou. Nenhum dado foi apagado pelo script.'}
Write-Host 'APK compativel instalado, sem apagar dados. Inicie a captura e abra o aplicativo no aparelho.'
