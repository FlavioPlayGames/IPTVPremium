param([string]$AdbPath, [string]$Serial, [string]$PastaSaida, [switch]$Autoteste)
$ErrorActionPreference = 'Stop'
function Ocultar-Dados([string]$linha) {
    $linha = [regex]::Replace($linha, '(?i)\bhttps?://[^\s"''<>]+', '[URL omitida]')
    return [regex]::Replace($linha, '(?i)\b(token|secret|password|passwd|username|authorization|api[_-]?key|pin|registration[_-]?token|fcm[_-]?token|access[_-]?token)\b(\s*[:=]\s*)(?:"[^"]*"|''[^'']*''|[^\s,;}]+)', '$1$2[omitido]')
}
if ($Autoteste) {
    $amostras = @(
        'GET https://servidor.invalid/get.php?username=USUARIO_PRIVADO&password=SENHA_PRIVADA http=403',
        'token=TOKEN_PRIVADO secret="SEGREDO PRIVADO" PIN:1234',
        'password=''SENHA PRIVADA'' username=USUARIO_PRIVADO',
        'Authorization=Bearer token=TOKEN_PRIVADO'
    )
    foreach ($amostra in $amostras) {
        $limpa = Ocultar-Dados $amostra
        if ($limpa -match 'USUARIO_PRIVADO|SENHA_PRIVADA|SENHA PRIVADA|TOKEN_PRIVADO|SEGREDO PRIVADO|1234') { throw 'Falha na protecao dos dados.' }
    }
    $publica = 'PortalPush: Aviso de listas recebido. PlaylistConnection: http=403 erro=UnknownHostException premium.dnsvip.net.br'
    if ((Ocultar-Dados $publica) -ne $publica) { throw 'O filtro removeu diagnostico necessario.' }
    Write-Host 'Autoteste concluido: dados privados omitidos; etapas, dominio publico e erros preservados.'
    return
}
if (-not $AdbPath) {
    $candidatos = @()
    if ($env:ANDROID_HOME) { $candidatos += (Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe') }
    if ($env:LOCALAPPDATA) { $candidatos += (Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe') }
    $AdbPath = $candidatos | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $AdbPath) { $comando = Get-Command adb -ErrorAction SilentlyContinue; if ($comando) { $AdbPath = $comando.Source } }
}
if (-not $AdbPath -or -not (Test-Path -LiteralPath $AdbPath)) { throw 'ADB nao encontrado. Execute novamente com -AdbPath e o caminho de platform-tools/adb.exe.' }
$dispositivos = @(& $AdbPath devices)
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel consultar os dispositivos ADB.' }
$conectados = @($dispositivos | Where-Object { $_ -match '^\S+\s+device\s*$' } | ForEach-Object { ($_ -split '\s+')[0] })
if (-not $Serial) {
    if ($conectados.Count -ne 1) { throw 'Deixe somente um emulador/aparelho conectado, ou informe -Serial. Nenhuma configuracao foi alterada.' }
    $Serial = $conectados[0]
}
if ($conectados -notcontains $Serial) { throw 'O aparelho selecionado nao esta conectado e autorizado no ADB.' }
$alvo = @('-s', $Serial)
$processos = ((& $AdbPath @alvo shell pidof br.com.flaviotorres.iptvpremium) -join ' ').Trim()
if ($LASTEXITCODE -ne 0 -or $processos -notmatch '^\d+(?:\s+\d+)*$') { throw 'Abra o IPTV Premium no emulador e execute este auxiliar novamente.' }
$processo = ($processos -split '\s+')[0]
if (-not $PastaSaida) { $PastaSaida = [Environment]::GetFolderPath('Desktop'); if (-not $PastaSaida) { $PastaSaida = [IO.Path]::GetTempPath() } }
[void][IO.Directory]::CreateDirectory($PastaSaida)
$arquivo = Join-Path $PastaSaida ('Diagnostico-envio-IPTV-' + (Get-Date -Format 'yyyyMMdd-HHmmssfff') + '.txt')
$utf8 = New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText($arquivo, "Diagnostico de recebimento e carga IPTV Premium`r`nInicio local: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`r`nCaptura somente do processo atual do aplicativo; dados de URL e credenciais omitidos.`r`n", $utf8)
$versao = @(& $AdbPath @alvo shell dumpsys package br.com.flaviotorres.iptvpremium) | Where-Object { $_ -match '^\s*version(?:Code|Name)=' }
foreach ($linha in $versao) { [IO.File]::AppendAllText($arquivo, (Ocultar-Dados $linha.Trim()) + "`r`n", $utf8) }
Write-Host "Arquivo: $arquivo" -ForegroundColor Cyan
Write-Host '1. Mantenha o aplicativo aberto. Envie UMA lista pelo site.'
Write-Host '2. Aguarde cerca de 30 segundos sem sincronizar manualmente.'
Write-Host '3. Se nao carregar, clique em Sincronizar uma vez para comparar os dois caminhos.'
Write-Host '4. Apos aparecer o resultado/erro, pressione Ctrl+C aqui e envie o arquivo TXT.'
Write-Host 'Nao feche nem reinicie o aplicativo durante a captura. O auxiliar nao altera dados, rede ou versao.'
$filtros = @('PortalPush:D','PlaylistConnection:D','M3uSourceSync:D','M3uImportPerf:D','XtreamCatalogSync:D','XtreamCatalogParser:D','CacheGeneration:D','IptvAutoSync:D','NavigationPerf:D','AndroidRuntime:E','FirebaseMessaging:W','*:S')
try {
    & $AdbPath @alvo logcat --pid=$processo -v threadtime -T 1 @filtros | ForEach-Object {
        $linha = Ocultar-Dados ([string]$_)
        [IO.File]::AppendAllText($arquivo, $linha + "`r`n", $utf8)
        Write-Host $linha
    }
    if ($LASTEXITCODE -ne 0) { throw 'A captura ADB foi interrompida. O que foi coletado permanece no arquivo.' }
} finally { Write-Host "Relatorio salvo: $arquivo" -ForegroundColor Green }
