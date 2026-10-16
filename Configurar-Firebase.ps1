param(
    [Parameter(Mandatory=$true)][string]$GoogleServicesPath,
    [Parameter(Mandatory=$true)][string]$ServiceAccountPath,
    [string]$SitePath = (Join-Path $env:USERPROFILE "Downloads\Central-IPTV-Premium-site\site")
)
$ErrorActionPreference = "Stop"
$base = (Get-Location).Path
if (-not (Test-Path -LiteralPath (Join-Path $base "app\build.gradle.kts"))) { throw "Abra o PowerShell na raiz do projeto Android." }
$site = (Resolve-Path -LiteralPath $SitePath).Path
$config = [IO.File]::ReadAllText((Join-Path $site "wrangler.jsonc")) | ConvertFrom-Json
if (-not $config.d1_databases[0].database_id -or $config.d1_databases[0].database_id -eq "SUBSTITUIR_PELO_ID_DO_BANCO") { throw "Use a pasta original do site publicado." }
$clientPath = (Resolve-Path -LiteralPath $GoogleServicesPath).Path
$privatePath = (Resolve-Path -LiteralPath $ServiceAccountPath).Path
$client = [IO.File]::ReadAllText($clientPath) | ConvertFrom-Json
$serviceRaw = [IO.File]::ReadAllText($privatePath)
$service = $serviceRaw | ConvertFrom-Json
$app = @($client.client | Where-Object { $_.client_info.android_client_info.package_name -eq "br.com.flaviotorres.iptvpremium" })
if ($app.Count -ne 1 -or -not $client.project_info.project_id -or -not $app[0].api_key[0].current_key) { throw "google-services.json nao corresponde ao aplicativo IPTV Premium." }
if ($service.type -ne "service_account" -or -not $service.private_key -or -not $service.client_email -or $service.project_id -ne $client.project_info.project_id) { throw "A conta de servico deve pertencer ao mesmo projeto Firebase do aplicativo." }
if ($privatePath.StartsWith((Join-Path $site "public") + "\", [StringComparison]::OrdinalIgnoreCase) -or $privatePath.StartsWith((Join-Path $base "app") + "\", [StringComparison]::OrdinalIgnoreCase)) { throw "Mova a chave privada para uma pasta privada fora de app e public antes de continuar." }
$wrangler = Join-Path $site "node_modules\.bin\wrangler.cmd"
if (-not (Test-Path -LiteralPath $wrangler)) { throw "Execute primeiro Implementar-Firebase-listas.ps1 para preparar e publicar o site." }
Push-Location $site
try {
    # Never echo the private JSON or put it in a command-line argument/file in the site.
    $serviceRaw | & $wrangler secret put FCM_SERVICE_ACCOUNT
    if ($LASTEXITCODE -ne 0) { throw "Nao foi possivel salvar o segredo Firebase na Cloudflare. Nenhuma configuracao Android foi substituida." }
} finally { Pop-Location; $serviceRaw = $null; $service = $null }
$destination = Join-Path $base "app\google-services.json"
if (Test-Path -LiteralPath $destination) { Copy-Item -LiteralPath $destination -Destination ($destination + ".backup-" + (Get-Date -Format "yyyyMMdd-HHmmssfff")) }
if ($clientPath -ne $destination) { Copy-Item -LiteralPath $clientPath -Destination $destination }
Write-Host "Firebase configurado no Android e segredo enviado a Cloudflare." -ForegroundColor Green
Write-Host "Agora compile e instale um novo APK com a assinatura original. Ao abrir o aplicativo ele registra o token automaticamente." -ForegroundColor Yellow
Write-Host "Teste com o aplicativo aberto: enviar uma lista no site deve iniciar o carregamento. Confira depois Recebido pelo aplicativo." -ForegroundColor Yellow
