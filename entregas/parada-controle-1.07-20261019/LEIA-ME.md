# Parada ao soltar o controle — 1.07

O diagnóstico continuous-trace-1 confirmou as quatro falhas na fase animate, com distância entre 1,04 e 1,56 pixel e velocidade entre 16,5 e 24,7 pixels/s. Não era espera por solicitante de foco. O encerramento passa a aceitar até dois pixels físicos com velocidade menor que 48 pixels/s; em seguida o alinhamento exato existente zera o deslocamento. Não altera o prazo dos testes.

As repetições nativas do botão segurado deixam de acumular destinos distantes: o destino fica limitado à região visível e à fileira adjacente. No KeyUp após repetição, descarta o destino acumulado, escolhe a fileira mais próxima da posição atual, preserva a coluna e zera o impulso da trajetória. Cliques separados continuam solicitando movimentos individuais. O teste de controle requer eventos Android com repeatCount, conforme a repetição nativa do DPAD.

Inclui dois testes novos, quatro/cinco colunas: segura o botão, envia repetições, solta e verifica parada a no máximo meia fileira da posição visível, foco na mesma coluna e duas fileiras completas. São 16 testes no total. App e APK de testes compilados, instalador validado. Os testes nativos ainda precisam rodar no emulador; não há emulador nesta nuvem.

Instala por cima sem limpar dados. Valida hashes, faz backup. Resultado diagnostico-parada-controle.txt. Apenas navegador e testes alterados. Preservados layout de oito/dez capas, imagens, site, dados e seis downloads. Testar também segurando e soltando no controle da TV após os testes automáticos.
