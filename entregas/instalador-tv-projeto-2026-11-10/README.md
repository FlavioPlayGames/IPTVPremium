# Instalador IPTV Premium por projeto

Instalador independente para o formato:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

O arquivo precisa existir na pasta atual. O aplicador da revisão r2 altera os fontes Kotlin e não copia o instalador para a raiz do projeto. Para colocar este instalador lá:

```powershell
$projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
$arquivo = "$env:USERPROFILE\Downloads\Instalar-TV.ps1"
Invoke-WebRequest -UseBasicParsing -Uri "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/instalador-tv-projeto-2026-11-10/Instalar-TV.ps1" -OutFile $arquivo
if ((Get-FileHash -LiteralPath $arquivo -Algorithm SHA256).Hash.ToLowerInvariant() -ne "c7c75e9abacbb7bb85aa107753b725ad55d2f43e2b792d7c70f90105f1802e60") { throw "Instalador diferente do publicado." }
Unblock-File -LiteralPath $arquivo
$destino = Join-Path $projeto 'Instalar-TV.ps1'
if (Test-Path -LiteralPath $destino) { Copy-Item -LiteralPath $destino -Destination ($destino + '.backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')) }
Copy-Item -LiteralPath $arquivo -Destination $destino -Force
Set-Location $projeto
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto $projeto
```

## Comportamento

- Usa o ADB já instalado em LOCALAPPDATA/Android/Sdk/platform-tools, com opção -Adb para outro caminho.
- Consulta os aparelhos conectados/autorizados. Prioriza o modelo Smart_TV_Pro / product G08_4K_GB / device G08 informado nas orientações; havendo apenas um aparelho autorizado, usa esse aparelho. Se houver ambiguidade, exige -Serial em vez de escolher arbitrariamente.
- Não presume IP nem porta e não altera pareamento.
- Consulta as ABIs da TV, com fallback para a propriedade ABI singular.
- Compila `:app:assembleDesempenho` com o Gradle wrapper e a assinatura já configurada no projeto local. -SemCompilar permite usar os APKs já gerados.
- Escolhe o APK desempenho compatível ou o universal e instala com `adb install -r`.
- Interrompe se o build falhar ou a TV não estiver autorizada. Não desinstala nem limpa dados para contornar uma recusa de assinatura.

Aceita -Projeto, -Serial (alias -Tv), -PastaApks, -Adb (alias -AdbPath) e -SemCompilar.

## Validação

Executados oito cenários em PowerShell 7 no Linux, com ADB e Gradle simulados: descoberta automática; escolha da TV conhecida entre vários aparelhos; recusa de TVs ambíguas; recusa de aparelho não autorizado; falha de build sem instalação; recusa de assinatura sem uninstall/clear; fallback universal; ABI de 32 bits com APK já compilado. Todos passaram.

Não foi executado Windows PowerShell 5.1 nativo nem instalação em TV real no ambiente remoto. O teste físico será feito no computador do usuário, com sua conexão ADB e assinatura existentes.
