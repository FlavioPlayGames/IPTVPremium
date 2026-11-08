# Capas do provedor em Xtream e M3U

O catálogo podia substituir uma imagem ausente/falha por uma busca TMDB e a M3U utilizava arte dos episódios como capa da série. A referência aplicada agora é a regra do Xtream antigo: stream_icon para filmes, cover para séries e carregamento direto da URL associada ao item.

## Comparação com o arquivo antigo

O corpo completo de MoviePosterLoader coincide com o objeto existente no MoviesScreen antigo. DefaultXtreamParser, XtreamDirectCatalogReader e XtreamArrayStream também coincidem. A tela antiga de séries chamava MoviePosterLoader.loadWithRetry com a URL da série, sem pesquisa alternativa por nome no catálogo. Essa chamada direta foi restabelecida no helper compartilhado pelas telas atuais, incluindo normal, Kids e player. Layout, foco e navegação não foram revertidos.

Não se copiou a regra antiga da M3U de aproveitar a última ocorrência do episódio: a orientação final foi usar a regra Xtream como referência para os dois formatos. O seletor adicional de imagens de outras edições foi removido. Não existe busca TMDB de substituição dentro de loadCatalogPoster.

## M3U mantida em modo direto

A fonte M3U continua sendo importada pelo caminho oficial e seus IDs/links não são convertidos para Xtream. Se o get.php permite resolver servidor/conta, a etapa opcional lê os metadados get_series_categories/get_series do próprio provedor e extrai cover, com os parsers existentes. O conjunto é filtrado aos nomes presentes na M3U; a associação exige nome normalizado compatível, ano não conflitante e candidata única, preferindo a categoria correspondente. Não há seleção por popularidade ou tradução aproximada.

As capas selecionadas são guardadas num índice temporário identificado por playlist/geração. O writer valida essa identidade e altera somente posterUrl das linhas de séries da geração IMPORTING. Consultas HTTP e parsing JSON são feitos antes da transação Room. O índice e os demais temporários são removidos ao concluir/cancelar a importação. As imagens de cada episódio permanecem na fonte permanente de episódios.

A consulta opcional tem prazo de 12 segundos e usa cancelamento/desconexão do cliente HTTP existente. Falha/indisponibilidade do serviço não impede a importação M3U. Cancelamento solicitado pelo usuário continua cancelando a operação, sem publicar READY parcial. Erro de armazenamento do índice é tratado como erro da importação para não publicar dados incompletos.

Na negociação automática que já decidiu por M3U nativa, a consulta opcional não é repetida. No caminho Xtream normal nenhuma requisição extra é acrescentada. Na opção administrativa M3U direta, a consulta é somente de capas e não muda a escolha do transporte dos vídeos.

M3U pura sem API: a capa depende de tvg-logo estável. Imagens variáveis e formatos conhecidos de cena não viram a capa da série. Sem fonte confiável o resultado é o visual de indisponibilidade. A política não reconhece visualmente todas as imagens e não corrige uma imagem já errada no próprio cover do provedor. Essa limitação é diferente da associação incorreta feita pelo aplicativo.

## Preservado

Banco/esquema versão 6, ordem do provedor, IDs e títulos exibidos, URLs dos vídeos, episódios sob demanda, campos individuais dos episódios, histórico/favoritos e prioridade/regras estáveis do Xtream. Nenhuma migração, dependência, qualidade de reprodução ou alteração do site.

## Validação

Ver validacao para fontes dos testes e resultados. Foram comparadas associações de filmes M3U com o importador nativo antigo, campos de capa Xtream em 60 registros, capa estável versus arte variável da M3U, metadados de capa da API em M3U direta, categoria/ano e ambiguidade, indisponibilidade, cancelamento, proteção de identidade do índice e preservação dos links/episódios. O teste de metadados usa servidor simulado; a disponibilidade/correção do cover real ainda precisa ser conferida na TV.

As regressões incluem importador oficial (20), cache transacional de episódios (4), sincronização/armazenamento (21) e repository Xtream (3). Ferramentas testam aplicação/remoção/restauração de fontes, idempotência, conflitos/corrupção, sintaxe PowerShell e filtragem de credenciais/URLs na coleta. A medição futura permite observar series_covers/matched e o transporte real.

## Resultado final

APK desempenho e Lint: BUILD SUCCESSFUL. Cenários aprovados: provider: 6, poster: 5, official: 20, deferred: 4, sync: 21, repository: 3, installer: 4, scripts: 2, collector: 1. O teste visual com o provedor real permanece para a TV após reinstalar e atualizar a mesma lista.
