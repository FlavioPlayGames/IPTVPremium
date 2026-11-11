# Validação r13

## Evidência analisada

Relatório r12 enviado pelo usuário: 542 linhas. Os eventos são anônimos e podem repetir o mesmo item; não representam contagem de filmes distintos nem teste controlado antes/depois.

| Resultado final do carregamento | Eventos | Mediana | Faixa |
| --- | ---: | ---: | ---: |
| Capa do provedor, incluindo cache | 248 | 18 ms | 0–911 ms |
| Capa TMDB, incluindo tentativas anteriores | 15 | 7.188 ms | 2.135–13.044 ms |
| Sem capa resolvida | 6 | 45,5 ms | 15–119 ms |

As quinze conclusões TMDB envolvem cinco identidades anônimas repetidas. Há 19 resultados de timeout de imagem, oito respostas HTTP 502 e uma HTTP 404. O tempo total de uma conclusão TMDB inclui as tentativas anteriores; esses números não demonstram que a API do TMDB, isoladamente, levou esse tempo. Os logs originais e as identidades anônimas individuais não acompanham a entrega.

O código confirmou uma lacuna de associação: o resultado encontrado com título/ano/ID enriquecidos não podia ser localizado pelo item original em todos os caminhos de retorno. O estado visual também aceitava apagar um bitmap válido ao receber uma nova ausência. A correção trata ambos os caminhos.

## Compilação Android

`assembleDesempenho` e `lintDesempenho` passaram em 11/11/2026: 0 erros, 154 avisos e 6 sugestões. APK arm64 de referência: 97.097.414 bytes (16.384 bytes acima da r12), não distribuído. JDK 25/Gradle 9.6/SDK 37/NDK 28.2.13676358 neste host. Heap de 6 GiB usado somente na linha de comando do host, sem alteração no Gradle do projeto.

## Testes

- Código real do vínculo, resolução, metadados persistentes e fila de capas, com adaptadores explícitos para Android, rede e bitmap.
- Ficha nativa fornece ID/ano que faltavam no catálogo. Após o primeiro sucesso, três retornos recuperam a imagem sem novas consultas de imagem/provedor/TMDB.
- Processo JVM novo recupera o vínculo do disco e usa somente a URL pública já confirmada, sem busca de identidade nem consulta aos detalhes do provedor.
- Reserva e proteção da área visível obtêm a mesma URL do card.
- Isolamento por lista/item/campos de identidade; chave ausente não cria associação. URL privada do provedor é recusada no vínculo persistente. JSON salvo não inclui URL original, credenciais nem ID textual da lista.
- Manipuladores reais de resultado, extraídos dos catálogos normal e Kids, preservam o bitmap já carregado quando recebem ausência. Ausência sem bitmap anterior continua finalizando o fallback normalmente.
- Regressões: histórico/transferência entre listas, pesquisa global/limpeza, identidades verificadas, temporadas, filas, rotação/orçamentos, parser M3U e parsers Xtream normal/streaming com 1.100 filmes.
- Políticas reais de abertura da r12 novamente verificadas: resposta válida após cinco segundos, cancelamento, prioridade sobre capas, série selecionada independente de consulta antiga, imagens opcionais e carrossel.
- Scripts de aplicação/repetição/restauração/reaplicação, proteção de edições, criação/remoção do arquivo novo e sintaxe PowerShell. Executados em PowerShell 7 e simulação do retorno JSON do PowerShell 5.1; não em Windows nativo.
- Coletor com ADB simulado aceita HIT/SAVED e demais campos previstos e rejeita URLs/credenciais/campos extras.

## Limites da validação

Não houve conexão com a TV ou com o provedor/TMDB reais neste ambiente. O cenário do teste usa respostas e IDs fictícios; não afirma qual produção real corresponde a esses IDs. O resultado visual de Insaciável precisa ser confirmado na TV.

Não há novos recursos de imagem nem aumento de limites de RAM/disco. Não se afirma cumprimento do total de 400 MB sem a medição de armazenamento da TV. Nenhum APK, assinatura, configuração privada ou lista completa acompanha a entrega.
