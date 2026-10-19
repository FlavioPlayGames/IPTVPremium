# Inversão de rolagem e foco — 1.07

O diagnóstico recebido mostra 10 de 12 testes passando. Restam os dois casos de inversão rápida de direção, com quatro e cinco colunas: a capa existe, mas não recebe o foco final.

O navegador agora confirma o alinhamento exato da fileira ao terminar a animação, antes de devolver o foco. Um resíduo fracionário de deslocamento pode provocar novo alinhamento concorrente; essa é uma hipótese compatível com o log, ainda dependente de confirmação no emulador. Novos comandos recebidos durante o alinhamento continuam tendo prioridade.

As mesmas verificações e o prazo de 900 ms permanecem. Acrescentados dois casos de inversão com a moldura real. São 14 testes no total. App e APK de testes compilados com sucesso; não há emulador nesta nuvem e não foi afirmada aprovação dos testes nativos.

Execute com -Projeto, -Emulador e -TestarNavegacao. O instalador valida hashes, faz backup, aplica apenas dois arquivos, compila e instala por cima sem limpar dados. Resultado em diagnostico-inversao-foco.txt. Falhas continuam interrompendo o instalador. Mantidos os seis downloads de capas e as demais correções.
