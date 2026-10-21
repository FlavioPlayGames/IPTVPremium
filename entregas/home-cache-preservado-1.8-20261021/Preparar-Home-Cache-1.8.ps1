[CmdletBinding()]
param([string]$Projeto = (Get-Location).Path, [string]$Emulador = '', [switch]$Verificar, [switch]$SomenteAplicar, [switch]$Testar, [switch]$Medir, [string]$PacoteLocal = '')
$ErrorActionPreference = 'Stop'
if (($Testar -or $Medir) -and -not $Emulador) { throw 'Informe -Emulador emulator-5554 para os testes.' }
if ($Emulador -and $Emulador -notmatch '^emulator-\d+$') { throw 'Esta entrega testa somente emuladores.' }
$Projeto = (Resolve-Path -LiteralPath $Projeto).Path
if (-not (Test-Path -LiteralPath (Join-Path $Projeto 'gradlew.bat'))) { throw 'Execute na raiz do projeto Android.' }
$zipPath = $PacoteLocal
$downloaded = -not $zipPath
if ($downloaded) {
    $zipPath = Join-Path ([IO.Path]::GetTempPath()) ('icones-capas-' + [guid]::NewGuid().ToString('N') + '.zip')
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/home-cache-preservado-1.8-20261021/home-cache-preservado.zip' -OutFile $zipPath
}
if ((Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne '80f2e07aff7fdf83507ab2fbb6127ea416796cce59ca515aa668d6e6487ceb95') { throw 'Pacote diferente do esperado; nada aplicado.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($zipPath)
$updates = @()
try {
    $reader = [IO.StreamReader]::new($archive.GetEntry('manifest.json').Open())
    try { $entries = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    foreach ($entry in $entries) {
        if ($entry.path -notmatch '^app/src/(main/java|androidTest/java|main/res/drawable-nodpi)/[a-zA-Z0-9_/.\-]+$' -or $entry.path.Contains('..')) { throw 'Caminho invalido no pacote.' }
        $target = Join-Path $Projeto $entry.path
        if (Test-Path -LiteralPath $target) {
            if ($entry.allowed -notcontains (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()) {
                throw "Arquivo diferente da entrega de icones e capas instalada: $($entry.path). Nenhuma alteracao aplicada."
            }
        } elseif (-not $entry.allowMissing) { throw "Arquivo ausente: $($entry.path). Nada aplicado." }
        $item = $archive.GetEntry($entry.path)
        if (-not $item) { throw 'Pacote incompleto.' }
        $memory = [IO.MemoryStream]::new()
        $stream = $item.Open()
        try { $stream.CopyTo($memory); $bytes = $memory.ToArray() } finally { $stream.Dispose(); $memory.Dispose() }
        $hash = [Security.Cryptography.SHA256]::Create()
        try { $digest = [BitConverter]::ToString($hash.ComputeHash($bytes)).Replace('-','').ToLowerInvariant() } finally { $hash.Dispose() }
        if ($digest -ne $entry.sha) { throw 'Arquivo corrompido no pacote.' }
        $updates += [pscustomobject]@{ path=$entry.path; target=$target; bytes=$bytes; existed=(Test-Path -LiteralPath $target) }
    }
} finally { $archive.Dispose(); if ($downloaded -and (Test-Path -LiteralPath $zipPath)) { Remove-Item -LiteralPath $zipPath } }
if ($Verificar) { Write-Host 'OK: arquivos compativeis. Nada alterado.'; return }
$backup = Join-Path $Projeto ('.navigation-backups/home-cache-preservado-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Path $backup -Force | Out-Null
foreach ($entry in $updates) {
    if ($entry.existed) {
        $saved = Join-Path $backup $entry.path
        New-Item -ItemType Directory -Path (Split-Path $saved -Parent) -Force | Out-Null
        Copy-Item -LiteralPath $entry.target -Destination $saved
    }
}
$updates | Select-Object path,existed | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'arquivos.json') -Encoding UTF8
try {
    foreach ($entry in $updates) {
        New-Item -ItemType Directory -Path (Split-Path $entry.target -Parent) -Force | Out-Null
        [IO.File]::WriteAllBytes($entry.target, $entry.bytes)
    }
} catch {
    foreach ($entry in $updates) {
        if ($entry.existed) { Copy-Item -LiteralPath (Join-Path $backup $entry.path) -Destination $entry.target -Force }
        elseif (Test-Path -LiteralPath $entry.target) { Remove-Item -LiteralPath $entry.target }
    }
    throw
}
Write-Host "Aplicado. Backup: $backup"
if ($SomenteAplicar) { return }
Push-Location $Projeto
try {
    if ($Testar) { & .\gradlew.bat --no-daemon --max-workers=2 assembleDebug assembleDebugAndroidTest }
    else { & .\gradlew.bat --no-daemon --max-workers=2 assembleDebug }
    if ($LASTEXITCODE -ne 0) { throw 'Compilacao falhou. Nenhum APK foi instalado.' }
    if ($Emulador) {
        $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
        if (-not (Test-Path -LiteralPath $adb)) { throw 'ADB nao encontrado. Compile e instale pelo Android Studio.' }
        $m = Get-Content -LiteralPath '.\app\build\outputs\apk\debug\output-metadata.json' -Raw | ConvertFrom-Json
        if ($m.applicationId -ne 'br.com.flaviotorres.iptvpremium') { throw 'APK de outro aplicativo. Instalacao cancelada.' }
        $abis = (& $adb -s $Emulador shell getprop ro.product.cpu.abilist).Trim().Split(',')
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel consultar o dispositivo.' }
        $chosen = @($m.elements | Where-Object { @($_.filters).Count -eq 0 }) | Select-Object -First 1
        if (-not $chosen) {
            foreach ($abi in $abis) {
                $chosen = $m.elements | Where-Object { @($_.filters | Where-Object { $_.filterType -eq 'ABI' -and $_.value -eq $abi }).Count -gt 0 } | Select-Object -First 1
                if ($chosen) { break }
            }
        }
        if (-not $chosen) { throw 'Nenhum APK compativel com o dispositivo.' }
        $apkRoot = [IO.Path]::GetFullPath((Join-Path $Projeto 'app\build\outputs\apk\debug'))
        $apk = [IO.Path]::GetFullPath((Join-Path $apkRoot $chosen.outputFile))
        if ((Split-Path $apk -Parent) -ne $apkRoot -or -not (Test-Path -LiteralPath $apk)) { throw 'Saida de APK invalida.' }
        & $adb -s $Emulador install -r $apk
        if ($LASTEXITCODE -ne 0) { throw 'Instalacao falhou; nao desinstale nem limpe dados.' }
        if ($Testar) {
            $testRoot = Join-Path $Projeto 'app\build\outputs\apk\androidTest\debug'
            $testMeta = Get-Content -LiteralPath (Join-Path $testRoot 'output-metadata.json') -Raw | ConvertFrom-Json
            if ($testMeta.applicationId -ne 'br.com.flaviotorres.iptvpremium.test' -or @($testMeta.elements).Count -ne 1) { throw 'APK de teste inesperado.' }
            $testApk = [IO.Path]::GetFullPath((Join-Path $testRoot $testMeta.elements[0].outputFile))
            if ((Split-Path $testApk -Parent) -ne [IO.Path]::GetFullPath($testRoot)) { throw 'Caminho de teste invalido.' }
            & $adb -s $Emulador install -r -t $testApk
            if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar os testes.' }
            $testLog = Join-Path $Projeto 'diagnostico-home-cache-preservado-testes.txt'
            $classes = 'br.com.flaviotorres.iptvpremium.data.storage.CatalogReusePolicyDeviceTest,br.com.flaviotorres.iptvpremium.data.storage.CatalogReuseStorageDeviceTest,br.com.flaviotorres.iptvpremium.data.cache.PosterCache125DeviceTest,br.com.flaviotorres.iptvpremium.data.cache.PosterCategoryRoomDeviceTest,br.com.flaviotorres.iptvpremium.ui.theme.StandardHomeArtworkDeviceTest'
            $result = & $adb -s $Emulador shell am instrument -w -e class $classes 'br.com.flaviotorres.iptvpremium.test/androidx.test.runner.AndroidJUnitRunner' 2>&1
            $testExit = $LASTEXITCODE
            $result | Out-File -FilePath $testLog -Encoding utf8
            $result | Write-Host
            $textResult = $result | Out-String
            if ($testExit -ne 0 -or $textResult -notmatch 'OK \(39 tests\)' -or $textResult -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
                throw "Teste da Home e reutilizacao do catalogo nao aprovado. Diagnostico: $testLog. O app instalado foi preservado."
            }
        }
    }
} finally { Pop-Location }
Write-Host "Concluido. Versao, listas, historico e recursos Kids existentes preservados. Backup: $backup"

if ($Emulador -and $Medir) {
    $startupLog = Join-Path $Projeto 'diagnostico-home-cache-abertura.txt'
    & $adb -s $Emulador shell am force-stop br.com.flaviotorres.iptvpremium
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel fechar o app para medir.' }
    & $adb -s $Emulador logcat -c
    $launch = & $adb -s $Emulador shell am start -W -n 'br.com.flaviotorres.iptvpremium/.MainActivity' 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel abrir o app para medir.' }
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $startup = @()
    do {
        Start-Sleep -Milliseconds 500
        $startup = & $adb -s $Emulador logcat -d -s 'StartupPerf:I' 'AutoPlaylistSync:I' 'IPTVStorage:I' '*:S' 2>&1
        if ($LASTEXITCODE -ne 0) { throw 'Falha na leitura do diagnostico.' }
        $ready = ($startup | Out-String) -match 'etapa=interface_liberada'
    } while (-not $ready -and $timer.Elapsed.TotalSeconds -lt 90)
    @('Medicao sem limpar dados/cache; fechamento e reabertura do processo.', $launch, $startup) | Out-File -LiteralPath $startupLog -Encoding utf8
    $launch | Write-Host
    $startup | Write-Host
    Write-Host "Diagnostico de abertura: $startupLog"
    if (-not $ready) { Write-Host 'A interface nao confirmou a abertura em 90s. Envie o diagnostico; nenhum dado foi apagado.' }
}
