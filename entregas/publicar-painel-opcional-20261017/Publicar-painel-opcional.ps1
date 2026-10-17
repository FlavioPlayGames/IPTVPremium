param([string]$PastaDestino = [Environment]::GetFolderPath('Desktop'))
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $PastaDestino -PathType Container)) { throw 'Pasta de destino nao encontrada. Informe -PastaDestino com uma pasta existente.' }
foreach ($command in @('node','npm.cmd','npx.cmd')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw ('Comando nao encontrado: ' + $command + '. Instale o Node.js 22 ou superior e reabra o PowerShell.') }
}
$nodeVersion = (& node --version | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch '^v(\d+)\.' -or [int]$matches[1] -lt 22) { throw 'Este pacote precisa do Node.js 22 ou superior.' }
$container = Join-Path $PastaDestino ('Central-IPTV-publicacao-opcional-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Path $container | Out-Null
$preparer = Join-Path $container 'Preparar-central-opcional.ps1'
$url = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/c1756420709015ef7be1149e410f2e3cee5aee88/entregas/atualizacao-opcional-1.03-20261017/Preparar-central-opcional.ps1'
Invoke-WebRequest -UseBasicParsing $url -OutFile $preparer
if ((Get-FileHash -LiteralPath $preparer -Algorithm SHA256).Hash.ToLowerInvariant() -ne '825f6ff17bba5683d5c713bf5e9ca760f90bb436988e48d53d7c351911f006d6') { throw 'Pacote incorreto. Nada foi publicado.' }
$executor = (Get-Process -Id $PID).Path
& $executor -NoProfile -ExecutionPolicy Bypass -File $preparer -PastaDestino $container
if ($LASTEXITCODE -ne 0) { throw 'Preparacao falhou. Nada foi publicado. Envie o erro.' }
$folders = @(Get-ChildItem -LiteralPath $container -Directory | Where-Object { $_.Name -like 'Central-IPTV-opcional-*' })
if ($folders.Count -ne 1) { throw 'Pasta preparada nao identificada. Nada foi publicado.' }
$site = $folders[0].FullName
Push-Location $site
try {
    & npm.cmd install
    if ($LASTEXITCODE -ne 0) { throw 'Instalacao das ferramentas falhou. Nada foi publicado. Envie o erro.' }
    & npm.cmd run check
    if ($LASTEXITCODE -ne 0) { throw 'Conferencia do Worker falhou. Nada foi publicado. Envie o erro.' }
    Write-Host 'Conferencia concluida. Publicando o painel opcional no Worker existente central-iptv-premium...'
    & npx.cmd wrangler deploy --config .\wrangler.jsonc
    if ($LASTEXITCODE -ne 0) { throw ('A publicacao nao foi confirmada. Pasta preservada: ' + $site + '. Envie a saida completa do Wrangler.') }
    Write-Host ('Painel opcional publicado. Pasta local: ' + $site)
    Write-Host 'Abra o painel e pressione Ctrl+F5. O titulo deve ser Atualizacao opcional e o botao Publicar atualizacao.'
    Write-Host 'Depois cadastre os links dos APKs 1.03, numero interno 5. Nao e preciso publicar e depois suspender.'
} finally { Pop-Location }
