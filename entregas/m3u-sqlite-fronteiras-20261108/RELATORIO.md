# Resultado das três rodadas G8 na TV

## Conclusão

A mediana real foi **125,697 segundos**; o maior tempo, **133,906 segundos**, e o menor, **120,956 segundos**. A meta de 25 segundos não foi atingida. O agrupamento G8 não resolveu o principal custo identificado.

O avanço concreto no diagnóstico é localizar aproximadamente 66 segundos DEPOIS do corpo das inserções. A espera para entrar no corpo somou apenas 0,159–0,190 s por importação. Portanto, estas capturas não sustentam priorizar uma fila de acesso ao Room como causa dominante.

| Parcela | Rodada 1 | Rodada 2 | Rodada 3 | Mediana |
|---|---:|---:|---:|---:|
| Tempo real do sincronizador | 133,906 s | 125,697 s | 120,956 s | 125,697 s |
| Chamadas de gravação | 129,583 s | 122,625 s | 117,832 s | 122,625 s |
| Antes de entrar no corpo | 0,190 s | 0,168 s | 0,159 s | 0,168 s |
| Depois de sair do corpo | 64,520 s | 66,461 s | 65,997 s | 65,997 s |
| Inserções de episódios | 47,274 s | 41,327 s | 38,518 s | 41,327 s |
| Inserções de filmes | 9,070 s | 7,721 s | 6,968 s | 7,721 s |
| Inserções de metadados das séries | 8,223 s | 6,672 s | 5,869 s | 6,672 s |

As durações das chamadas de inserção incluem agendamento e possíveis preempções durante as chamadas. O parsing roda em paralelo e seus tempos não devem ser somados a essas parcelas. beforeBodyUs começa antes da chamada ao DAO, e afterBodyUs termina depois do retorno completo da transação. São limites do corpo Kotlin, não provas de tempo exclusivo de commit.

## Integridade e configuração observadas

As três execuções informaram M3U nativo, batchSize=250, transactionBatches=8, limite de texto 524.288 caracteres, fila de 1 grupo, original=0 e status final SUCESSO. Os resumos de cada execução registram 175 grupos e 1.320 lotes. Nenhuma falha ou tentativa adicional aparece nos resumos.

Todas processaram 329.766 entradas: 2.355 canais, 44.094 filmes e 283.317 episódios, com 9.802 séries distintas e 104 categorias. São contadores do processamento e das chamadas, não uma auditoria independente de todas as linhas finais do banco ou de deduplicação. O protocolo aguarda o escritor antes de READY, mas a coleta não contém hashes de todos os metadados.

A leitura registrou 85.857.362 bytes nas três execuções, sem compressão HTTP. O contador dentro das leituras de rede ficou em 2,744–2,783 s; não mede o download independente de buffers e contrapressão. O heap Java máximo observado nos pontos de amostragem foi de 16.807–17.304 KB; não inclui memória nativa nem picos entre amostras. Não há prova de falta de RAM nessa coleta, nem medição completa que permita descartá-la.

## Problema da instrumentação anterior

beginCalls=0 e endCalls=0 nas três rodadas. Os overrides de RoomDatabase.beginTransaction/endTransaction não foram chamados nesse caminho; não se pode interpretar beginUs/endUs=0 como operação instantânea. Esses ganchos não serviram para decompor o custo de encerramento e foram removidos.

O corpo terminou, em média por grupo, antes de aproximadamente 0,4 s de retorno adicional. A mediana por grupo de afterBodyUs foi 402 ms, 419 ms e 408 ms nas rodadas. O restante pode estar na chamada SQLite endTransaction, em notificações/rotinas do Room, em agendamento do retorno ou outros efeitos desse trecho. Até medir a interface efetivamente usada, nenhum deles é uma causa física comprovada.

Também houve duas lacunas no detalhamento capturado: faltou a linha do grupo 23 na rodada 1 e do grupo 114 na rodada 2. A rodada 3 contém os 175 grupos. Os resumos finais do aplicativo informam todos os grupos nas três; isso indica lacuna do log detalhado, não evidência de perda de conteúdo. A análise usa os totais do resumo e sinaliza a cobertura parcial das duas primeiras capturas.

## Comparação com a captura anterior

A captura anterior durou 130,153 s, mas era apenas uma execução e tinha outro tamanho de resposta e outras contagens: 329.732 entradas, 44.099 filmes, 283.278 episódios e 9.800 séries. A resposta atual tem mais 9.498 bytes e mais 34 entradas processadas. Isso impede uma comparação perfeitamente controlada entre a referência antiga e a mediana nova.

Além disso, a diferença entre as próprias rodadas G8 chega a 12,950 s. Não há evidência para atribuir a redução entre a primeira e a terceira rodada ao algoritmo; aquecimento, sistema, I/O e outros fatores não foram isolados. Não foi calculado ganho de velocidade atribuível à mudança.

## Séries

seriesPrepared=127.262 nas três rodadas significa preparação de metadados durante a leitura, não 127 mil séries distintas ou necessariamente 127 mil INSERTs. Existem 9.802 séries distintas. Metadados podem variar entre episódios e a lista por lote pode coalescer repetidos. O próximo diagnóstico acrescenta seriesWritten, que conta entidades enviadas aos métodos de inserção, preservando as regras atuais. Não ignorei mudanças de capa/metadados nem alterei o resultado da importação para reduzir esse contador.

## Correção entregue

M3uMeasuredOpenHelperFactory envolve o mesmo FrameworkSQLiteOpenHelperFactory usado pelo auxiliar Android. Encaminha a mesma configuração e todas as operações, incluindo SQL, bindings, parâmetros, durabilidade e callbacks. Apenas as chamadas da interface SupportSQLiteDatabase de início/fim de transação são cronometradas quando há uma medição M3U ativa. O código de inserção, G8, limites, índices e esquema continuam iguais.

As variantes normal, não exclusiva, somente leitura e com listener são encaminhadas. Chamadas externas e aninhadas têm contadores separados. Um estado por thread acompanha o aninhamento do auxiliar, e a identidade do wrapper é reutilizada para acessos de leitura/escrita do mesmo banco. Falhas são propagadas, sem substituição por mensagens com URLs ou SQL.

boundary=SUPPORT_SQLITE_R1 identifica o ponto novo. beginUs/endUs passam a medir a interface efetiva, e não os overrides anteriores do Room. nestedBeginUs/nestedEndUs registram chamadas aninhadas da interface; não pretendem medir todo SAVEPOINT executado por SQL. contextMeasured/contextGroups confirmam se o contexto do cronômetro chegou ao corpo transacional.

Se endUs concentrar os aproximadamente 66 s, a investigação deve se concentrar dentro do encerramento SQLite. Se for pequeno, a prioridade passa para a parcela restante depois dessa chamada. Mesmo endUs elevado não é prova exclusiva de fsync: a chamada pode incluir commit, checkpoint, bloqueios e callbacks. Contadores zerados ou contexto não observado devem ser tratados como medição ausente.

Nenhuma consulta de contagem nem medição por registro/bind foi acrescentada. O coletor mantém uma lista estrita de campos numéricos e enums; não publica SQL, URLs ou credenciais. O banco continua na versão 5, sem nova migração. O caminho Xtream não recebeu nova política de sincronização.

## Validação e próximo passo

A nova fronteira tem testes de encaminhamento, classificação externo/aninhado, variantes com listener e leitura, parâmetros intactos, falha ao iniciar/finalizar, reabertura e contexto de corrotina. São testes com interface nativa simulada, não novas medições na TV. As regressões do importador, sincronização e SQLite são executadas novamente; os resultados entregues ficam em validacao/.

A próxima ação é aplicar a correção e coletar UMA execução da mesma lista G8 para conferir a observação das chamadas. Esse teste serve primeiro para localizar o custo, não para anunciar ganho de velocidade. Não há outra otimização de conteúdo ou de transações nesta atualização.

## Validação concluída desta entrega

Geração completa do APK (assembleDesempenho) e Lint: BUILD SUCCESSFUL. Passaram 10 verificações do wrapper/contexto com interface nativa simulada, 18 cenários de regressão do importador, 15 de sincronização e 2 de armazenamento, 8 de SQLite real/adaptadores gerados, aplicação/reaplicação/restauração/rejeição de conflitos, sintaxe PowerShell e filtro de dados privados. Não houve execução da nova fronteira na TV neste ambiente.

Arquivos reais alterados em /workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/: PlaylistCacheDatabase.kt (instala o observador e remove os hooks anteriores), M3uTransactionMetrics.kt (campos de fronteira/contexto), importer/M3uCacheImporter.kt (contadores/identificação) e M3uMeasuredOpenHelperFactory.kt (novo wrapper). O esquema gerado permanece com versão e identidade iguais.
