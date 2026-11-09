# Relatório — capa por temporada do episódio salvo

## Comportamento

A seleção da temporada parte do histórico já filtrado para a lista ativa e para o modo Kids/normal. A função de apresentação considera episódios reais com temporada válida, incluindo especiais (temporada zero). Ordena pelo evento mais recente, sem exigir progresso incompleto e sem escolher o maior número de temporada. Registros que só representam a série, sem episódio reproduzido, não substituem a temporada de um episódio real.

Normal, Kids, Favoritos e Continuar recebem a temporada pelo componente de catálogo compartilhado. No catálogo do player, todos os três caminhos das capas principais de série (Continuar, série em reprodução fixada e catálogo paginado) passam a temporada. A série em reprodução usa o episódio atual; as demais usam o histórico. A temporada faz parte da identidade do estado de imagem no Compose, evitando que a mudança preserve a capa anterior na tela.

A origem da lista não entra nessa regra: Xtream e M3U já entregam IDs de série e números de temporada/episódio ao mesmo histórico. Não há alteração na identificação de streams ou no importador nesta etapa.

## Imagens e cache

A identificação validada da série no TMDB é mantida. Os detalhes da obra já contêm a lista `seasons`; guardamos apenas os números das temporadas e seus caminhos válidos de poster, junto ao ID público e à capa geral. Não é necessária uma chamada de rede por temporada ou episódio. A consulta continua limitada aos itens visíveis e a duas resoluções simultâneas.

A seleção da temporada ocorre ao ler esses metadados. O cache compartilhado guarda arte pública, não a preferência ou o progresso do usuário. Duas pessoas/modos podem escolher capas de temporadas diferentes usando a mesma obra em cache.

Namespace `series_catalog_poster_v2:` evita que um registro da versão anterior, sem mapa de temporadas, impeça a nova função. Mantém validade de 30 dias para esse namespace e as cotas de armazenamento existentes. As outras informações do catálogo e o cache dos arquivos de imagem mantêm as regras anteriores.

Sem um poster válido da temporada solicitada, usa o poster geral. Se a URL da temporada existir nos metadados mas o carregador não conseguir baixar a imagem, tenta também a capa geral dentro do prazo da requisição. Se a obra só tiver arte de temporada e nenhuma capa geral, ainda é possível usar a temporada correspondente. URLs arbitrárias e fotos de episódios não são aceitas no mapa de posters de temporada.

## Pacote e compatibilidade

Pacote cumulativo com `series-capas-tmdb`: aceita a base `m3u-capas-automaticas` ou a etapa anterior de capas verificadas. Os arquivos da etapa anterior são incluídos para permitir aplicação direta; o instalador não reescreve os que já estiverem iguais. O backup e a restauração retornam exatamente à base encontrada. A etapa anterior retirava a consulta coletiva de capas na importação M3U, substituída por capas na exibição; esta etapa acrescenta a seleção por temporada.

Não muda o transporte Xtream, prioridade sobre M3U, banco versão 6, histórico persistido, favoritos, vídeo, áudio ou legendas. Não requer reimportar a lista.

## Validação

- Compilação Kotlin concluída com sucesso antes dos testes de comportamento.
- 17 cenários novos: série sem progresso, troca de temporada, último episódio concluído, revisita de temporada antiga, registro apenas da série, especiais, número inválido, IDs dos dois formatos, perfis independentes, capa geral, seleção de temporadas sem chamadas adicionais, leitura inicial do cache, fallback, série sem capa geral, cache anterior e prazo do cache.
- 17 testes anteriores do resolvedor de identidade preservados: ambiguidades, ano incorreto, aliases, identificação numérica, conflito de IDs, caminhos de imagem, concorrência, cancelamento e política do cache.
- 6 verificações estáticas de ligação com as telas, separação de perfil no ponto de navegação, preservação de filmes e fallback de download.
- Instalador verificado nas duas bases aceitas, com idempotência, backups, criação/remoção de arquivos e restauração integral. Recusa de base desconhecida, pacote corrompido e alteração posterior.
- Scripts PowerShell analisados pelo parser; coletor testado com ADB simulado para excluir eventos históricos e filtrar campos privados.

O resultado final de assembleDesempenho e lintDesempenho está em `validacao/build-results.txt`. Os testes funcionais utilizam respostas TMDB simuladas e classes compiladas do projeto. A ligação das telas foi auditada no código; não houve teste visual instrumentado nem instalação na TV nesta máquina. A origem remota real pode ter temporadas sem poster ou com a mesma imagem. Conferir visualmente na TV continua necessário.
