# Relatório — episódios M3U sob demanda

Implementado no catálogo M3U nativo e no protótipo separado. A importação inicial grava canais, filmes, categorias e metadados finais das séries. Os episódios ficam em arquivos privados compactos, separados por playlist e geração. Abrir uma série prepara e grava somente seus episódios no Room; novas aberturas reutilizam esse cache. Não há nova chamada HTTP para montar os episódios.

A M3U ainda é lida por completo. Ao contrário da API Xtream, ela não oferece consulta por série. A fonte local indexada permite evitar a criação de UUIDs/entidades e todas as gravações de episódios no carregamento inicial. O índice contém somente uma entrada por série; os registros usam encadeamento por offsets, reconstruindo a ordem original na abertura. Metadados finais de séries são persistidos uma vez por série.

A primeira abertura tem o custo de materialização dos episódios daquela série. As rotinas existentes de foco/Continuar também podem solicitá-los. O cache é transacional, respeita cancelamento e confere a geração ativa antes de gravar. Requisições simultâneas são coordenadas. Marcador de cache é escrito após commit; falha do marcador não esconde episódios já gravados. Fontes incompletas são rejeitadas. A limpeza retém gerações ainda referenciadas no banco; fontes órfãs de playlists excluídas não têm coleta global nesta alteração.

## Compatibilidade

Banco Room versão 5, identidade 579c4ad4cd357bb9f2d064c3011bd3fa, sem migração ou dependência nova. IDs, links, capas, números, ordem e última ocorrência duplicada foram comparados com o importador anterior. Os quatro arquivos protegidos do caminho Xtream têm hashes idênticos aos anteriores (validacao/xtream-before.json). AppNavigation adiciona integração somente nos caminhos M3U.

## Validação

APK desempenho e Lint concluídos com BUILD SUCCESSFUL. Testes JVM/SQLite: 18 cenários de carregamento/cache/rollback/concorrência/corrupção; 3 do snapshot; 2 de volume; 18 regressões do importador; 17 de sincronização/armazenamento. Lista sintética com 329.732 registros e 283.278 episódios: zero chamadas de inserção Room de episódios na importação inicial, fonte indexada completa. Execução dos testes novos com heap de 128 MiB. Isso verifica funcionalidade, não velocidade na TV.

PowerShell: sintaxe dos cinco scripts, aplicação idempotente, backup/restauração, bloqueio de divergência/corrupção e conflito ao desfazer. Coletor com ADB simulado: término correto, limpeza restrita e remoção de links/credenciais. AtomicFile é simulado nos testes JVM; não houve ensaio físico de queda de energia Android.

## Aplicação e medição

Leia LEIA-ME.md. Aplicar sobre m3u-snapshot-prototipo, compilar e reinstalar. Atualizar uma vez a mesma lista M3U nativa, sem apagar dados. Capturar importação e abertura/reabertura de séries pelo script de medição. A velocidade precisa ser medida no aparelho; nenhum prazo de 25 segundos é garantido. Ao desfazer, reinstalar e atualizar as listas M3U para voltar a preencher todos os episódios no modelo antigo.
