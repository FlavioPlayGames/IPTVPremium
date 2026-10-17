# Sincronização segura — pacote aprovado

Base: IPTVPremium (28).zip e correções posteriores autorizadas. Implementação sem alterações de layout, teclado, player, cores, versão, assinatura, identidade, regras parentais ou limites de cache. Android mínimo 12 preservado. Nenhuma alteração de schema, índices ou versão do Room, e nenhuma exclusão de favoritos/histórico/listas.

## O que mudou

### 1. Gravações M3U

O lote atual de 250 itens foi mantido. O flush entrega categorias, canais, filmes, séries e episódios a um único método @Transaction do DAO. As listas temporárias só são limpas depois que a transação inteira retorna com sucesso. O Room gera a transação externa, e uma falha em uma das tabelas reverte as escritas daquele lote.

O Xtream já grava por lote via métodos @Insert do Room, que já são transacionais; não foi criada uma transação enorme envolvendo o catálogo inteiro ou a rede. Seu lote atual de 500 também foi mantido.

### 2. Sincronizações repetidas

SharedSync compartilha somente uma operação em andamento, não armazena resultados concluídos. O coordenador é compartilhado entre instâncias do sincronizador. Pedidos da mesma lista e configuração aguardam a mesma geração; mudanças apenas de nome não duplicam a operação. Mudança de URL, credenciais ou tipo de fonte cria uma operação separada, serializada por ID da lista. Listas diferentes podem continuar em paralelo.

Cancelar um dos aguardantes não interrompe os demais. Quando o último cancela, a operação é cancelada; uma nova operação na mesma lista aguarda a finalização da anterior, incluindo a limpeza. Erros não ficam armazenados como resultado permanente: uma nova tentativa pode executar normalmente. M3U compatível com Xtream e Xtream direto mantêm suas regras distintas de fallback.

### 3. Cancelamento e proteção de dados

A leitura HTTP verifica cancelamento entre blocos e linhas, e a importação verifica durante processamento/gravação e antes de marcar READY. Não houve mudança nos timeouts de conexão/leitura: uma chamada de rede bloqueada ainda pode precisar concluir ou atingir o timeout existente antes de observar o cancelamento.

O descarte da geração IMPORTING roda em contexto não cancelável no dispatcher de IO. Uma falha de limpeza é anexada à falha original, sem substituí-la. O descarte confere o status dentro de uma transação e nunca apaga gerações READY ou ACTIVE. A ativação também confere READY antes de despromover a geração atual.

### 4. Limpeza segura

No trabalho de manutenção já adiado pelo aplicativo, pode ser removida uma geração IMPORTING abandonada há mais de 24 horas, em lotes de 100 registros com as pausas já existentes. O registro de importações em andamento no processo protege operações ainda vivas, inclusive diante de mudanças de relógio. Uma limpeza cancelada deixa a geração incompleta disponível para retomada posterior. READY/ACTIVE não são selecionadas. Cada chamada continua removendo no máximo uma geração elegível (abandonada ou STALE).

Os downloads de capas já removiam seus .part no finally. Agora, antes de disponibilizar a pasta às primeiras requisições de capas do processo, também são excluídos resíduos de encerramentos anteriores que correspondam exatamente ao padrão próprio de arquivo SHA-256.img.part. Capas .img concluídas, arquivos de outros nomes e diretórios são preservados. Não existe limpeza genérica recursiva de temporários ou credenciais.

### 5. Métricas privadas

Os logs incluem tempo total, tipo de fonte, resultado, compartilhamento de operação e heap utilizado antes/depois. O heap antes/depois não é uma medição do pico. As contagens/tempos de gravação e etapas já existentes permanecem. Não se registram URLs, nomes de listas, IDs, credenciais, tokens, PIN ou corpo de respostas.

## Verificação executada

- Compilação debug das quatro arquiteturas e universal concluída; configurações de versão/assinatura inalteradas.
- Android Lint NewApi/InlinedApi sem erros com mínimo API 31.
- Código real do coordenador, importador M3U, sincronizador e cliente HTTP executado em JVM com coroutines reais e adaptadores de Android/DAO/repositório. Casos: 32 chamadas compartilhadas; cancelamento de um aguardante; cancelamento do último com espera pela limpeza; erros compartilhados e nova tentativa; configuração alterada; listas independentes; scope já cancelado.
- Importação real de fixture M3U mista com 620 entradas: contagens preservadas, rollback, proteção de ACTIVE/READY, cancelamento e preservação da exceção original quando a limpeza também falha.
- Sincronizador real: oito pedidos Xtream por duas instâncias e nomes distintos geram uma operação; mudança de credencial gera nova operação; falha do provedor mantém fallback M3U; cancelamento não inicia fallback.
- Limpeza: idade/status, lista diferente, geração ativa/pronta/recente e importação viva protegidas; manutenção interrompida retomada; seleção estrita de .part de capas.
- HTTP real contra servidor local: UTF-8, leitura por linhas, erro HTTP, cancelamento antes da próxima linha e logs sem marcadores privados.
- SQLite real com schema gerado pelo Room e consultas reais do DAO: rollback entre tabelas após erro de restrição, limpeza limitada a 100 registros e preservação de ACTIVE/READY/fresh/outra lista. Wrappers transacionais gerados pelo Room conferidos.
- Instalador PowerShell: simulação sem escrita, aplicação de seis fontes existentes e dois helpers novos, backups, repetição, BOM/CRLF, configurações privadas preservadas e rejeição de fonte alterada/colisão de arquivo antes de qualquer escrita.

Os testes com adaptadores e SQLite de desktop não substituem execução do Room no Android. Não houve teste físico/emulador com as listas privadas do usuário, e nenhum percentual de ganho de desempenho é prometido.

## Aplicar

Na raiz do projeto, execute:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Otimizar-sincronizacao-segura.ps1
```

```powershell
.\gradlew.bat :app:assembleDebug
```

O script valida os arquivos antes de escrever e cria backup em .iptv-backups/sincronizacao-segura-*. Pode ser repetido; -SomenteVerificar não altera o projeto. Não toca em local.properties, google-services.json ou chaves de assinatura.

APKs em app\build\outputs\apk\debug. Compile com a assinatura original e instale sobre a instalação atual, sem desinstalar nem limpar dados.

## Continua fora deste pacote

Meta total de 300 MB/orçamento de caches; mudança para WAL; aumento de lotes; paralelização e streaming do Xtream; atualização por ABI; otimização de índices do parental; buffers/renderers e upscale. Essas decisões continuam dependendo das aprovações e medições combinadas. Upscale fica por último.
