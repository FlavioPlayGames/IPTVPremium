# Correção da medição de transações SQLite

As três rodadas G8 tiveram mediana de 125,697 segundos. Cerca de 66 segundos ficaram depois do corpo das inserções. Os ganchos anteriores begin/end não foram chamados pelo caminho usado pelo Room; os valores zero significam medição ausente.

Esta atualização corrige a observação da interface SupportSQLiteDatabase do mesmo auxiliar Android. Mantém G8, chamadas de 250, limites, SQL, índices, política de conflito e banco 5. Não inclui outra otimização de velocidade. A meta de 25 segundos continua pendente.

## Aplicar e instalar

Extraia e execute na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Somente após BUILD SUCCESSFUL:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

## Uma coleta para verificar o diagnóstico

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -Estrategia G8 -Rodada 1 -PastaSaida "C:\Users\flavi\Downloads"
```

Aguarde Captura iniciada e atualize a mesma lista M3U direta UMA vez na TV, sem apagar dados ou remover a lista. Envie o TXT cujo nome contém g8-r1-sqlite. Para confirmar primeiro o ponto de medição, basta essa execução; ela não serve isoladamente para provar ganho de velocidade.

Informe -Dispositivo com o serial do adb devices se houver mais de um aparelho conectado. O coletor salva somente campos permitidos de desempenho e aguarda também uma breve janela de finalização da interface.

## Campos novos

boundary=SUPPORT_SQLITE_R1 identifica o ponto corrigido. beginUs/endUs passam a medir chamadas efetivas da interface SQLite, separadas de chamadas aninhadas (nestedBeginUs/nestedEndUs e contadores). Os zeros da captura antiga eram ganchos do Room não observados e não devem ser comparados como tempo real zero.

contextGroups deve indicar em quantos grupos o contexto chegou ao corpo; contextMeasured aparece por grupo. beginCalls/endCalls precisam ser observados para interpretar os tempos. endUs contém toda a chamada SQLite, podendo incluir commit, checkpoint, espera ou callbacks; não mede apenas fsync. afterBodyUs continua abrangendo o retorno completo depois das inserções, incluindo intervalos sobrepostos aos de endUs. Não some indiscriminadamente.

seriesWritten conta entidades de séries enviadas aos métodos de inserção, não séries únicas no banco. seriesPrepared conta preparação de metadados; não equivale ao número de linhas persistidas. Nenhuma consulta COUNT foi acrescentada ao caminho de importação.

RELATORIO.md contém a análise das três capturas. O instalador valida a base m3u-otimizacao-completa e cria backup; Desfazer.ps1 restaura somente os arquivos desta atualização. Não há migração nem alteração do site.
