[CmdletBinding()]
param(
    [string]$Projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium",
    [Alias('Tv')][string]$Serial = '',
    [string]$PastaApks = '',
    [Alias('AdbPath')][string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [switch]$SemCompilar
)
$ErrorActionPreference = 'Stop'
$Projeto = (Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath $Adb -PathType Leaf)) { throw "ADB nao encontrado: $Adb" }
$linhas = @(& $Adb devices -l 2>&1)
if ($LASTEXITCODE -ne 0) { throw ('Falha ao consultar o ADB: ' + ($linhas -join "`n")) }
$conectados = @($linhas | ForEach-Object {
    if ("$_" -match '^(\S+)\s+device(?:\s+(.*))?$') {
        [PSCustomObject]@{ Serial = $Matches[1]; Detalhes = [string]$Matches[2] }
    }
})
if ([string]::IsNullOrWhiteSpace($Serial)) {
    $tvs = @($conectados | Where-Object { $_.Detalhes -match '(?:^|\s)(?:model:Smart_TV_Pro|product:G08_4K_GB|device:G08)(?:\s|$)' })
    if ($tvs.Count -eq 1) { $Serial = $tvs[0].Serial }
    elseif ($tvs.Count -eq 0 -and $conectados.Count -eq 1) { $Serial = $conectados[0].Serial }
    elseif ($conectados.Count -eq 0) {
        throw "Nenhum aparelho conectado e autorizado no ADB. Confira a conexao existente na TV e execute adb devices -l. Nao foi alterado o pareamento."
    } else {
        throw ('Mais de um aparelho conectado. Informe -Serial com a TV desejada. Conectados: ' + (($conectados | ForEach-Object { $_.Serial }) -join ', '))
    }
}
if ($Serial -notmatch '^[A-Za-z0-9._:-]+$') { throw 'Identificador ADB invalido.' }
if (@($conectados | Where-Object { $_.Serial -eq $Serial }).Count -ne 1) { throw "Aparelho nao conectado ou nao autorizado: $Serial" }
$estado = @(& $Adb -s $Serial get-state 2>&1)
if ($LASTEXITCODE -ne 0 -or (($estado -join "`n").Trim()) -ne 'device') { throw "Aparelho indisponivel: $Serial" }
$listaAbis = @(& $Adb -s $Serial shell getprop ro.product.cpu.abilist)
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel consultar a arquitetura da TV.' }
$arquiteturas = ($listaAbis -join ',').Trim()
if ([string]::IsNullOrWhiteSpace($arquiteturas)) {
    $abi = @(& $Adb -s $Serial shell getprop ro.product.cpu.abi)
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel consultar a arquitetura da TV.' }
    $arquiteturas = ($abi -join ',').Trim()
}
if ([string]::IsNullOrWhiteSpace($arquiteturas)) { throw 'A TV nao informou uma arquitetura valida.' }
Write-Host "Aparelho: $Serial. ABIs: $arquiteturas"
if (!$SemCompilar) {
    $gradlew = Join-Path $Projeto 'gradlew.bat'
    if (!(Test-Path -LiteralPath $gradlew -PathType Leaf)) { throw "Gradle wrapper nao encontrado: $gradlew" }
    Write-Host 'Compilando a variante desempenho com a configuracao e assinatura locais...'
    Push-Location -LiteralPath $Projeto
    try {
        & $gradlew :app:assembleDesempenho
        if ($LASTEXITCODE -ne 0) { throw 'A compilacao falhou. Nenhum APK foi instalado.' }
    } finally { Pop-Location }
}
if ([string]::IsNullOrWhiteSpace($PastaApks)) { $PastaApks = Join-Path $Projeto 'app\build\outputs\apk\desempenho' }
if (!(Test-Path -LiteralPath $PastaApks -PathType Container)) { throw "Pasta de APKs nao encontrada: $PastaApks" }
$arquivos = @(Get-ChildItem -LiteralPath $PastaApks -Filter '*.apk' -File)
$apk = $null
foreach ($itemAbi in $arquiteturas.Split(',')) {
    $itemAbi = $itemAbi.Trim()
    if ($itemAbi -notin @('armeabi-v7a','arm64-v8a','x86','x86_64')) { continue }
    $candidatos = @($arquivos | Where-Object { $_.Name -match ('-' + [regex]::Escape($itemAbi) + '-desempenho\.apk$') })
    if ($candidatos.Count -eq 1) { $apk = $candidatos[0]; break }
    if ($candidatos.Count -gt 1) { throw "Mais de um APK desempenho para $itemAbi. Confira a pasta de saida." }
}
if ($null -eq $apk) {
    $universais = @($arquivos | Where-Object { $_.Name -match '-universal-desempenho\.apk$' })
    if ($universais.Count -ne 1) { throw "APK desempenho compativel nao encontrado. ABIs da TV: $arquiteturas" }
    $apk = $universais[0]
}
Write-Host "Instalando $($apk.Name)..."
& $Adb -s $Serial install -r $apk.FullName
if ($LASTEXITCODE -ne 0) { throw 'Instalacao recusada. Confira assinatura e versao. O script nao desinstala nem apaga dados para contornar a recusa.' }
Write-Host 'Instalacao concluida. Abra o IPTV Premium na TV para testar.'
