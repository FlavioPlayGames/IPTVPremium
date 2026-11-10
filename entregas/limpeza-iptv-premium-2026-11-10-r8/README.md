# IPTV Premium — r8: limpeza física e de código

Base r7. O inventário do PC mostrou 4.644.172.018 bytes no projeto: aproximadamente 4,33 GB decimais de gerados/cache e 239 MB de backups. A limpeza também retira dos fontes 24 imagens sem uso (25,6 MB), três arquivos antigos de capas e funções sem chamadas. O projeto sem gerados/backups fica estimado em cerca de 43 MB no Windows. Compilações futuras recriam intermediários.

O otimizador já retirava as imagens sem uso do APK. A redução real testada no APK veio da disponibilização do Android NDK para remover símbolos de depuração: ARM64 passou de 104.994.502 para 97.064.646 bytes. ARMv7 não teve redução relevante. As seções carregadas em memória de 20 bibliotecas nativas, incluindo tabelas de símbolos dinâmicos, permaneceram iguais. Não foi medido ganho de velocidade na TV.

As regras de capas, pesquisa, histórico, controle parental e cache rotativo foram preservadas. Cotas não aumentadas. Os backups antigos ficam compactados fora do projeto e são verificados por SHA256 antes da exclusão. Os APKs finais por arquitetura são copiados para Downloads antes de remover app/build. Configurações privadas, assinatura, Gradle/wrapper, recursos e fontes em uso são preservados.

[Baixar limpeza-iptv-premium-r8.zip](https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/limpeza-iptv-premium-2026-11-10-r8/limpeza-iptv-premium-r8.zip)

Feche o Android Studio e execute em PowerShell separado, com a TV conectada:

```powershell
$pasta = "$env:USERPROFILE\Downloads\limpeza-iptv-premium-r8"
Unblock-File -LiteralPath "$pasta.zip"
Expand-Archive -LiteralPath "$pasta.zip" -DestinationPath $pasta -Force
Set-Location $pasta
powershell -ExecutionPolicy Bypass -File .\Limpeza-Completa.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O fluxo prepara NDK, aplica a limpeza com backup externo, compila/instala desempenho com a assinatura local e só depois limpa os gerados/arquivos antigos. Se a instalação falhar, a limpeza física não acontece.

O NDK 28.2.13676358 é uma ferramenta de desenvolvimento grande, instalada fora do projeto, no SDK. O script tenta instalar pelo sdkmanager existente e pode pedir licença no terminal. Se faltar sdkmanager, orienta instalação pelo Android Studio. Para fazer a limpeza sem essa etapa agora, acrescente **-SemPrepararNdk** ao comando; as cotas e a limpeza permanecem, e a redução nativa depende da ferramenta disponível no SDK.

O pacote inclui LEIA-ME.md, VALIDACAO.md e plano explícito de caminhos. Compilação Kotlin, Lint (zero erros, 154 avisos), assembleDesempenho, regressões JVM e testes de aplicação/reversão/limpeza passaram. Scripts testados em PowerShell 7 com comportamento JSON do Windows PowerShell 5.1 também simulado; execução física no Windows/TV precisa de conferência local. Não contém APK assinado, lista M3U, binários, credenciais ou configurações privadas.

**Não publique/envie as pastas ou ZIPs de recuperação:** podem conter configurações privadas antigas. Para conferir tamanhos, envie somente tamanhos-apk.csv ou um novo inventário de metadados.
