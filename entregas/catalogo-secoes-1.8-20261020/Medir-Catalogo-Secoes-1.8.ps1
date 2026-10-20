param(
    [string]$Dispositivo = '',
    [string]$PastaSaida = (Get-Location).Path,
    [ValidateSet('M3U','XTREAM','Automatico')][string]$Formato = 'Automatico',
    [ValidateRange(30,900)][int]$TempoMaximoSegundos = 600,
    [string]$AdbPath = '',
    [ValidateSet('PADRAO','ATUAL','WAL','NORMAL','LOTES','XTREAM_1000','XTREAM_500','XTREAM_650')][string]$Perfil = 'PADRAO',
    [switch]$AbrirAplicativo
)
$ErrorActionPreference = 'Stop'
if (-not $AdbPath) {
    $AdbPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $AdbPath)) {
        $found = Get-Command adb -ErrorAction SilentlyContinue
        if ($found) { $AdbPath = $found.Source }
    }
}
if (-not (Test-Path -LiteralPath $AdbPath)) { throw 'ADB nao encontrado. Informe -AdbPath.' }
$devices = @(& $AdbPath devices | ForEach-Object { if ($_ -match '^(\S+)\s+device$') { $Matches[1] } })
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel consultar os aparelhos.' }
if (-not $Dispositivo) {
    if ($devices.Count -ne 1) { throw 'Informe -Dispositivo com o serial exibido por adb devices.' }
    $Dispositivo = $devices[0]
}
if ($Dispositivo -notin $devices -or $Dispositivo -notmatch '^[A-Za-z0-9_.:-]+$') { throw 'Aparelho nao conectado ou serial invalido.' }
New-Item -ItemType Directory -Force -Path $PastaSaida | Out-Null
$reportPath = Join-Path $PastaSaida 'diagnostico-catalogo-secoes.txt'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('iptv-import-' + [guid]::NewGuid().ToString('N'))
$stdout = $temporary + '.out'
$stderr = $temporary + '.err'
$process = $null
$captured = @()
function Read-Metrics {
    if (Test-Path -LiteralPath $stdout) { return @(Get-Content -LiteralPath $stdout -ErrorAction SilentlyContinue) }
    return @()
}
function Safe-Metrics([string[]]$Lines) {
    foreach ($line in $Lines) {
        if ($line -notmatch '\b(SectionCatalog|DbPerf|XtreamEndpointPerf|M3uImportPerf|M3uSourceSync|PlaylistConnection|StartupPerf)\s*:\s*(.*)$') { continue }
        $tag = $Matches[1]; $body = $Matches[2]
        $fields = [Collections.Generic.List[string]]::new()
        # Rebuild a strict allowlist of metrics. No URLs, exception texts or credentials.
        foreach ($match in [regex]::Matches($body, '\b([A-Za-z][A-Za-z0-9]*)=([^\s]+)')) {
            $key=$match.Groups[1].Value; $value=$match.Groups[2].Value
            if ($key -cin @('pendentes','duracaoMs','totalMs','tempoMs','tempoTotalMs','gravacaoRoomMs','redeEParsingMs','parsingMs','gravacaoMs','lotes','gravacaoPicoMs','aceitos','entradas','canais','filmes','episodios','seriesDistintas','categoriasDistintas','bytesRede','bytesTexto','heapKb','heapPicoObservadoKb','heapAntesKb','heapDepoisKb','synchronous','lote','limiteCaracteres','loteM3u','limiteCaracteresM3u') -and $value -match '^\d+$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'secao' -and $value -cmatch '^(LIVE_TV|MOVIES|SERIES)$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'perfil' -and $value -cmatch '^(PADRAO|ATUAL|WAL|NORMAL|LOTES|XTREAM_1000|XTREAM_500|XTREAM_650)$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'journal' -and $value -cmatch '^(wal|truncate|delete|persist|memory|off)$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'revisao' -and $value -cmatch '^db-r[12]$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'endpoint' -and $value -cmatch '^(authenticate|get_live_categories|get_live_streams|get_vod_categories|get_vod_streams|get_series_categories|get_series|XTREAM_AUTH|XTREAM_DATA|M3U_GET)$') { $fields.Add($key+'='+$value) }
            elseif ($key -cin @('tipo','formato','origem','destino') -and $value -cmatch '^(XTREAM|M3U)$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'codificacao' -and $value -cmatch '^(gzip|identity|br)$') { $fields.Add($key+'='+$value) }
            elseif ($key -cin @('resultado','status') -and $value -cmatch '^(SUCESSO|ERRO|FALHA|CANCELADA|CANCELADO|OK|PRONTA)$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'modo' -and $value -cmatch '^(STREAM|LEGACY)$') { $fields.Add($key+'='+$value) }
            elseif ($key -ceq 'etapa' -and $value -cmatch '^(XTREAM_TOTAL|M3U_NATIVA|catalogo_importacao|catalogo_finalizacao|abertura_concluida|PRIORIDADE|HOME_LIBERADA)$') { $fields.Add($key+'='+$value) }
        }
        if ($fields.Count -gt 0) {
            $prefix=''
            if ($line -match '^(\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d+\s+\d+\s+\d+)') { $prefix=$Matches[1]+' ' }
            $marker = if ($body -match '\bCATALOGO_RESUMO\b') { 'CATALOGO_RESUMO ' } elseif ($body -match '\bSYNC_RESUMO\b') { 'SYNC_RESUMO ' } elseif ($body -match '\bIMPORT_ALTERNATIVA\b') { 'IMPORT_ALTERNATIVA ' } elseif ($body -match '\bCOMPACTACAO\b') { 'COMPACTACAO ' } else { '' }
            $prefix+$tag+': '+$marker+($fields -join ' ')
        }
    }
}
# Exclude the last historical line that logcat -T 1 can replay.
$baseline = @(& $AdbPath -s $Dispositivo logcat -d -t 1 -v threadtime -s 'SectionCatalog:I' 'XtreamEndpointPerf:I' 'M3uImportPerf:I' 'M3uSourceSync:I' 'PlaylistConnection:D' 'StartupPerf:I' 'DbPerf:I' '*:S')
$state = 'CAPTURA_INCOMPLETA'
try {
    $arguments = @('-s',$Dispositivo,'logcat','-v','threadtime','-T','1','-s','SectionCatalog:I','XtreamEndpointPerf:I','M3uImportPerf:I','M3uSourceSync:I','PlaylistConnection:D','StartupPerf:I','DbPerf:I','*:S')
    $process = Start-Process -FilePath $AdbPath -ArgumentList $arguments -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru -NoNewWindow
    if ($AbrirAplicativo) {
        Start-Sleep -Milliseconds 500
        & $AdbPath -s $Dispositivo shell am start -W -n 'br.com.flaviotorres.iptvpremium/.MainActivity' | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel abrir o aplicativo.' }
    }
    Write-Host 'Captura iniciada. Agora envie UMA lista nova e entre primeiro na secao que deseja testar.'
    Write-Host 'Nao apague os dados. Uma lista ja importada abre do cache e nao mede a primeira importacao.'
    $deadline = [DateTime]::UtcNow.AddSeconds($TempoMaximoSegundos)
    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Milliseconds 500
        $captured = @(Read-Metrics | Where-Object { $_ -notin $baseline })
        if ($captured -match '\bCATALOGO_RESUMO\s+.*resultado=(SUCESSO|ERRO|CANCELADA)\b') { $state='IMPORTACAO_TERMINADA'; break }
        if ($process.HasExited) { $state='CAPTURA_ENCERRADA'; break }
    }
    if ($state -eq 'CAPTURA_INCOMPLETA') { $state='PRAZO_DE_CAPTURA_ATINGIDO' }
} finally {
    if ($process -and -not $process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue; $process.WaitForExit() }
    $captured = @(Read-Metrics | Where-Object { $_ -notin $baseline })
    $safe = @(Safe-Metrics $captured)
    @('IPTVPremium 1.8 - teste do catalogo por secoes',('Rotulo solicitado: '+$Formato),('Perfil solicitado: '+$Perfil),('Estado: '+$state),('Gerado em UTC: '+[DateTime]::UtcNow.ToString('o')),'Compare abertura_concluida (Home), secao PRONTA e CATALOGO_RESUMO (total).','Gravacao inclui espera pelo banco e transacao; restante nao e download puro.','Heap registrado e Java, nao representa RAM total do aplicativo.','---') + $safe | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Remove-Item -LiteralPath $stdout,$stderr -Force -ErrorAction SilentlyContinue
    Write-Host ('Diagnostico salvo: '+$reportPath)
    if ($safe.Count -eq 0) { Write-Host 'Sem metricas: a captura precisa comecar antes da importacao, com o app atualizado.' }
}
