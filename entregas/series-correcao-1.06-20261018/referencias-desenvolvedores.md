# Referência de código aberto consultada

Consulta via Git em 18/10/2026: repositório 4gray/iptvnator, revisão 5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0. Leitura de fontes, sem executar código externo nem importar dependências.

1. `XtreamApiService.getSeriesInfo` usa `GetSeriesInfo` com `series_id`, numa consulta de detalhes, sem requisição distinta por temporada.
   https://github.com/4gray/iptvnator/blob/5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0/libs/portal/xtream/data-access/src/lib/services/xtream-api.service.ts#L303
2. `ElectronXtreamDataSource.getContent` consulta `contentRequests`, retorna a mesma Promise em andamento e a remove em `finally`. Isso compartilha também sua falha enquanto há consumidores, em vez de proteger apenas o armazenamento do sucesso.
   https://github.com/4gray/iptvnator/blob/5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0/libs/portal/xtream/data-access/src/lib/data-sources/electron-xtream-data-source.ts#L330
   A implementação comparada é de catálogo, não de temporadas. No IPTV Premium, aplicamos o princípio ao carregamento da série via coroutine/cache existente; os testes locais são a evidência da correção.
3. `SerialDetailsSeasonsService` faz enriquecimento TMDB pela temporada selecionada e pelo tmdb_id; reavalia quando o identificador chega, mantendo a operação idempotente por série/temporada.
   https://github.com/4gray/iptvnator/blob/5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0/libs/portal/xtream/feature/src/lib/serial-details/serial-details-seasons.service.ts#L77
   Confirma a prioridade de imagens da temporada visível já implementada; não prova rapidez do provedor do usuário.

As páginas de documentação PHP/Android/Kotlin/OkHttp tentadas por HTTPS retornaram 403 nesta rede (PHP repetido fora do sandbox, mesmo resultado). Não são apresentadas como páginas efetivamente lidas. A pesquisa foi concluída por leitura da referência Git acima. Não houve verificação de resposta privada de Marília/NCIS.
