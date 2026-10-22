# Limpeza das imagens marcadas em vermelho

Base: `java.zip` restaurado enviado em 22/10/2026. Esta entrega não altera importação, banco, lotes Xtream ou cache. Não inclui o experimento em memória.

## Removidas de drawable-nodpi

Lista exata em `remover.txt`: 32 nomes, independentemente de estarem presentes. Dez home_approved (cinco ícones × claro/escuro); kids_bear; marble_home_black e marble_home_white (cenas completas antigas); movie_session_loading_bg; nove preview_poster; cinco standard_home; quatro theme_*_background (azul, rosa, roxo e vermelho).

Preservadas: todas as home_original_*, marble_home_layer_black/white, marble_wallpaper_black/white, logotipos dos temas, marca dourada, imagens Kids não marcadas, actor_cinematic_background e series_living_room. O script não usa padrões amplos para decidir exclusões.

**A pedido do usuário, esta revisão também remove todos os conjuntos convencionais de testes automatizados do módulo app:** `app/src/test`, `app/src/androidTest`, `app/src/testFixtures` e variantes como `testDebug`/`androidTestDebug`. As pastas são movidas inteiras para `.image-cleanup-backups/<identificador>/app/src`, fora dos fontes compilados. Nenhum teste é substituído ou recriado. Isso inclui os testes antigos que referenciavam imagens removidas. O instalador verifica as referências no código que permanecerá no aplicativo; uma referência desconhecida em produção continua bloqueando toda a operação antes de qualquer alteração.

Não altera configurações Gradle nem dependências de teste declaradas. Não remove diagnósticos de produção ou funções das telas. Para preparar um ZIP sem testes e sem as imagens antigas, exclua a pasta de backup do ZIP.

Algumas imagens marcadas ainda eram citadas no código. Foram ajustados cinco arquivos:

- ThemeArtwork: referências aos fundos antigos apontam para mármore mantido; remove código de recorte das antigas standard_home.
- MarbleArtwork: usa marble_home_layer para referências à cena antiga; remove o tratamento exclusivo da imagem antiga que deixa de existir.
- KidsScreen: reutiliza o recorte do ursinho das home_original_*, respeitando cor e modo claro/escuro, sem o PNG kids_bear.
- AppNavigation: carregamento de filmes Kids usa a própria Home Kids desfocada, selecionando rosa ou azul em vez de movie_session_loading_bg. A imagem é parada: o bitmap desfocado é calculado fora da interface e reutilizado em cache; não existe animação do fundo ou processamento por frame. Usa os fundos kids_home_pink/blue mantidos, sem duplicar imagens no APK.
- CatalogLayoutPreview: capas estáticas removidas dão lugar a blocos numerados na prévia de configuração. Não altera as capas reais do catálogo.

## Executar

Feche o Android Studio se houver arquivo de build bloqueado. Execute no PowerShell:

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/limpeza-imagens-marcadas-20261022'
$script=Join-Path $env:TEMP 'Limpar-Imagens-Marcadas.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Limpar-Imagens-Marcadas.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O script baixa somente as correções de código e a lista de exclusões. Não baixa imagens antigas nem recria imagens já excluídas. Aceita arquivo do código enviado ou já corrigido; verifica hashes e procura referências restantes nos arquivos .kt, .java e .xml que permanecerão em app/src. Se encontrar referência desconhecida ou arquivo alterado, interrompe antes de escrever.

Depois aplica o código e move somente as imagens listadas de `app/src/main/res/drawable-nodpi` para `.image-cleanup-backups/<identificador>/drawable-nodpi`. Faz backup do código na mesma pasta. Imagens já ausentes são ignoradas. Para preparar um ZIP menor do projeto, exclua `.image-cleanup-backups` do ZIP; esse backup fica fora dos recursos e não entra no APK.

`-Testar` executa assembleDebug com cache de configuração desabilitado. Não instala automaticamente. Depois de BUILD SUCCESSFUL, gere/instale seu APK normalmente, escolhendo o arquivo real produzido pelo projeto. Não pressupomos o nome app-debug.apk, pois seu projeto pode gerar APKs separados por arquitetura.

A versão do projeto permanece como foi enviada. A limpeza não troca seu modelo de importação restaurado.

## Verificação

Na estrutura Android de verificação, 31 dos nomes estavam presentes, somando 33.995.565 bytes (32,4 MiB); isso não é uma estimativa do tamanho final do APK, e a economia no seu projeto depende dos arquivos existentes.

Veja `verificacao.txt`. Não foi feita validação visual no seu aparelho; confira Home, Kids e prévia de layout após instalar. O ZIP enviado contém fontes Kotlin, não seus recursos atuais completos; a compilação foi verificada com os recursos disponíveis na estrutura Android local, removendo as imagens marcadas.
