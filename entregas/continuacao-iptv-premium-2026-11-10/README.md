# IPTV Premium — correções de 10/11/2026

[Baixar continuacao-iptv-premium.zip](./continuacao-iptv-premium.zip?raw=true)

Pacote de alterações localizadas: capas M3U compartilhadas com o filme 13 Horas; persistência do próximo episódio no Continuar; identidade da capa da temporada no Continuar normal/Kids; retorno à Home após atualização manual.

Compilação Kotlin/Java e Lint da variante desempenho passaram. Lint: zero erros, 176 avisos e seis sugestões. Os testes do parser consumiram a lista completa fornecida e preservaram os 329.991 itens; a comparação alterou apenas a coluna de capa de 514 séries. Testes isolados de histórico, cache em disco e aplicação/reversão também passaram. Os limites estão detalhados em VALIDACAO.md dentro do ZIP.

O ZIP contém Aplicar.ps1, Desfazer.ps1, Instalar-TV.ps1, instruções e testes. Não contém APK nem credenciais. Instalação física na TV e integração real de legendas com o site continuam pendentes. Versão/pacote/assinatura da base não foram alterados.

## Aplicar a partir de Downloads

No PowerShell:

```powershell
$zip = "$env:USERPROFILE\Downloads\continuacao-iptv-premium.zip"
$pasta = "$env:USERPROFILE\Downloads\continuacao-iptv-premium"
$hashEsperado = "439bc0be19c3631e6f42def070cc3c1cd4e16673ed0efab4001aae5b21cea0a2"
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hashEsperado) { throw "O ZIP nao corresponde ao pacote publicado." }
Unblock-File -LiteralPath $zip
Expand-Archive -LiteralPath $zip -DestinationPath $pasta -Force
& "$pasta\Aplicar.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O aplicador confere a base e cria backup. Se ele recusar algum arquivo, não force a substituição; a base mudou e precisa ser conferida. O comando exato de reversão é mostrado após a aplicação.

Para compilar na máquina que possui sua assinatura existente, depois da aplicação bem-sucedida:

```powershell
Set-Location "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
& .\gradlew.bat :app:compileDesempenhoKotlin :app:lintDesempenho :app:assembleDesempenho
if ($LASTEXITCODE -ne 0) { throw "O Gradle informou uma falha. Confira a saida antes de instalar." }
```

O procedimento de instalação está no LEIA-ME.md do pacote. O instalador consulta a arquitetura da TV e usa adb install -r. Preserve a assinatura já usada no seu computador; não desinstale o aplicativo para contornar incompatibilidade de assinatura.
