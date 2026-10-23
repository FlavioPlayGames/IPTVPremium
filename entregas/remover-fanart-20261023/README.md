# Remover Fanart.tv

Feito sobre os arquivos atualizados enviados em 23/10/2026. Mantém a versão configurada no projeto.

- Remove as consultas e alternativas do Fanart.tv para filmes e séries, o cliente específico de filmes e o campo `FANART_API_KEY` gerado pelo Gradle.
- Remove os links para o site, que podiam abrir uma página durante a navegação pelos detalhes.
- Mantém os dados do provedor, TMDB, TVmaze e o carregamento de imagens em cache. Quando não existe logo, mostra o título; sem fundo disponível, usa a alternativa genérica já existente.
- TVmaze continua para fundos e imagens de episódios, com as verificações de identidade e temporada. Não o consulta apenas porque está faltando uma logo.
- Limpa somente a propriedade `FANART_API_KEY` do `local.properties` do seu computador. As demais configurações são preservadas e o arquivo original fica no backup local. **Nenhum local.properties, token ou google-services.json está incluído nesta entrega.**

## Aplicar pelo PowerShell

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/remover-fanart-20261023'
$script = Join-Path $env:TEMP 'Preparar-Remover-Fanart.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Remover-Fanart.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O instalador valida os fontes e `app/build.gradle.kts` antes de qualquer escrita. Alterações incompatíveis fazem a instalação parar sem sobrescrever. Cria backup em `.remove-artwork-service-backups`, fora de `app/src`, inclusive do auxiliar removido e da configuração local quando houver mudança. Depois instale o APK gerado sobre o aplicativo existente.

A entrega modifica apenas os arquivos relacionados ao serviço removido; usa o projeto enviado como referência e preserva as demais alterações que você fez. Não adiciona testes ao APK. Verificações em `VERIFICACAO.txt`; não houve teste físico na TV.
