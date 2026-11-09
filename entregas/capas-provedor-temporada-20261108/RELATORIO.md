# Relatório — provedor primeiro, temporada durante reprodução

## Mudança de regra autorizada

A busca TMDB por cada série visível dava resultado, mas acrescentava espera na primeira abertura. A nova regra aceita a imagem do provedor como capa inicial e prepara a imagem da temporada quando o usuário assiste à série. Isso substitui a regra anterior que exigia identificação TMDB para toda capa de série.

O catálogo não chama mais o resolvedor remoto. Séries sem temporada salva e filmes seguem diretamente para o carregador de imagens do provedor. Séries com temporada salva consultam somente metadados locais já preparados. Se a imagem da temporada estiver em disco, podem usá-la imediatamente; caso contrário, mostram a imagem do provedor enquanto carregam a imagem de temporada já identificada. Sem metadados locais, seguem com o provedor, sem pesquisar a obra.

O resolvedor identifica explicitamente quando uma imagem pertence à temporada. A capa geral do TMDB não é confundida com uma substituição de temporada: sem imagem específica, conserva-se a capa do provedor. A validação de identidade continua no resolvedor remoto usado ao assistir.

## Reprodução e atualização da tela

PlaybackScreen aguarda o estado PLAYING da mídia com mesmo ID e URL do episódio atual. Após 1,5 segundo, chama uma ação opcional e cancelável de preparação de arte. Filmes e canais não disparam essa ação. A ação obtém a série do catálogo da lista ativa, garantindo que nome, ano e IDs sejam os mesmos usados nas capas. O preparador aguarda a prioridade de abertura de mídia/importação, limita a concorrência a dois pedidos e mantém prazo de 15 segundos. Erros de arte/leitura do catálogo não interrompem a reprodução; cancelamento continua sendo propagado.

Após obter e carregar a imagem, uma revisão de apresentação invalida o estado das capas no catálogo normal, Kids e player. Não apaga os caches. A escolha de temporada continua vindo do histórico separado por perfil; apenas arte pública é compartilhada.

As telas aceitam a imagem do provedor antes da substituição terminar. O catálogo do player também tenta a atualização quando já tem um bitmap inicial, evitando conservar indefinidamente a imagem inicial. Continua usando caminhos próprios para imagens explícitas de episódios e temporadas.

## M3U e Xtream

M3uCatalogParser conserva o logo HTTP/HTTPS original fornecido para a série, inclusive quando o provedor reutiliza a mesma imagem em mais de uma obra. Remove a rejeição anterior por imagem compartilhada/variável. Não há pesquisa de arte na importação. Isso é deliberado na regra provedor-primeiro; a fidelidade da capa inicial depende da imagem da lista.

Para listas M3U já importadas com os filtros anteriores, é necessária uma atualização da lista para repor as imagens descartadas. Não é necessário excluir a lista nem limpar dados.

A antecipação limitada já existente para imagens do provedor volta a valer para séries. Não foi criado outro mecanismo de pré-carregamento. O fluxo Xtream, prioridade de reconhecimento, tentativas, episódios e geração de cache não foram alterados nesta etapa. Os registros M3U continuam com os mesmos IDs, ordem e demais campos; muda a aceitação da imagem.

## Validação e limites

- Compilação Kotlin e build/Lint finais: resultado em `validacao/build-results.txt`.
- 17 testes do resolvedor de identidade e 17 de seleção por temporada mantidos.
- 3 testes de metadados locais: ausência sem cliente remoto, reaproveitamento após preparação e distinção entre capa geral e capa de temporada.
- 2 testes nativos de imagens do provedor: preservar imagens compartilhadas e preservar imagens entre edições da mesma obra.
- Regressões do importador e estrutura/ordem dos registros: resultados em `validacao/official-results.txt`.
- 7 verificações estáticas das ligações: catálogo sem busca remota, imagem provisória e atualização nas três telas, gatilho PLAYING da mídia correta, identidade do catálogo e antecipação existente.
- Instalador: bases compatíveis, backup, idempotência, criação/remoção de arquivos, restauração e rejeição de conflitos/corrupção. Parser PowerShell e coletor com ADB simulado.

Os testes funcionais usam metadados simulados e as classes compiladas do projeto; a ligação das telas foi verificada no código. Não houve instalação na TV nem medição de tempo real de CDN/provedor nesta máquina. O ganho esperado é retirar a identificação TMDB da abertura do catálogo; o primeiro download da imagem do provedor ainda depende da rede. Nenhum parâmetro de qualidade de vídeo foi alterado.

Pacote cumulativo, com fontes anteriores necessários à aplicação nas bases aceitas. Backup devolve os fontes exatamente ao estado anterior. Não há alteração de schema, histórico persistido, favoritos ou configurações de reprodução. Não inclui APK, propriedades locais ou segredos.
