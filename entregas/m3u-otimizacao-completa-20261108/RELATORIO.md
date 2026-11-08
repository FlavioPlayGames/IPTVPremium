# Importação M3U — investigação, implementação e validação

## Resultado e limite de aprovação

Foi implementado um candidato com chamadas de até 250 entradas e transações de até 8 lotes, limitado também a 2.000 entradas e 512 Ki caracteres por grupo. Nenhuma conclusão antecipada do catálogo foi introduzida. A configuração G8 é candidata para avaliação na TV, não uma garantia de desempenho nela.

**A meta de até 25 segundos na mesma TV NÃO foi comprovada.** A última medição real da TV continua sendo 130,153 segundos. O novo APK ainda precisa ser medido no aparelho. O benchmark local não substitui esse teste.

## Evidência de partida na TV

A captura confirma M3U nativo, revisão M3U_METADATA_R4, banco 5, 167 lotes, 329.732 entradas e conclusão SUCESSO. São 2.355 canais, 44.099 filmes, 9.800 séries distintas e 283.278 episódios. A lista recebeu 85.847.864 bytes sem compressão HTTP. A configuração observada foi WAL e synchronous=1.

| Operação real da TV | Antes | Depois na TV | Redução comprovada na TV |
|---|---:|---|---|
| Fora do corpo das inserções | 64,301712 s | Não medido | Não calculável |
| Episódios | 48,509134 s | Não medido | Não calculável |
| Filmes | 9,540682 s | Não medido | Não calculável |
| Séries | 4,166389 s | Não medido | Não calculável |
| Canais e categorias | 0,327179 s | Não medido | Não calculável |
| Tempo real do sincronizador | 130,153 s | Não medido | Não calculável |

Os tempos internos são de chamadas e podem se sobrepor ao parsing e à leitura. Os 64,30 s incluem esperas e operações externas ao corpo do método transacional; não comprovam 64,30 s de fsync, nem identificam isoladamente a memória ou a flash como causa.

## Fluxo real localizado

O ponto de sincronização é M3uSourceSynchronizer.sync(source), que passa por sharedSync e SharedSync.run. A mesma configuração da mesma playlist compartilha o resultado em andamento; configurações diferentes da mesma playlist são serializadas. StorageMaintenance.foregroundImport mantém a prioridade da importação em relação à manutenção.

M3uSourceSynchronizer.syncM3u verifica a geração ACTIVE e a política M3uTransportPolicy. forceM3u impede a sondagem Xtream. Uma lista automática pode usar a API quando permitido; essa decisão foi preservada. O diagnóstico analisado entrou em M3uCacheImporter.import, portanto não é o carregamento Xtream sob demanda.

M3uCacheImporter.import cria uma geração IMPORTING via PlaylistCacheGenerationManager.beginImport. DefaultPlaylistHttpClient.readLines lê a resposta usando PlaylistResponseBody e M3uLineStream.consume, com filas limitadas. A coleta já separa tempo nas leituras de rede, do corpo e espera do leitor pelo parser; essas leituras não equivalem a uma medida independente do download ponta a ponta.

importIntoGeneration interpreta EXTINF, usa M3uEntryMetadata e M3uEntryClassifier, calcula os mesmos IDs estáveis e monta as mesmas entidades CachedCategoryEntity, CachedChannelEntity, CachedMovieEntity, CachedSeriesEntity e CachedEpisodeEntity. Mantém categorias já vistas, ID da categoria em cache e primeira ordem de cada série. Não consulta o banco para descobrir a existência de cada episódio.

O produtor prepara os lotes e entrega grupos ao único escritor através de Channel com capacidade 1. O escritor chama PlaylistCacheDao.insertM3uGroup, uma transação Room sobre dados já preparados. A ordem categorias → canais → filmes → séries → episódios permanece em cada lote. O código gerado chama EntityInsertAdapter e reutiliza a instrução preparada na coleção, com bindings seguros e a política REPLACE original.

Depois da última leitura, o produtor envia o grupo restante, fecha a fila e aguarda writer.join. Só então markImportReady permite READY. A publicação READY → ACTIVE continua atômica no fluxo existente; os dados ACTIVE anteriores permanecem válidos até a troca. A limpeza de STALE ocorre depois, fora do caminho crítico. Erro/cancelamento descarta a geração incompleta, preservando a anterior.

## Gargalos: o que foi confirmado e o que falta

Confirmado no código: não há DAO por episódio, consulta por episódio, COUNT completo por lote ou paralelismo de várias inserções dentro do importador. O Room recebe coleções. Já há escritor único por importação, controle de sincronização por playlist e serialização de transações no Room. Não acrescentei um coordenador global ou outra fila que duplicasse esses mecanismos.

Confirmado nos logs: as chamadas de gravação ocupam aproximadamente 126,86 s; 62,56 s ficam no corpo e 64,30 s fora dele. A escrita limita a fila e faz leitor/parser esperarem. parsingMs de 80,815 s é tempo decorrido acumulado, incluindo agendamento e concorrência, não CPU exclusiva. leituraRedeMs de 2,759 s mede chamadas de leitura e não toda a transferência.

A implementação de RoomDatabase.beginTransaction nesta versão faz sincronização de invalidações e inicia a transação SQLite. endTransaction encerra a transação e agenda atualização de invalidações. Essa é evidência do caminho executado, não prova de qual parcela consumiu os 64 s na TV. Espera do executor, aquisição da transação, commit e notificações continuam hipóteses até a nova coleta.

O banco é um singleton Room 2.8.5 no caminho de compatibilidade Android, sem driver alternativo configurado. Não foram alterados executores, driver, dependências, WAL, synchronous, outros PRAGMAs ou índices. A alteração anterior de índice redundante já integra o banco 5; esta entrega não remove outro índice e não exige migração.

## Alterações implementadas

1. Chamadas de inserção de até 250 entradas no M3U, com modos G1, G4 e G8. ORIGINAL mantém a referência de chamadas de 2.000. Xtream não usa essa configuração.

2. Grupos preparados antes de abrir a transação, com até 1, 4 ou 8 lotes. O corpo transacional não recebe dados do canal, não faz HTTP e não interpreta EXTINF. O limite de texto vale para o grupo, evitando multiplicar a reserva de memória por 8. Há um grupo em escrita, no máximo um na fila e o produtor prepara o seguinte. Não há materialização de todos os episódios em RAM.

3. Corpo de inserção compartilhado entre a transação antiga e a agrupada. Não há um wrapper @Transaction adicional por sublote. Os métodos @Insert do Room continuam seguros e a política de conflito não muda. Não foi adotada escrita SQL manual: o código gerado já prepara/reutiliza o statement e não surgiu evidência que justificasse substituí-lo.

4. Cache LRU de até 256 metadados de séries. Episódios intercalados de séries com metadados idênticos deixam de regravar a capa da série. Qualquer mudança observada é gravada e a última ocorrência continua vencendo. Evicção só perde a otimização; não perde o registro. IDs, ordem, vínculos, URLs e metadados foram comparados com a implementação original.

5. Logs IPTV_PERF monotônicos por grupo, com startNs/endNs, duração, entrada/saída do corpo e medições das chamadas beginTransaction/endTransaction do Room. M3uTransactionMetrics transporta os cronômetros pelo contexto de corrotinas, inclusive o executor de transações, sem dados de conteúdo.

6. Resumos de parsing e conversão, contagens por tipo/categoria numerada, grupos/lotes, pico de grupo, heap Java observado e identificação da configuração. O coletor usa lista estrita de campos numéricos/enums; exclui SQL, títulos, URLs e credenciais. Não há medição por linha SQL ou por bind.

7. Perfis reproduzíveis de compilação e roteiro de três medições na TV. O coletor verifica os eventos reais de configuração, não transforma o formato somente pelo rótulo informado no PowerShell.

Atenção: a implementação medida na TV usava até 2.000 entradas por lote, não 250. G8 mantém um teto de registros por transação equivalente ao anterior. Ele reduz a chamada individual, mas NÃO reduz necessariamente o número de commits em relação à referência de 2.000. Seu benefício precisa ser medido. G1/G4 permitem avaliar o equilíbrio de memória, chamadas e transações.

## Benchmark local — método

Foram executadas três medições de cada configuração e cenário, na mesma máquina e JVM (-Xmx384m), com leitura sintética incremental, 329.732 entradas, as mesmas contagens por tipo, índices/esquema reais e adaptadores gerados pelo Room atual. O SQLite foi real, em arquivo local, via JDBC, com WAL e synchronous=NORMAL para corresponder ao valor observado na TV. Isso não altera os PRAGMAs do aplicativo.

Na atualização, cada fixture foi previamente preenchida com um catálogo completo, marcado ACTIVE. A medição inclui leitura sintética, parsing, entidades, gravação e READY da nova geração; o preenchimento inicial fica fora do cronômetro da atualização. As contagens da nova geração são verificadas em todas as execuções. A referência local é a cópia do importador que existia antes desta tarefa.

**Limitação essencial:** o adaptador de teste executa os defaults do DAO e os adaptadores gerados, mas não o executor Room Android, notificações/Paging ou driver do aparelho. Não há HTTP real, TV ou emulador nessa comparação. Logo os segundos abaixo não preveem o tempo na TV e não resolvem sozinhos a origem dos 64 s. Os arquivos bench.csv e refresh.csv permitem conferir cada rodada.

Não houve falha de memória no processo com limite de heap acima. Não foi feito profiling completo de heap/RSS nesse benchmark; a coluna de entradas/grupo é um limite estrutural, não um consumo de memória em KB. O novo APK registra heap Java observado por grupo; a validação de memória nativa e estabilidade na TV continua pendente.

## Primeira importação — benchmark LOCAL

| Estratégia | Mediana de 3 | Maior tempo | Transações de catálogo | Chamadas de lote | Máximo de entradas/grupo | Erros |
|---|---:|---:|---:|---:|---:|---:|
| ORIGINAL | 9,80 s | 9,97 s | 165 | 165 | 2000 | 0 |
| G1 | 10,66 s | 11,33 s | 1319 | 1319 | 250 | 0 |
| G4 | 9,97 s | 10,01 s | 330 | 1319 | 1000 | 0 |
| G8 | 8,85 s | 9,12 s | 165 | 1319 | 2000 | 0 |

## Atualização — benchmark LOCAL

| Estratégia | Mediana de 3 | Maior tempo | Transações de catálogo | Chamadas de lote | Máximo de entradas/grupo | Erros |
|---|---:|---:|---:|---:|---:|---:|
| ORIGINAL | 8,21 s | 8,88 s | 165 | 165 | 2000 | 0 |
| G1 | 10,64 s | 10,70 s | 1319 | 1319 | 250 | 0 |
| G4 | 9,96 s | 10,12 s | 330 | 1319 | 1000 | 0 |
| G8 | 8,61 s | 8,68 s | 165 | 1319 | 2000 | 0 |

## Seleção e aprovação

Na primeira importação local, G8 passou de 9,80 s para 8,85 s de mediana (9,7% menos). Na atualização local, passou de 8,213 s para 8,608 s (4,8% mais lento). Isso impede declarar ganho geral; a variação deve ser verificada no aparelho.

G8 foi selecionado como candidato entre as configurações de chamadas de 250 por apresentar o melhor resultado inicial local e manter os limites de registros/texto por grupo. O resultado da atualização deve ser considerado junto à primeira importação; não há seleção definitiva para todos os aparelhos antes das três coletas equivalentes na TV. Não foram selecionados G1/G4 apenas por presumir que lotes menores sempre aceleram.

Não há ganho numérico comprovado na TV para apresentar nesta entrega. A meta de 25 s não foi declarada atingida. O próximo teste prioritário é comparar ORIGINAL e G8 na mesma TV, mesma lista e mesmo cenário, três vezes, verificando beginCalls/endCalls e os novos campos temporais. Se os ganchos não forem observados, o coletor deve informar ausência de medida, nunca interpretar zero como operação gratuita.

beforeBodyUs inclui tempo antes de entrar no corpo; afterBodyUs inclui o retorno após o corpo. beginUs/endUs são intervalos das chamadas de Room, contidos ou sobrepostos a outros intervalos. Não devem ser somados a eles. endUs não mede apenas fsync. A taxa média de entradas/segundo pode ser calculada com entries e o tempo total real; não com o somatório de etapas sobrepostas.

## Integridade e testes

Geração completa do APK (assembleDesempenho), compilação Kotlin da variante desempenho e Lint passaram. O esquema gerado segue versão 5 e identity hash 579c4ad4cd357bb9f2d064c3011bd3fa, sem mudança de tabelas/índices.

18 cenários do importador com DAO simulado passaram: dados/IDs/ordem iguais à referência, metadados variáveis, Unicode, duplicidade, XUI, limites, filas, erro de rede, erro de escrita, cancelamento e READY somente após o escritor. Incluem cache de séries intercaladas e evicção de mais de 256 séries.

8 cenários com SQLite real e adaptadores gerados passaram: paridade de todos os campos em ORIGINAL/G1/G4/G8 e segunda importação; atualização que adiciona/remove itens e muda capas sem alterar a geração ACTIVE; política de conflito com último valor, nulos, aspas/Unicode e integrity_check; rollback do grupo em falha no segundo lote; rollback e propagação da CancellationException original.

15 cenários de sincronização e 2 de armazenamento passaram, preservando sondagem/API Xtream, fallback autorizado, atualização explícita, M3U direta do administrador, IDs e ordem do provedor. Esses testes usam transporte/DAO simulados; não são execução de Xtream no servidor ou TV.

6 verificações dos cronômetros e propagação de contexto passaram. Os scripts foram testados em PowerShell: aplicação, reaplicação, restauração, rejeição de base diferente, pacote corrompido e conflito ao desfazer. O filtro de logs foi testado com campos privados falsos e enums desconhecidos.

Não houve teste de reinício físico do aparelho ou APK executado na TV nesta etapa. A política existente de gerações foi preservada, mas esse teste real e o consumo de memória/fluidez no aparelho ainda precisam ser executados. Não foram declarados como aprovados por compilação.

## Arquivos do projeto

Os caminhos completos abaixo são do projeto real editado. No Windows, o mesmo caminho relativo fica dentro de C:\Users\flavi\AndroidStudioProjects\IPTVPremium.

- /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/importer/M3uCacheImporter.kt
- /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/dao/PlaylistCacheDao.kt
- /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/PlaylistCacheDatabase.kt
- /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/importer/M3uWriteGroup.kt
- /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/importer/M3uWritePolicy.kt
- /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/M3uTransactionMetrics.kt
- /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/dao/ImportBatchWriteMetrics.kt
- /workspace/backup-reviewed-build/app/tv-performance.gradle

M3uCacheImporter.kt: produtor/escritor agrupado, cache limitado de séries e métricas. PlaylistCacheDao.kt: corpo compartilhado e transação de grupo. PlaylistCacheDatabase.kt: ganchos de medição de begin/end. M3uWriteGroup.kt: lotes/grupos preparados. M3uWritePolicy.kt: perfis. M3uTransactionMetrics.kt: transporte dos relógios entre threads. ImportBatchWriteMetrics.kt: arquivo já existente, incluído sem alteração para conferir a base. tv-performance.gradle: propriedade de compilação iptvM3uProfile. Nenhuma versão de dependência/app foi alterada.

Arquivos auxiliares entregues: Aplicar.ps1 e Desfazer.ps1 com manifesto de hashes e backup; Instalar-TV.ps1; Medir-Importacao-M3U.ps1; Benchmark-TV.ps1; Comparar-Coletas.py; relatório; métricas e evidências de validação. Código e dados de teste usam fixtures próprias; nenhum banco da TV foi alterado por este ambiente.
