# Revisão técnica — M3U oficial

## Decisão

Promover a preparação em arquivo do protótipo e sua escrita por statements reutilizados para a importação normal. Preservar o banco compartilhado, o gerenciamento de gerações e a navegação atual. O snapshot isolado experimental não pode substituir o banco inteiro: isso arriscaria outras playlists e os catálogos Xtream.

O caminho único agora é: download temporário → preparação local → fonte indexada de episódios finalizada → transação Room dos metadados → READY → ativação existente. A transação não espera rede nem parsing. Mantém os índices do banco compartilhado. A primeira abertura de uma série grava somente seus episódios; as seguintes reutilizam o cache da geração.

Removidos: fila e grupos de gravação do importador antigo, perfis G1/G4/G8/ORIGINAL desse importador, wrapper exclusivo de medição de transações, parser M3U em String e sua interface, cinco arquivos do snapshot/atividade experimental e registro da atividade no manifesto desempenho. DefaultPlaylistRepository também encaminha M3U exclusivamente ao importador oficial; a escolha entre API e M3U fica no sincronizador existente, respeitando M3U direta. O parser/classificador de atributos continua compartilhado; não é um segundo importador.

## Ordenação

A causa encontrada estava na consulta de categorias por nome e na reaplicação de ordenação no repositório e nas telas de canais normal/Kids. A importação já tinha posições de canais/séries, mas categorias não tinham uma posição persistida.

Adicionada coluna nullable providerOrder à categoria, migração Room 5→6 e propagação opcional ao modelo de domínio. O novo preparador registra a primeira ocorrência de cada categoria. Consulta e interface respeitam as posições M3U, sem reordená-las alfabeticamente. Categorias sem posição explícita (Xtream e gerações antigas) mantêm a política anterior. Canais e séries em DEFAULT mantêm a ordem da fonte; a escolha explícita A–Z/Z–A continua funcionando. Favoritos/Continuar permanecem nas seções existentes.

## Dados e falhas

A geração é criada IMPORTING antes da preparação. O writer verifica esse estado e a identidade playlist/geração de cada registro; valida quantidades antes do commit. Fonte de episódios finalizada antes de READY. Erro, arquivo inválido ou cancelamento não publica dados parciais e preserva a geração ativa. A limpeza usa as regras existentes que protegem READY/ACTIVE em uma corrida de cancelamento. Fonte permanente em files; spool e download em cacheDir. Um encerramento abrupto do processo pode deixar temporários no cache do sistema; não há coletor global novo nesta revisão.

Não há troca de arquivo do banco ativo, DROP de índices compartilhados ou alteração de qualidade de reprodução. Não foram alterados os arquivos protegidos do sincronizador, seleção de transporte, parser Xtream ou repositório de episódios.

O retorno funcional pelo Desfazer preserva a migração/entidade versão 6 e a remoção da atividade experimental. Isso evita reinstalar código versão 5 sobre dados migrados; o backup continua contendo os originais completos.

## Evidência

Consulte validacao para resultados finais e fontes dos testes. Os testes JVM usam SQLite real e os adaptadores/schema gerados pelo Room, com ponte de APIs Android e AtomicFile simulado. Testam orquestração real do importador através da mesma escrita oficial; não equivalem a executar Android/queda de energia física na TV. A compilação do APK e o Lint são verificados separadamente.

A lista sintética de 329.732 entradas inclui 283.278 episódios e foi executada com heap JVM de 128 MiB. A importação grava zero episódios no Room e mantém todos na fonte por série. Não se atribui ao aparelho o tempo dessa execução em máquina de desenvolvimento. O próximo diagnóstico deve medir este caminho oficial, incluindo abertura/reabertura de séries.

## Resultado final

APK desempenho e Lint: BUILD SUCCESSFUL (6m54s). Cenários aprovados: official: 20, volume: 2, deferred: 4, sync: 17, repository: 3, installer: 4, collector: 1, scripts: 2, order: 3. Verificações de pacote confirmam correspondência dos fontes com o build e ausência da atividade experimental no manifesto mesclado. Download remoto do ZIP será conferido por SHA-256 antes da entrega.
