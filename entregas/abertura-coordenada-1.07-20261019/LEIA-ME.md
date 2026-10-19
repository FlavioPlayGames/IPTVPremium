# Abertura coordenada — 1.07

Meta: reduzir o trabalho concorrente da abertura e medir o tempo real até a interface utilizável. Nenhum resultado de 15–20 s na TV foi afirmado: ainda é necessário medir no aparelho. Não foram alterados timeouts, velocidade da rolagem, memória de imagens, lista, licença, player ou site.

## Alterações

- MainActivity não dispara limpeza no onCreate. A manutenção aguarda a interface utilizável; a primeira passagem só pode iniciar após 60 segundos de ociosidade. Nova abertura ou importação cancela cooperativamente uma passagem da manutenção; a importação espera a liberação da limpeza antes de começar. Cancelamento/falha e importações simultâneas não deixam a manutenção bloqueada. Limpezas continuam em lotes e retomam depois, sem descartar o agendamento.
- A splash pode terminar depois que a política existente aplicou uma licença armazenada ou recebida da central (mac e serverNow). Uma conta bloqueada/expirada continua impedindo AppNavigation pelo mesmo PortalAccess.message; não há temporizador abrindo acesso. Consulta de comandos e ack podem continuar depois da confirmação do acesso. Primeira instalação ainda exige registro/validação.
- Após confirmar a geração ACTIVE, grava um comprovante do pedido remoto exato antes da preparação final. Se o processo for interrompido nessa janela, a próxima abertura reaproveita o catálogo existente. Novo comando tem identificador diferente e invalida o comprovante; cache ausente continua exigindo importação. Não trata qualquer remoto=true como pedido duplicado e não apaga comandos novos.
- StartupPerf mede inicialização de tema/atualizador/push/categorias/parental, preferências resolvidas, acesso local, registro/pull/ack, consulta/ativação do cache, importação, finalização e total desde onCreate até interface_liberada. Não registra URLs, credenciais ou corpos.

## Validação

Oito testes JVM do coordenador de disco e quatro de recibos com DataStore real passaram. Os mesmos 12 testes foram compilados para Android; a adaptação JVM do teste de recibos troca somente o diretório cache do Android por uma pasta temporária local. App e APK de testes compilados. Medidor exercitado com ADB simulado: captura nova sessão, fecha/reabre uma vez, reconhece interface liberada e encerra apenas a captura que iniciou. Não há emulador/TV disponível nesta nuvem.

## Aplicar e testar

Execute Preparar-Abertura-Coordenada-1.07.ps1 -Projeto <raiz> -Emulador <serial ADB> -TestarAbertura. Aceita os arquivos do src.zip enviado ou a revisão com mensagem simples de erro. Valida todos os hashes antes de gravar, faz backup e instala por cima sem limpar dados. Resultado diagnostico-abertura-testes.txt. Não publicar APKs se o teste falhar.

## Medir na TV

Instale a mesma compilação na TV (o instalador aceita o serial ADB da TV em -Emulador). Conecte por ADB na rede local; consulte adb devices. Execute Medir-Abertura-1.07.ps1 -Dispositivo <serial da TV> na pasta do projeto. O medidor encerra e reabre o app para medir uma abertura de processo, preservando todos os dados e cache. Captura até interface_liberada ou 75 s e salva diagnostico-abertura-tempos.txt. Não limpa o logcat global. O prazo de captura não modifica a abertura do app.

Primeiro medir uma reabertura com catálogo salvo. Medir primeira importação separadamente; apagar os dados elimina licença/configuração/cache e não foi incluído no medidor. Para compartilhar o diagnóstico, enviar diagnostico-abertura-tempos.txt. cache=true, remoto=false indica reutilização normal; pedidoJaImportado=true com cache=true identifica recuperação do mesmo pedido sem novo download. Se catalogo_importacao dominar, a próxima otimização deve atuar no download/parser/banco, sem atribuir os segundos à splash.
