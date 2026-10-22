# Correção KidsPolicy

Elimina a dependência da classificação Kids em ui.settings.AdultChannelMarker, mantendo localmente os mesmos marcadores de conteúdo adulto e normalização. Não altera o controle parental central. Remove priority sem chamadas no código enviado, simplifica [S]? para S? e usa maxOfOrNull para extrair o ano.

Modifica somente `app/src/main/java/br/com/flaviotorres/iptvpremium/ui/kids/KidsPolicy.kt`, com conferência do conteúdo anterior e backup. Não cria testes no aplicativo nem altera imagens ou importação.

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/correcao-kids-policy-20261022'
$script=Join-Path $env:TEMP 'Corrigir-KidsPolicy.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Corrigir-KidsPolicy.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Alternativa: substitua manualmente o conteúdo do arquivo por KidsPolicy.kt desta pasta. Não mova para outro pacote.

Validação: compilação JVM do arquivo corrigido e checks locais de classificação, exclusão de nomes adultos, expressões de legendados e ano; nenhum arquivo de teste é instalado no projeto. Não executado na TV.
