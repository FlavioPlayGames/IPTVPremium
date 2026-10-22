# Canais: retomada, conexão alternativa, tela cheia e avaliação sem animações

Entrega sobre o último projeto enviado, mantendo a versão 1.11 (13). Não altera imagens, fontes, listas cadastradas ou catálogos importados.

## Canais

- **Retomar:** ao abrir Canais, resolve no banco o último canal reproduzido com sucesso naquela lista e inicia sua prévia. O histórico fica salvo no aparelho e é separado por lista. A opção **Continuar assistindo** usa esse histórico real, limitado a 32 canais; não mostra mais os quatro primeiros itens do catálogo como “recentes”.
- **Conexão:** ajustes de buffer inicial e limites de conexão/leitura. Erro, encerramento inesperado ou tentativa sem iniciar reprodução por 12 segundos levam à próxima transmissão candidata. Uma transmissão saudável continua tocando; não se consultam outras listas sem necessidade.
- **Ordem da recuperação:** outras transmissões do mesmo canal na **categoria original → demais categorias da lista atual → outras listas já importadas**, como último recurso. Favoritos e Continuar usam a categoria original do canal nessa busca, e não o grupo virtual de favoritos/histórico.
- **Mesma identidade:** compara nomes normalizados, ignorando marcas de qualidade/codec como HD, FHD, 4K e H265. Preserva números, “+”, região, idioma e temporadas. Um EPG repetido sozinho não autoriza mudar para outro canal. Nomes ambíguos ou abreviações não reconhecidas não são trocados por aproximação.
- **Mensagens:** durante a recuperação aparece **“Buscando servidor disponível…”**; se todas as candidatas falharem, **“Canal indisponível no momento.”** É possível selecionar novamente o canal indisponível para tentar outra vez.
- **Outras listas:** usa apenas gerações de catálogo já salvas, sem importar, ativar outra lista ou copiar canais para a atual. A conexão da transmissão encontrada é preservada na URL original. Seleção, EPG, favorito e histórico continuam associados ao canal escolhido na lista atual. Filtros parentais são aplicados também às candidatas de outras listas.
- **Tela cheia:** a prévia não navega mais para outro player. O mesmo ExoPlayer e SurfaceView continuam montados; expandir muda apenas os limites e o enquadramento do vídeo. Não chama `prepare()` nem reabre a mídia por expandir/recolher. Mantém SurfaceView, apropriado para vídeo de TV/4K.
- **Troca pelo controle:** em tela cheia, CH+/CH− ou cima/baixo trocam de canal e exibem uma lista à direita com os vizinhos. Cima/baixo segue a posição visual; CH+ avança e CH− retrocede. A consulta volta do último ao primeiro, respeitando a categoria, favoritos ou histórico em uso. A lista some após cinco segundos; OK a mostra/esconde. Voltar retorna à prévia.
- **Bordas:** removidos os quatro enfeites de canto do vídeo. Painéis e itens sem destaque usam contorno neutro do modo claro/escuro; o metal do tema fica no foco/seleção.

“Disponível” na lista lateral significa presente no catálogo e permitido pelos filtros. Não abre vários canais antecipadamente para testar os vizinhos; a recuperação acontece ao tentar reproduzir um canal que falha.

## Avaliação de filmes e séries

Sem menu de avaliação, retângulo branco ou animações. OK entra na escolha sempre na **primeira estrela**. Somente a estrela atual fica com contorno amarelo; esquerda/direita move esse destaque até a quinta. Outro OK salva imediatamente na ficha. Voltar cancela a edição. A nota salva continua representada pelas estrelas preenchidas e pelo número da avaliação. “Limpar” permanece disponível ao editar uma nota existente.

O arquivo antigo `RatingCelebration.kt` é retirado, com backup, se ainda existir e corresponder à entrega anterior. Não foram adicionados testes automatizados ao aplicativo.

## Aplicar no PowerShell

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/canais-conexao-avaliacao-20261022'
$script = Join-Path $env:TEMP 'Preparar-Canais-Conexao.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Canais-Conexao.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O instalador verifica todos os arquivos antes de alterar qualquer um, recusa sobrescrever edições locais desconhecidas e cria backup em `.live-channels-backups`. Aceita também os fontes conhecidos anteriores às entregas de avaliação/troca de listas. A remoção fica limitada ao arquivo de animação indicado no manifesto; não apaga recursos nem dados.

Se baixar o ZIP manualmente, use `-PacoteLocal 'C:\caminho\canais-conexao-avaliacao.zip'`.

Depois de aplicar, gere e instale o novo APK sobre o aplicativo existente para preservar os dados. O script compila, mas não instala o APK na TV automaticamente.

## Conferir no aparelho

1. Reproduza um canal, saia de Canais e volte; feche/reabra o app e confira a retomada na mesma lista. Canais que falharam antes de tocar não devem substituir o último assistido.
2. Teste um canal com versão 4K que falha e outra qualidade que funciona. Confira a ordem de busca, as duas mensagens e a interrupção da busca ao escolher outro canal.
3. Teste Favoritos e Continuar, incluindo uma alternativa que só exista em outra lista já importada. O favorito e o histórico devem continuar ligados ao item original.
4. Expanda/recolha um canal já tocando. Confira continuidade do áudio/vídeo, foco do controle e ausência de nova conexão. A transição visual da superfície e o comportamento de 4K/HDR dependem do aparelho e precisam dessa validação.
5. Em tela cheia, teste CH+/CH− e as setas, incluindo a passagem do último ao primeiro. Confira a lista lateral, OK e Voltar.
6. Confira bordas nos modos claro e escuro. Em um filme e uma série, confira seleção de uma estrela por vez desde a primeira, salvamento sem animação e “Limpar”.

Compilação e verificações locais estão em `verificacao.txt`. Não houve teste com TV, controle físico ou servidores reais nesta entrega; os tempos e a disponibilidade do provedor não foram medidos.
