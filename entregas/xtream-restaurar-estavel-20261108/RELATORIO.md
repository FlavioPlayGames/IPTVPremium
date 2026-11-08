# Comparação e restauração da regra Xtream

Base antiga: java.zip informado pelo usuário nesta solicitação. Base atual: projeto atualizado pelos pacotes M3U oficial e correção de capas. O usuário confirmou que a demora ocorre ao importar/atualizar uma lista cadastrada por link M3U com reconhecimento automático.

## Diferença relevante

O sincronizador atual tentava a API duas vezes para uma lista automática nova/em renegociação. Se a API falhasse, podia iniciar a M3U nativa completa. A versão antiga mantinha a API escolhida com até quatro tentativas e não iniciava M3U nessa falha. A versão atual também transformava certas exceções da verificação inicial em escolha nativa; a antiga propagava o erro.

Restaurados esses dois comportamentos. A função syncXtreamWithRetry é textualmente idêntica à antiga. Preservados forceM3u, edição explícita do portal, decisão persistida somente após importação bem-sucedida e preservação da geração ativa. O M3uAccountProbe preservado considera negativa somente a ausência definida de API (HTTP 404/405); autenticação inválida, JSON incompleto, DNS e timeout permanecem erros. Uma verificação explicitamente negativa ainda permite a M3U; uma exceção não altera o formato. Cancelamento e erros locais permanecem terminais, sem tentativas desnecessárias.

A comparação encontrou HTTP, parser Xtream, writer, repositório de episódios, coordenação SharedSync e parâmetros ImportTuning idênticos. A parte syncFromXtream em diante do sincronizador é idêntica à antiga. Não se atribui uma lentidão geral a esses arquivos nem se afirma ter corrigido tempo de resposta do provedor. A mudança remove a possibilidade identificada de o caminho automático virar uma importação M3U longa após uma falha da API.

Não foi feito rollback integral do ZIP antigo, que usa banco versão 4 e contém versões anteriores de histórico/Kids e M3U. O banco atual 6 e todas as correções M3U permanecem. Esse pacote contém somente um arquivo Kotlin alterado e ferramentas de aplicação/medição.

## Validação

Consulte validacao para resultados finais. Testes de sincronização incluem API automática e direta, quatro falhas do provedor sem leitura M3U, cancelamento, armazenamento, resposta negativa versus exceção da verificação, edição explícita, preservação de catálogo e opção M3U direta. Testes do repository verificam leitura progressiva, preservação dos IDs e tratamento de falhas. O coletor inclui XtreamEndpointPerf e XtreamWritePerf, retentativas e transporte; reconstrói somente métricas permitidas, descartando URLs e credenciais.

A velocidade na TV ainda precisa ser medida. Se já havia uma decisão persistida de M3U nativa, ela é conservada até edição explícita, evitando migração silenciosa de IDs.

## Resultado final

APK desempenho e Lint: BUILD SUCCESSFUL. Testes aprovados: sync: 21, repository: 3, installer: 4, scripts: 2, collector: 1. Velocidade e transporte efetivo serão conferidos na TV pelo coletor após reenviar a mesma lista em modo automático.
