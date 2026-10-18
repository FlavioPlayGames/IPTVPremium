# Avaliação da importação Xtream — 18/10/2026

Análise do código em /workspace/storage-work, sem alterar a importação para esta comparação. Os testes usam respostas controladas e cliente HTTP local; a consulta ao provedor real neste ambiente recebeu HTTP 403. Portanto, os achados abaixo não estabelecem a causa no aparelho.

## Importação e episódios são caminhos separados

DefaultPlaylistRepository.importXtream autentica e importa categorias/canais, filmes e séries. O catálogo é processado em lotes e escrito em nova geração; a ativação da geração READY é transacional. Em falhas, o catálogo ativo anterior é preservado. Respostas completas inválidas de filmes/séries podem cair para consultas por categoria com até três consultas concorrentes e estágios temporários. Os IDs são preservados de series_id, passam ao CachedSeriesEntity e continuam separados por playlist e geração. A biblioteca compartilhada não participa desse importador e não reescreve esses IDs no catálogo.

A importação não chama get_series_info para todas as séries. Temporadas/episódios são carregados sob demanda pela série em foco/aberta. Assim, importar com sucesso confirma o catálogo, mas não garante que uma consulta posterior de episódios terá JSON válido, autenticação aceita ou imagens disponíveis.

## Pontos concretos de atenção

1. **Logos personalizados:** DefaultXtreamParser.parseSeriesBatch define titleLogoUrl = null. O gravador preserva o que recebe, mas um logo disponibilizado pelo painel nesse catálogo não é aproveitado por esse parser. Não explica temporadas vazias. O relato de que recolocar a chave restaurou o logo é compatível com a obtenção independente pelo TMDB, mas não prova que o painel forneceu o logo. Não foram inventados nomes de campos a partir dessa hipótese.
2. **Imagens relativas:** posterUrl é copiado de cover; backdrop usa o primeiro caminho do painel, sem resolver contra a URL base. No caminho dos detalhes/miniaturas, referências que não começam com http:// ou https:// podem ser descartadas. Há uma lacuna de compatibilidade para //host/imagem ou /pasta/imagem. Não foi comprovado que esse provedor específico envia esses formatos.
3. **Espaços no usuário:** a importação usa source.username.trim(), mas SeriesEpisodesRepository monta a consulta com xtream.username sem trim. Caso o cadastro conserve espaços nas extremidades, o catálogo pode autenticar e os detalhes falharem depois. É uma inconsistência real do código, condicionada à existência desses espaços; não há evidência de que ocorreu com esta conta.

## Velocidade e vínculos

As etapas de importação são sequenciais: canais e filmes precedem séries. Isso pesa no cadastro/atualização integral, mas não justifica sozinho a demora de abrir série de catálogo já ativo. O fallback por categoria pode abrir até três conexões simultâneas ao painel durante uma sincronização, concorrendo com uma consulta de episódios se ocorrerem ao mesmo tempo.

A reconciliação de favoritos/progresso é posterior e separada da importação. Ela percorre o catálogo local e pode fazer consultas adicionais para completar identidades de filmes e nomes alternativos. O teste Pausados interrompe essas tarefas e a aplicação dos vínculos, sem modificar importação, IDs nem consulta de episódios. Importação de lista em andamento não é pausada por esse teste.

## Como interpretar a comparação

Compare a mesma série da mesma lista pela categoria/busca, em Pausados e Ativos. Os caches permanecem. Considere nos logs origem=rede versus origem=disco e o total da abertura; uma diferença de tempo isolada entre primeira e segunda abertura não prova interferência. Se falhar nos dois modos, os vínculos deixam de ser a principal hipótese. Se falhar somente por atalhos no modo Ativos, investigue o ID associado. Se falhar apenas durante importação/atualização, investigue disputa de conexões e a transição de gerações.

Compilação dos dois modos e lint concluídos. Foram reexecutados os cenários existentes de integração Xtream, enquadramento/JSON Android e transporte HTTP local. O emulador do usuário ainda precisa validar o comportamento real.
