# Corrigir as tentativas automáticas de abertura de filmes

O prazo anterior usava cancelamento estruturado: podia continuar esperando uma conexão bloqueada terminar sua limpeza. Além disso, a abertura podia esperar o mutex de uma antecipação do mesmo filme. As próximas tentativas gastavam seu prazo esperando trabalho antigo. O botão manual começava depois, com a consulta/cache já mais adiantados.

## Correção

- Até 3 tentativas de preparação, cada uma com até 5 segundos, sob um prazo único de 15 segundos. Falhas rápidas iniciam a próxima imediatamente.
- No timeout, cancela a tarefa anterior e solicita o encerramento de suas conexões. A próxima inicia sem aguardar uma limpeza de socket que pode bloquear no Android.
- Abertura em primeiro plano não espera o mutex de antecipação em segundo plano. A segunda e a terceira tentativa iniciam uma nova consulta ao provedor, sem reutilizar o resultado de metadados da tentativa anterior. Um cache já concluído continua válido na primeira entrada.
- Resultados atrasados de tarefas canceladas não podem concluir a preparação nem substituir a resposta válida da nova tentativa.
- Voltar cancela o ciclo; “Tentar novamente” começa outro ciclo com orçamento novo. Não garante que todo provedor responda em 5 segundos: continua mostrando a falha se nenhuma tentativa concluir.

O tempo de fechamento físico de uma conexão depende do Android/provedor. A correção garante que a espera por essa limpeza não segure o início da tentativa seguinte. Não altera reprodução de vídeo, títulos, fonte, fundos, navegação, limites de cache, importação ou histórico. Aplica-se à preparação da tela do filme.

## Aplicar

Execute `Corrigir-Tentativas.ps1 -Projeto (Get-Location).Path` na raiz do projeto, depois `.\gradlew.bat assembleDebug`.

Aceita o último java.zip enviado e as atualizações Prata/títulos conhecidas, preservando cada combinação. Inclui a ligação do MoviesViewModel com a política mesmo se a atualização anterior de três tentativas não tiver sido aplicada. No arquivo da tela, apenas instala a interrupção de downloads se ainda estiver ausente; arte, elenco e fonte permanecem como estavam. Bases desconhecidas são recusadas antes de qualquer gravação. Backup e restauração automática em falha de escrita.

## Validação

Testes com tempo virtual (limites exatos e descarte de resposta atrasada), cache com uma consulta de fundo segurando o mutex e cliente HTTP real com conexões simuladas bloqueadas tanto na leitura quanto no disconnect. Não é uma medição na TV do usuário. Fontes dos testes em `testes/kotlin`.
