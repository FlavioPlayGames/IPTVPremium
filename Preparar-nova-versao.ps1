param(
    [Parameter(Mandatory=$true)][ValidateRange(3,2100000000)][int]$Numero,
    [Parameter(Mandatory=$true)][ValidateLength(1,40)][string]$Nome
)
$ErrorActionPreference = "Stop"
$base = (Get-Location).Path
if (-not (Test-Path -LiteralPath (Join-Path $base "app\build.gradle.kts"))) { throw "Abra o terminal na raiz do projeto Android." }
$propsPath = Join-Path $base "local.properties"
$props = if (Test-Path -LiteralPath $propsPath) { [IO.File]::ReadAllText($propsPath) } else { "" }
$previous = [regex]::Match($props, '(?m)^APP_VERSION_CODE=(\d+)\s*$')
$current = if ($previous.Success) { [int]$previous.Groups[1].Value } else { 2 }
if ($Numero -le $current) { throw "O numero interno deve ser maior que $current. Cada nova versao precisa de um numero maior." }
if ($Nome -notmatch '^[A-Za-z0-9][A-Za-z0-9._+-]{0,39}$') { throw "Use um nome como 1.2 ou 1.2-beta, sem espacos ou barras." }
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) {
    $match = [regex]::Match($props, '(?m)^sdk\.dir=(.+)$')
    if ($match.Success) { $sdk = $match.Groups[1].Value.Trim().Replace('\\','\').Replace('\:',':') }
}
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$tools = Get-ChildItem -LiteralPath (Join-Path $sdk "build-tools") -Directory | Sort-Object Name -Descending | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "aapt2.exe") } | Select-Object -First 1
if (-not $tools) { throw "Build-tools Android nao encontrados. Instale pelo SDK Manager do Android Studio." }
$apk = Join-Path $base "app\build\outputs\apk\debug\app-debug.apk"
$signer = Join-Path $tools.FullName "apksigner.bat"
$oldSignature = $null
if ((Test-Path -LiteralPath $apk) -and (Test-Path -LiteralPath $signer)) {
    $signatureOutput = (& $signer verify --print-certs $apk | Out-String)
    if ($LASTEXITCODE -eq 0) { $oldSignature = [regex]::Match($signatureOutput, 'Signer #1 certificate SHA-256 digest:\s*([a-fA-F0-9]{64})').Groups[1].Value }
}
$stamp = Get-Date -Format "yyyyMMdd-HHmmssfff"
if (Test-Path -LiteralPath $propsPath) { Copy-Item -LiteralPath $propsPath -Destination ($propsPath + ".backup-" + $stamp) }
$props = [regex]::Replace($props, '(?m)^APP_VERSION_(CODE|NAME)=.*(?:\r?\n|$)', '')
$props = $props.TrimEnd() + "`r`nAPP_VERSION_CODE=$Numero`r`nAPP_VERSION_NAME=$Nome`r`n"
[IO.File]::WriteAllText($propsPath, $props, (New-Object Text.UTF8Encoding($false)))
& (Join-Path $base "gradlew.bat") :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "A compilacao falhou. Confira a mensagem acima. O numero escolhido ficou salvo; para repetir a compilacao use .\gradlew.bat :app:assembleDebug." }
$badging = (& (Join-Path $tools.FullName "aapt2.exe") dump badging $apk | Out-String)
if ($LASTEXITCODE -ne 0) { throw "Nao foi possivel ler os dados do APK." }
$code = [regex]::Match($badging, "versionCode='([0-9]+)'").Groups[1].Value
$name = [regex]::Match($badging, "versionName='([^']+)'").Groups[1].Value
if ([int]$code -ne $Numero -or $name -ne $Nome) { throw "O APK gerado nao corresponde a versao solicitada. Nao publique." }
if ($oldSignature) {
    $signatureOutput = (& $signer verify --print-certs $apk | Out-String)
    if ($LASTEXITCODE -ne 0) { throw "A assinatura do APK nao foi validada. Nao publique." }
    $newSignature = [regex]::Match($signatureOutput, 'Signer #1 certificate SHA-256 digest:\s*([a-fA-F0-9]{64})').Groups[1].Value
    if ($newSignature -ne $oldSignature) { throw "A assinatura mudou. Nao publique: use a mesma chave da versao instalada." }
}
$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
$metadata = @{ versionCode = [int]$code; versionName = $name; sizeBytes = (Get-Item -LiteralPath $apk).Length; sha256 = $hash }
$metadataPath = Join-Path (Split-Path $apk -Parent) "dados-atualizacao.json"
[IO.File]::WriteAllText($metadataPath, ($metadata | ConvertTo-Json), (New-Object Text.UTF8Encoding($false)))
Write-Host "APK preparado: $apk" -ForegroundColor Green
Write-Host "Dados para o painel: $metadataPath" -ForegroundColor Green
$metadata | Format-List
Write-Host "Teste a instalacao sobre a versao atual. Depois hospede o APK e preencha Atualizacao do aplicativo no painel. Nao desinstale o aplicativo nem troque a chave de assinatura." -ForegroundColor Yellow
