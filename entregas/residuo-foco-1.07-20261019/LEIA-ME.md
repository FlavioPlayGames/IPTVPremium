# Encerramento da rolagem — 1.07

O diagnóstico enviado mostra 14 testes executados, 10 aprovados e quatro falhas de inversão. Todos os quatro registram first=0 offset=1 scrolling=true, ainda antes da transferência do foco. A correção anterior não alcançava o alinhamento final porque o laço da animação permanecia ativo.

A condição de término aceitava erro menor que meio pixel. A grade representa o deslocamento com pixels inteiros: no limite superior um pequeno resíduo pode não ser consumido e manter o laço ativo indefinidamente. Agora, com velocidade baixa e erro de até 1,5 pixel físico, termina a animação e executa o alinhamento exato já existente antes de transferir o foco. Não há aumento do prazo de 900 ms nem retirada de verificações.

App e APK de testes compilados. Os mesmos 14 testes precisam rodar no emulador; não há emulador disponível nesta nuvem. O pacote inclui dois arquivos, sendo o teste idêntico ao anterior. A única alteração de produção é a tolerância final, sem mudar curva, velocidade, colunas, downloads ou dados.

Use -TestarNavegacao e -Emulador. Resultado em diagnostico-residuo-foco.txt. O instalador valida hashes, faz backup e instala por cima sem limpar dados.
