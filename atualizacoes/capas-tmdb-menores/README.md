# Capas e miniaturas TMDB menores na origem

Requer o teste capas-400x600 (71acd3b). Mantem os links da lista como fonte, sem pesquisar novos titulos no TMDB.

- Capas de filmes, series e carrossel: links do CDN oficial image.tmdb.org/t/p/original ou w500/w780 maiores que 342 passam a ser baixados em w342.
- Miniaturas horizontais de episodios: somente requisicoes series_episode usam w300. Mantem a proporcao horizontal. Outros consumidores do carregador compartilhado nao sao afetados.
- Links de provedores permanecem intactos. Imagens TMDB ja menores nao sao ampliadas. Parametros e nomes escapados sao preservados.
- Se a tentativa otimizada falhar, tenta o link original. Cancelamento da navegacao continua sendo propagado.
- Mantem as chaves e arquivos de cache antigos: as imagens ja baixadas sao reaproveitadas sem limpeza ou download forcado. A melhoria de rede vale para novos downloads.
- Continuam os limites de 100 MB para imagens e 350 MB para cache gerenciado. Navegacao, animacao, antecipacao, RGB atual e importacao nao mudam. Fundos, logos e artistas nao recebem essa reducao.
- Capas de outras origens continuam com a reducao local ate 400x600 do teste anterior. Miniaturas de episodios continuam com a decodificacao existente conforme o card.

O instalador verifica a base, cria backup exato e nao inclui configuracoes privadas nem Gradle. Backup: .navigation-backups/capas-tmdb-menores-*. Testes verificam tamanhos, dominios, links de provedores, parametros, preservacao de proporcao/arquivos pequenos e reuso do cache. Qualidade e desempenho reais dependem da TV e do servidor.

Validacao concluida: 13 testes automatizados passaram. APK debug e APK de testes compilados. Instalador PowerShell validado com LF/CRLF, backup, reaplicacao e rejeicao de fonte incompativel.
