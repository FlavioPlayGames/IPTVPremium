# Correção de capas das séries M3U

## Caso investigado

O diagnóstico oficial terminou com sucesso em 64.965 ms: download 15.139 ms, preparação 39.371 ms e gravação 10.231 ms. Ele não contém títulos ou URLs de arte, portanto a identificação foi investigada no código e na M3U enviada anteriormente, usando O Conto da Aia como exemplo informado pelo usuário. A lista anterior não é assumida como cópia exata da lista atual.

Nessa amostra, O Conto da Aia tem 66 episódios numa categoria usando um mesmo retrato e outros 66 numa categoria com 66 imagens w1280 diferentes. A URL do retrato também aparece em 29 episódios de O Conto da Raposa de Nove Caudas. Há tanto imagens individuais dos episódios quanto uma associação incompatível de imagem entre séries no próprio material de origem.

O importador anterior escolhia a imagem da última ocorrência. O carregador aceitava a imagem fornecida antes de buscar arte oficial. O cache de imagens é indexado por SHA-256 da URL; não foi encontrado no trecho revisado um vínculo de cache por posição da grade que justificasse atribuir o problema a uma colisão entre índices.

## Alteração

M3uSeriesPosterPolicy guarda apenas candidatos resumidos por série, sem colecionar todas as imagens em RAM. Reconhece proporções explícitas no CDN TMDB, descarta o rendition w1280 usado pelas cenas desta lista e não promove uma sequência de imagens desconhecidas diferentes para capa. Aceita retratos reconhecidos ou uma imagem comum estável. Uma candidata associada a obras com nomes/anos incompatíveis é rejeitada.

É possível reaproveitar uma única candidata compatível de outra categoria da mesma obra, com comparação normalizada de nome e anos não conflitantes. Não ocorre compartilhamento quando há remakes/anos ou candidatas ambíguas. Sem candidata local, a URL do catálogo fica ausente e o carregador visível utiliza sua busca oficial existente; uma correspondência incerta continua com o visual de indisponibilidade.

A escolha não pretende reconhecer visualmente todo poster ou detectar uma associação incorreta que apareça isoladamente no provedor. URLs sem formato reconhecido e imagens estáveis ainda dependem da qualidade da fonte. O objetivo é evitar as duas contaminações verificadas, sem adivinhar por popularidade.

A busca de arte de séries remove uma duplicação de ano no formato Título 2017 (2017), preservando o título apresentado, IDs, agrupamento e números de obras como 1899 e The 100. Filmes não usam essa normalização. O helper compartilhado de arte também beneficia séries Xtream com esse formato duplicado; importação, transporte e episódios Xtream não foram alterados.

As imagens originais continuam na fonte de episódios. Não há chamadas HTTP no novo seletor de importação nem varredura remota do catálogo. Metadados oficiais são buscados somente pelo carregamento de capas visíveis já existente, com seus limites e identificação exata. Não foi alterado o esquema Room 6, ordenação, favoritos, histórico, reprodução ou dependências.

## Validação

Os resultados finais estão em validacao. Há testes sintéticos de candidato/capa, imagem repetida por títulos distintos, remakes, falta de ano, título numérico, parser real e preservação das imagens individuais dos episódios. O conjunto real recortado contém 132 entradas de O Conto da Aia e 29 de outra série compartilhando o retrato: o parser rejeita essa candidata em vez de atribuí-la arbitrariamente, conservando todos os 161 episódios. O teste da busca oficial usa resposta de serviço simulada; não comprova disponibilidade remota atual de imagens na TV.

Foram executadas regressões do importador oficial (20 cenários) e do cache transacional de episódios (4 cenários). Instalação e scripts testam aplicação idempotente, backup/restauração, bloqueio de base divergente/corrupção/conflito, sintaxe PowerShell e filtragem de dados privados no coletor. Nenhuma medição desta correção foi realizada na TV; atualizar uma vez a lista é necessário para substituir os metadados já persistidos.

## Resultado final

Compilação APK desempenho e Lint: BUILD SUCCESSFUL. Cenários aprovados: poster-tests: 10, aia-tests: 2, official-regression: 20, deferred-regression: 4, installer-tests: 4, scripts-tests: 2, collector-tests: 1. O fixture anexado contém somente títulos/categorias e URLs públicas de imagens TMDB, sem links dos vídeos ou credenciais. O teste foi executado no ambiente de desenvolvimento; resta a validação visual na TV após atualização da lista.
