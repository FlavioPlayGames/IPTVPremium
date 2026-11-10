> **Substituído pela [revisão r3](../continuacao-iptv-premium-2026-11-10-r3/README.md)** para as correções adicionais de capas M3U e Continuar.

# IPTV Premium — pacote r2 (aplicador corrigido)

[Baixar continuacao-iptv-premium-r2.zip](./continuacao-iptv-premium-r2.zip?raw=true)

Esta revisão corrige o tratamento do array JSON pelo Aplicar.ps1 no Windows PowerShell 5.1. A falha anterior acontecia no Join-Path, antes de copiar os fontes. Também foi acrescentada validação de tipo para cada caminho na aplicação e reversão.

Os sete arquivos Kotlin e os hashes da base continuam iguais aos da primeira entrega. Compilação Kotlin/Java e Lint desempenho já passaram nesses mesmos fontes. Nenhum APK foi incluído e a assinatura existente foi preservada.

Testes executados: regressão do formato de saída JSON do Windows PowerShell 5.1 simulada em PowerShell 7; aplicação, reaplicação, reversão e proteções de hashes passaram. Não houve execução nativa de Windows PowerShell 5.1 neste ambiente.

## Baixar e aplicar no PowerShell

```powershell
$zip = "$env:USERPROFILE\Downloads\continuacao-iptv-premium-r2.zip"
$pasta = "$env:USERPROFILE\Downloads\continuacao-iptv-premium-r2"
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/continuacao-iptv-premium-2026-11-10-r2/continuacao-iptv-premium-r2.zip"
Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $zip
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne "f60a936be9f3f930f2e4a554b204553c440f23a2a4833027359ea4484c79e446") { throw "ZIP diferente do publicado." }
Unblock-File -LiteralPath $zip
Expand-Archive -LiteralPath $zip -DestinationPath $pasta -Force
& "$pasta\Aplicar.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

Depois da aplicação bem-sucedida:

```powershell
Set-Location "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
& .\gradlew.bat :app:compileDesempenhoKotlin :app:lintDesempenho :app:assembleDesempenho
if ($LASTEXITCODE -ne 0) { throw "O Gradle informou uma falha. Confira a saida antes de instalar." }
```

O aplicador mostra o caminho do backup e o comando de reversão. O pacote contém LEIA-ME.md, VALIDACAO.md e Instalar-TV.ps1 para a instalação posterior com sua assinatura local e preservação de dados.
