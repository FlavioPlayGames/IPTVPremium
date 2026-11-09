# Validação — capas verificadas por série

## Problema e solução

O diagnóstico anterior terminou com zero correspondências na consulta coletiva de capas. A resposta get_series tinha aproximadamente 9,9 MB e o processamento estava sujeito ao prazo de 12 segundos, além das imagens repetidas ou de episódios no M3U. Estender esse prazo não resolveria a confiança na imagem apresentada.

Esta revisão passa a resolver a capa individualmente na exibição da série, independentemente da importação. Remove a consulta coletiva de capas da importação M3U e seu índice temporário; o restante do fluxo nativo e os episódios sob demanda são preservados. M3uSourceSynchronizer não foi alterado: a prioridade e as tentativas do Xtream continuam existentes.

O identificador do cache reúne nome normalizado, ano efetivo e IDs públicos, sem depender do UUID da lista ou de URLs de episódios. Usa um namespace novo para não reaproveitar uma identificação antiga influenciada por fotos do provedor. Busca em português primeiro; verifica títulos oficiais, traduções e nomes alternativos, com recusa de ambiguidades. Títulos numéricos como 1923 não usam o próprio nome como ano de estreia.

Somente caminhos de poster válidos do TMDB viram URLs HTTPS do CDN oficial. Sem poster principal, pode usar um poster vertical da mesma obra, com preferência por português. Não usa fotos do provedor como referência para identificar a série. O primeiro desenho da imagem no Compose também usa a identidade verificada, impedindo a prévia de uma imagem antiga e trocada em RAM.

As capas principais do catálogo normal, Kids e player usam o mesmo resolvedor. Continuar e Favoritos passam pelo componente compartilhado. Imagens explícitas de temporadas e episódios mantêm o comportamento existente. Filmes mantêm a origem de imagens anterior.

Duas consultas simultâneas, carregamento apenas enquanto o item está visível, cancelamento fora da tela e prioridade para abertura de mídia/importação. Metadados públicos mínimos no cache de disco existente: ID e caminho do poster; validade de 30 dias apenas para esse namespace. Outras informações continuam com validade anterior de 6 horas. O cache de imagens e suas cotas não foram substituídos.

## Evidências

- Build final `:app:assembleDesempenho :app:lintDesempenho --offline --console=plain --max-workers=2`: BUILD SUCCESSFUL.
- 17 testes do resolvedor: título duplicando ano, homônimos, ano incorreto, nome oficial, namespace isolado, título numérico, poster vertical, conflito de IDs, caminho inválido, aliases, ambiguidade, deduplicação simultânea, cancelamento e prazo do cache.
- 20 testes de regressão do importador/cache/gerações/ordem e migração 5→6 com SQLite e schema Room gerado.
- 21 testes de sincronização/armazenamento, incluindo prioridade Xtream, M3U direto, tentativas, cancelamento e edição sem duplicar lista.
- 2 testes dos filtros nativos de imagens compartilhadas entre obras.
- 1 teste confirmando importação compatível sem chamadas coletivas de capas e com episódios indexados sob demanda.
- 4 verificações estáticas do encaixe UI: normal/Kids, player, antecipação e remoção da consulta coletiva.
- Instalador: aplicação, idempotência, backup, criação/remoção, restauração completa, recusa de base desconhecida, pacote corrompido e alteração posterior.
- Scripts PowerShell analisados pelo parser; coletor testado com ADB simulado para excluir eventos antigos e remover campos privados.

Os testes funcionais usaram respostas TMDB simuladas e SQLite local. A verificação UI acima é de código, não um teste visual instrumentado. Não foi executada nesta máquina uma consulta autenticada real ao TMDB nem instalado este resultado na TV. A validação visual final deve ser feita no aparelho, em especial O Conto da Aia. Obras sem correspondência segura ou sem poster podem permanecer com imagem padrão; não prometemos uma capa para todo título.

Não há alteração de schema, parâmetros de vídeo, seleção de qualidade, áudio ou legendas. O pacote não inclui APK, local.properties ou segredos. Os logs anexados contêm resultados de teste selecionados, não os arquivos originais de listas.
