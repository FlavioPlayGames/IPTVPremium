# Kids, catálogos, PIN de 6 dígitos e capas 5/35

Base exata: **IPTVPremium (11).zip** enviado pelo usuário. Aplicação pelo PowerShell na raiz do projeto Android Studio:

```powershell
& "$env:TEMP\Atualizar-Kids.ps1" -Projeto (Get-Location).Path
.\gradlew.bat assembleDebug
```

O comando de download com a revisão publicada é fornecido na entrega. O script contém todos os arquivos necessários, inclusive o urso; não depende de baixar arquivos separados. Verifica a versão dos arquivos antes de escrever, aceita LF/CRLF e BOM, verifica o PNG por bytes, cria backup em `.navigation-backups/kids-catalogos-pin6-*` e restaura os originais se uma gravação falhar. Pode ser reaplicado. Uma base diferente é recusada sem sobrescrever mudanças desconhecidas.

## Comportamento

- Home com **Kids** e urso baseado na imagem fornecida, com acentos acompanhando os sete temas. Kids abre Canais, Filmes e Séries com os mesmos componentes de catálogo. Controle remoto e Esc retornam pela navegação normal.
- Filtra as categorias identificadas pela lista como Kids, Infantil, Crianças, Desenhos, Cartoons, Família e equivalentes. Categorias não identificadas ficam fora; Animes, Animação e marcas de serviços, isoladamente, não são classificadas automaticamente como infantis. Identificações adultas são excluídas. É um filtro das identificações do provedor, sem classificação automática de idade.
- Kids consulta os mesmos registros do Room e reaproveita IDs, URLs, cache de imagens, episódios, favoritos e histórico. Não importa outra lista, não cria outra base nem outra pasta de imagens. Favoritos, links diretos e troca de canais respeitam o filtro. A filmografia externa de artistas não abre dentro do Kids.
- **Legendados** em Filmes e Séries agrega os títulos com L separado (`[L]`, `(L)`, `- L` etc.), nomes com Legendado/Legendada e categorias Legendados do provedor. Não duplica registros nem soma a categoria virtual no total geral.
- Todos, Continuar assistindo e Favoritos permanecem no início. Depois: lançamentos/anos em ordem decrescente, 4K/UHD e demais categorias em ordem alfabética. Essa regra fixa vale para Filmes e Séries; a ordenação de canais permanece a existente.
- Trailers e seus botões foram retirados de filmes, séries e artistas. A Activity não é registrada e o launcher antigo não abre nenhuma tela. Helpers antigos sem acesso pela interface permanecem para compatibilidade interna.
- Novos PINs exigem **exatamente 6 números**; teclado e entrada limitados a 6. PIN antigo de 4 ou 5 continua validando para não perder acesso; ao entrar no controle parental com ele, é necessário cadastrar e confirmar os 6 números antes de gerenciar as regras. Sair sem concluir mantém o PIN antigo, sem perder configurações.
- Opção **Kids** no controle parental autenticado. Quando ligada, os três menus normais ficam desativados e os acessos ao conteúdo normal ficam bloqueados, inclusive com autorizações de categoria antigas. Kids continua disponível sem pedir PIN na abertura do aplicativo. Voltar do Kids não desliga a restrição; o responsável desliga a opção dentro do controle parental. Não há uma segunda senha para sair da tela Kids.
- Capas antecipadas em grupos sequenciais de **5**, até **35 à frente** da janela visível; repõe quando restam 30 ou menos. Apenas as próximas 5 são preparadas na RAM pela rotina existente, enquanto as mais distantes ficam no disco. O limite não significa manter apenas 35 arquivos em todo o cache. Pausa/cancelamento ao abrir detalhes e sob pressão de memória continuam existentes.

## Preservado

Capas de até **300 × 450**, links originais, **JPEG qualidade 90**, RGB_565 opaco/ARGB_8888 com transparência, RAM adaptativa, importação **Xtream 250 / M3U 2.000**, armazenamento gerenciado **350 MB**, sendo **100 MB para imagens**, 25 MB para episódios, 25 MB para respostas e 5 MB para legendas. Os arquivos de navegação da grade, animação e duas fileiras têm guardas de integridade e são preservados. Gradle, credenciais, assinatura e configurações privadas não são distribuídos ou alterados.

## Validação

Resultado no ambiente de desenvolvimento: **60 testes locais passaram, sem falhas ou testes ignorados**. `assembleDebug` e `assembleDebugAndroidTest` concluídos. Instalador verificado com LF e CRLF/BOM, reaplicação, ativo binário íntegro, rejeição sem sobrescrever uma base alterada e recuperação após falha de gravação.

Testes locais com JUnit/Robolectric/Compose e Room cobrem Kids, Legendados, filtros por favoritos e acesso direto, troca de canal, classificação, PIN legado/novo, bloqueio Kids, urso nos sete temas, ausência dos botões de trailer, fila 5/35, reutilização de disco com RAM vazia/servidor offline, retorno de categoria, resolução, RAM e armazenamento. APK e APK de instrumentação são compilados separadamente dos testes locais. Instrumentação compilada não significa execução em TV física.

Os testes novos/atualizados de dispositivo seguem no instalador. O antigo teste de cabeçalho de trailer foi substituído por verificações de remoção da Activity e do launcher. Os testes locais usados na revisão estão em `testes/kotlin`. Para executá-los, com o projeto atualizado, SDK configurado e JDK compatível com o Gradle do projeto:

```powershell
.\gradlew.bat -I CAMINHO\kids-catalogos-pin6\testes\validar.gradle :app:testDebugUnitTest
```

Essa configuração de teste é opcional e externa; não modifica os arquivos Gradle do aplicativo. O desempenho e o aspecto final na sua TV precisam de teste no aparelho.
