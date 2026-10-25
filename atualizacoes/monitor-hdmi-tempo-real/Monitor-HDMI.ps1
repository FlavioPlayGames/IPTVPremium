# Monitor local e somente leitura. Windows PowerShell 5.1 ou PowerShell 7.
[CmdletBinding()]
param(
    [string]$AdbPath = '',
    [string]$Tv = '',
    [ValidateRange(5,1800)][int]$DuracaoSegundos = 180,
    [string]$Saida = '',
    [switch]$SemTeclado
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (!$AdbPath) {
    if (!$env:LOCALAPPDATA) { throw 'Informe -AdbPath com o caminho do adb.exe.' }
    $AdbPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
}
if (!(Test-Path -LiteralPath $AdbPath -PathType Leaf)) { throw "ADB nao encontrado: $AdbPath" }
$devices = @(& $AdbPath devices 2>&1)
if ($LASTEXITCODE -ne 0) { throw ($devices -join "`n") }
$ready = @($devices | ForEach-Object { if ("$_" -match '^([^\s]+)\s+device\s*$') { $Matches[1] } })
if (!$Tv) {
    if ($ready.Count -ne 1) { throw "Informe -Tv com um identificador de 'adb devices'. Conectados: $($ready -join ', ')" }
    $Tv = $ready[0]
}
if ($ready -notcontains $Tv) { throw "TV nao conectada/autorizada: $Tv" }
if ($Tv -notmatch '^[A-Za-z0-9._:-]+$') { throw 'Identificador ADB invalido.' }
if (!$Saida) { $Saida = Join-Path (Join-Path $env:USERPROFILE 'Downloads') ('HDMI-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
$Saida = [IO.Path]::GetFullPath($Saida)
[IO.Directory]::CreateDirectory($Saida) | Out-Null
$utf8 = [Text.UTF8Encoding]::new($false)
$log = [IO.StreamWriter]::new((Join-Path $Saida 'eventos-hdmi.txt'), $false, $utf8)
$markers = [IO.StreamWriter]::new((Join-Path $Saida 'marcacoes.txt'), $false, $utf8)
$log.AutoFlush = $true
$markers.AutoFlush = $true
$raw = Join-Path $Saida 'captura-temporaria.tmp'
$stderrPath = Join-Path $Saida 'erro-adb.txt'
$capture = $null
$reader = $null
$watch = [Diagnostics.Stopwatch]::StartNew()
$lastSummary = -1
$matched = 0
$render = 0
$metadata = 0
$other = 0
$status = 'concluido'
$keyboard = !$SemTeclado
try {
    $markers.WriteLine('Inicio no PC: ' + [DateTimeOffset]::Now.ToString('o'))
    $deviceTime = @(& $AdbPath -s $Tv shell date 2>&1)
    $markers.WriteLine('Relogio da TV: ' + ($deviceTime -join ' '))
    $markers.WriteLine('F = percebeu linhas piscando; 1 = modo 144 Hz; 2 = modo 120 Hz; 3 = VRR desligado; Q = encerrar.')
    $markers.WriteLine('Marcacoes sao declaracoes manuais; o monitor nao altera nem confirma esses modos.')
    if (Get-Command Get-CimInstance -ErrorAction SilentlyContinue) {
        Get-CimInstance Win32_VideoController |
            Select-Object Name, DriverVersion, DriverDate, CurrentRefreshRate |
            Format-List | Out-String | Set-Content -LiteralPath (Join-Path $Saida 'placa-driver.txt') -Encoding UTF8
    }
    $capture = Start-Process -FilePath $AdbPath -ArgumentList @('-s', $Tv, 'logcat', '-v', 'threadtime', '-T', '1', '*:V') -RedirectStandardOutput $raw -RedirectStandardError $stderrPath -PassThru -NoNewWindow
    $wait = [Diagnostics.Stopwatch]::StartNew()
    while (!(Test-Path -LiteralPath $raw)) {
        if ($capture.HasExited -or $wait.Elapsed.TotalSeconds -gt 5) { throw 'Nao foi possivel iniciar a captura ADB.' }
        Start-Sleep -Milliseconds 100
    }
    $stream = [IO.FileStream]::new($raw, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
    $reader = [IO.StreamReader]::new($stream, [Text.Encoding]::UTF8, $true, 8192)
    Write-Host "Monitorando por ate $DuracaoSegundos segundos. Pasta: $Saida" -ForegroundColor Cyan
    Write-Host 'Deixe a TV na entrada do PC. Com este terminal em foco: F marca o piscar; Q encerra.'
    Write-Host '1/2/3 apenas marcam mudancas feitas por voce. Nenhuma configuracao sera alterada.'
    $quit = $false
    while (!$quit -and $watch.Elapsed.TotalSeconds -lt $DuracaoSegundos) {
        # Drain in bounded chunks so heavy firmware logging cannot starve the controls/timer.
        for ($i = 0; $i -lt 1000; $i++) {
            $line = $reader.ReadLine()
            if ($null -eq $line) { break }
            if ($line -match '(?i)hdmi|vrr|freesync|edid|\bfrl\b|\btmds\b|MI_RENDER|PQ_AIDL') {
                $safe = [regex]::Replace($line, 'https?://\S+', '[URL_REMOVIDA]')
                $safe = [regex]::Replace($safe, '(?i)(username|password|token)=([^\s&]+)', '$1=[REMOVIDO]')
                $log.WriteLine($safe)
                $matched++
                if ($line -match 'MI_RENDER') { $render++ }
                elseif ($line -match 'PQ_AIDL') { $metadata++ }
                else { $other++ }
            }
        }
        $second = [int][Math]::Floor($watch.Elapsed.TotalSeconds)
        if ($second -ne $lastSummary) {
            Write-Host ("{0}s | Eventos: {1} | MI_RENDER: {2} | PQ/HDR: {3} | Outros HDMI: {4}" -f $second,$matched,$render,$metadata,$other)
            $lastSummary = $second
        }
        if ($keyboard) {
            try {
                if ([Console]::KeyAvailable) {
                    $key = [Console]::ReadKey($true).Key
                    $label = switch ($key) {
                        'F' { 'PISCAR percebido pelo usuario' }
                        'D1' { 'Usuario informa: 144 Hz' }
                        'D2' { 'Usuario informa: 120 Hz' }
                        'D3' { 'Usuario informa: VRR desligado' }
                        'Q' { $quit = $true; 'Encerramento solicitado' }
                        default { $null }
                    }
                    if ($label) {
                        $entry = [DateTimeOffset]::Now.ToString('o') + (' elapsedMs={0} ' -f $watch.ElapsedMilliseconds) + $label
                        $markers.WriteLine($entry)
                        Write-Host $label -ForegroundColor Yellow
                    }
                }
            } catch { $keyboard = $false; Write-Host 'Teclas indisponiveis neste terminal; encerramento automatico pelo tempo.' }
        }
        if ($capture.HasExited) { $status = 'adb_encerrado'; break }
        if ((Get-Item -LiteralPath $raw).Length -gt 64MB) { $status = 'limite_temporario_64MB'; break }
        Start-Sleep -Milliseconds 100
    }
    if ($status -eq 'adb_encerrado') {
        $errorText = Get-Content -LiteralPath $stderrPath -Raw -ErrorAction SilentlyContinue
        Write-Warning "ADB encerrou. Confira erro-adb.txt. $errorText"
    }
} catch {
    $status = 'erro'
    $markers.WriteLine('Erro: ' + $_.Exception.Message)
    throw
} finally {
    if ($null -ne $capture -and !$capture.HasExited) { Stop-Process -Id $capture.Id -ErrorAction SilentlyContinue }
    if ($null -ne $reader) { $reader.Dispose() }
    $markers.WriteLine('Fim no PC: ' + [DateTimeOffset]::Now.ToString('o'))
    $markers.WriteLine("Status=$status eventos=$matched MI_RENDER=$render PQ_HDR=$metadata outros=$other")
    $log.Dispose()
    $markers.Dispose()
    # The broad system log is local scratch, not part of the report.
    Remove-Item -LiteralPath $raw -Force -ErrorAction SilentlyContinue
    $zipPath = $Saida + '.zip'
    Compress-Archive -Path (Join-Path $Saida '*.txt') -DestinationPath $zipPath -Force
    Write-Host "Relatorio: $zipPath" -ForegroundColor Green
}
