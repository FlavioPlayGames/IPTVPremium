# Home: ícones 30% menores e abertura

Altera somente três arquivos da Home sobre o `java.zip` restaurado. Os cinco ícones principais passam a 70% da largura e altura anteriores, preservando a base e o centro. Nomes, áreas de clique, navegação pelo controle, logo e botões de atualizar/configurações permanecem nas posições anteriores. A mesma geometria vale para Dourado, Azul, Rosa, Roxo e Vermelho; imagens e filtros dos temas são preservados.

Na abertura, a Home distingue carregamento de falha. Usa imediatamente imagens já disponíveis no cache existente; durante a leitura mostra o fundo assim que disponível e aguarda os cinco ícones antes de liberar o menu. O antigo menu cinza com destinos em texto foi removido. Se os recursos não carregarem após três tentativas, oferece tentar novamente. Sair da tela cancela o trabalho pendente.

## Aplicação

Execute `Atualizar-Home.ps1 -Projeto CAMINHO_DO_PROJETO` no PowerShell. O instalador verifica todos os arquivos antes de gravar, cria backup em `.navigation-backups/home-icones70-inicio-*` e recusa versões diferentes da base para evitar sobrescrever modificações. A reaplicação não cria alterações. Não substitui recursos, Gradle ou arquivos de assinatura.

Depois execute `./gradlew assembleDebug` (Windows: `.\gradlew.bat assembleDebug`).

## Validação e limites

Testes Robolectric/Compose cobrem geometria, abertura assíncrona, carregamento do fundo antes dos ícones, controle direcional, tentativas limitadas, recuperação e cancelamento. O instalador foi verificado com LF e CRLF/BOM, reaplicação, recusa de fonte modificada e restauração após falha de escrita simulada.

O `res.zip` enviado ultrapassou o limite de transferência de 32 MB deste ambiente. A compilação de testes utilizou aliases locais temporários para os recursos ausentes da cópia de trabalho e imagens simuladas nos testes; esses aliases não integram esta atualização. Não foi feita validação visual com as novas artes nem em TV física. Os recursos existentes no projeto do usuário são mantidos.

Para executar os testes no projeto completo, sem modificar o Gradle do aplicativo:

```powershell
.\gradlew.bat -I caminho\testes\validar-home70.gradle :app:testDebugUnitTest --tests '*MarbleHomeStartupHostTest'
```

A classificação publicada anteriormente permanece intacta. Cache de capas, importação, favoritos, catálogos, canais e player não são alterados.
