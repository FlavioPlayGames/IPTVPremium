# Comparação Xtream — referência enviada e projeto atual

Comparação feita em 11/11/2026, usando os `.kt` ativos do `java.zip` enviado. Arquivos `.backup-*` não foram usados como implementação. Diferenças de BOM e quebras de linha Windows/Linux foram normalizadas; os conteúdos não foram executados. O ZIP de referência e seus backups não são publicados nesta entrega.

## Resultado

**As configurações de desempenho e o fluxo principal de importação Xtream já são iguais à referência.** Não havia outro tamanho de lote, outra política HTTP ou outro perfil padrão para restaurar a partir desses fontes.

| Componente | Referência e código atual |
| --- | --- |
| Lotes de leitura Xtream | 500 itens / limite de 256 KiB |
| Transações bufferizadas do escritor | limite de 2.000 registros |
| Leitura de filmes/séries | progressiva, com contrapressão; sem catálogo inteiro em RAM |
| HTTP de importação por leitor | conexão de 10 s, leitura de 15 s |
| Recuperação por categorias | janela ordenada de até três respostas em andamento |
| Perfil padrão de importação | PADRAO; configurações alternativas somente quando permitidas pela variante debug |
| Compartilhamento de sincronização repetida | mesma implementação |
| Ativação READY→ACTIVE | mesma implementação |

A comparação também confirmou igualdade no trecho completo de `importXtream` e dos auxiliares de leitura/recuperação progressiva em `DefaultPlaylistRepository`. São preservadas as mesmas chamadas, tentativas e ordem de mesclagem. Os hashes abaixo documentam igualdade de fontes, não desempenho medido na TV.

## Diferenças relevantes encontradas

- O leitor/parser atual preserva campos adicionais de capa e IDs externos TMDB/IMDb; o escritor grava os dois IDs opcionais no cache de filmes. Esses campos sustentam as correções de capas e identificação entre listas. Isso acrescenta dados ao processamento/gravação, mas o ZIP não permite quantificar seu custo na TV. Os lotes e o fluxo de rede não foram alterados por isso.
- A negociação de transporte M3U passou a respeitar escolha explícita e reavaliação controlada. O caminho Xtream direto e sua recuperação permanecem iguais à referência.
- O repositório recebeu integração XMLTV para o guia dos canais e limpeza do caminho M3U antigo. A descoberta Xtream deriva o endereço XMLTV localmente; não baixa o guia na importação. Não foi encontrada espera de download XMLTV no caminho Xtream direto.
- A finalização continua agendando o histórico para depois da liberação, sem aguardar sua transferência antes da Home. Há diferenças de isolamento normal/Kids que foram preservadas.
- Consultas novas de capas/metadados podem coexistir com uma importação se já estavam iniciadas. A r14 estende o cancelamento/retomada dessas tarefas à prioridade de importação, além da prioridade do conteúdo selecionado.

Não foi revertido o leitor para perder os novos campos de capas/IDs. As configurações já coincidentes foram mantidas. A alteração de prioridade elimina uma possível competição identificada no código, mas ainda não prova que ela era a causa da demora observada.

## O que falta medir

O ZIP contém fontes, sem a variante/configuração Gradle utilizada no teste antigo ou o estado de preferências da TV. Igualdade dos fontes não confirma igualdade de perfil efetivo, resposta do servidor, quantidade de registros ou estado/tamanho do banco.

`Coletar-Importacao.ps1 -AoVivo` registra cabeçalhos/rede, caracteres e registros lidos, processamento, gravação, totais da sincronização, ativação e os tempos de navegação existentes. Captura o último perfil de banco disponível no log, se ainda estiver no buffer. Não inventa esse dado quando ausente. Alguns temporizadores de finalização só existem no fluxo de abertura ou de seleção de listas; o coletor exporta os disponíveis.

## Igualdades verificadas

```text
IGUAL data/cache/ImportTuning.kt sha256_atual=9dca2d9ae21b6485d588491bd82b9b71c2670e42ce75f332497c0b15cc94d981
IGUAL data/cache/ImportTuningPolicy.kt sha256_atual=4207b8d7a073153cc55ad683c7655e461166373e34035a3ac60663aa8f9402f2
IGUAL data/remote/DefaultPlaylistHttpClient.kt sha256_atual=f76fa21c2fb4730ce44aea80d29b3adb2cb9a36612a1494bd3f811b182d25c19
IGUAL data/remote/PlaylistHttpClient.kt sha256_atual=f87919318594e69f07fc2e6dea35310d93851ea902d7f67814475189149d2b65
IGUAL data/parser/XtreamArrayStream.kt sha256_atual=c3daabe88c65f4a4e1555d86dc5209b837372d4cb2293f233b42b5a914c6b11f
IGUAL data/sync/ProgressiveXtream.kt sha256_atual=00541fced613ee1086dfd1044e5b47a6fd0f443aa4167a5beb0e0e11987c5a28
IGUAL data/sync/ImportAlternatives.kt sha256_atual=18f5aba29e423ab546c5b8d686439884eabe991e501bd35d3b2a33c2d0de5dfc
IGUAL data/sync/SharedSync.kt sha256_atual=f34f92603f1eeacf9c0d47a312755b4bdffc7dce0a5877aabe648c59b6053ae4
IGUAL data/cache/PlaylistCacheGenerationManager.kt sha256_atual=ed73bc874be22a829760195756612e92eb32bf404ae85ae120c8875cbf54a82e
IGUAL data/repository/XtreamAuthenticationRecovery.kt sha256_atual=a69654996bb0a95779b8349e661517c9df4165c558a218f20e44f93120244751
IGUAL fluxo_progressivo_e_recuperacao_Xtream sha256_atual=46e00e42e6cb29ff161bf19c5067d60c6d0cb90d254de26759117e70734fc7c9
```

Pode reproduzir a comparação sem alterar arquivos:

```powershell
python .\testes\Comparar-Xtream.py --referencia "CAMINHO_DO_JAVA_ZIP" --projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```
