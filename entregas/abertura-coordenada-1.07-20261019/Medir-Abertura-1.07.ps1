[CmdletBinding()]
param(
    [string]$Dispositivo = 'emulator-5554',
    [string]$Adb = (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'),
    [string]$PastaSaida = (Get-Location).Path
)
$ErrorActionPreference = 'Stop'
if ($Dispositivo -notmatch '^[a-zA-Z0-9_.:-]+$') { throw 'Informe o identificador ADB do aparelho.' }
if (-not (Test-Path -LiteralPath $Adb)) { throw 'ADB nao encontrado. Informe -Adb.' }
$PastaSaida = (Resolve-Path -LiteralPath $PastaSaida).Path
$log = Join-Path $PastaSaida 'diagnostico-abertura-tempos.txt'
$err = Join-Path $PastaSaida 'diagnostico-abertura-adb.txt'
$marker = 'medicao_inicio=' + [guid]::NewGuid().ToString('N')
$reader = $null
try {
    Write-Host 'Medicao: fecharemos e reabriremos o app. Dados e cache serao preservados.'
    & $Adb -s $Dispositivo get-state
    if ($LASTEXITCODE -ne 0) { throw 'Aparelho nao conectado ao ADB.' }
    $reader = Start-Process -FilePath $Adb -ArgumentList @('-s',$Dispositivo,'logcat','-T','1','-v','brief','StartupPerf:I','IPTVStorage:I','M3uSourceSync:I','PlaylistConnection:D','*:S') -NoNewWindow -PassThru -RedirectStandardOutput $log -RedirectStandardError $err
    & $Adb -s $Dispositivo shell log -t StartupPerf $marker
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel marcar o inicio do log.' }
    & $Adb -s $Dispositivo shell am force-stop br.com.flaviotorres.iptvpremium
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel fechar o app para medir.' }
    & $Adb -s $Dispositivo shell am start -W -n br.com.flaviotorres.iptvpremium/.MainActivity
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel iniciar o app.' }
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $completed = $false
    while ($watch.Elapsed.TotalSeconds -lt 75) {
        if ($reader.HasExited) { throw 'A captura ADB terminou antes da medicao.' }
        $text = Get-Content -LiteralPath $log -Raw -ErrorAction SilentlyContinue
        if ($text) {
            $from = $text.IndexOf($marker)
            if ($from -ge 0 -and $text.Substring($from).Contains('etapa=interface_liberada')) { $completed = $true; break }
        }
        Start-Sleep -Milliseconds 500
    }
    if ($completed) { Write-Host 'Interface liberada. Confira o totalMs e os tempos por etapa no diagnostico.' }
    else { Write-Host 'A abertura nao foi confirmada em 75 segundos; o diagnostico foi preservado.' }
} finally {
    if ($reader -and -not $reader.HasExited) { Stop-Process -Id $reader.Id -ErrorAction SilentlyContinue; $reader.WaitForExit() }
    Write-Host ('Diagnostico: ' + $log)
}
