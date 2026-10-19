# Velocidade do botão segurado — 1.07

O diagnóstico anterior do usuário aprovou os 16 testes. Esta entrega aumenta em 25% a resposta e o limite de velocidade da trajetória somente durante repetição nativa do DPAD. Cliques individuais mantêm o fator 1. Ao soltar, mantém o descarte de destino acumulado, escolha da fileira mais próxima, mesma coluna e reinício da trajetória sem impulso anterior.

Somente CatalogGridNavigation.kt e CatalogScrollMotion.kt. Não altera layout, memória, downloads, dados ou testes. A configuração da velocidade não significa medir um tempo de navegação exatamente 25% menor: o resultado depende dos comandos e da distância.

App e APK de testes compilados; quatro verificações JVM da curva em 24/30/60/120 fps passaram. Os mesmos 16 testes nativos precisam ser repetidos no emulador para esta nova versão; não há emulador disponível nesta nuvem. Use -TestarNavegacao e depois avalie segurando o botão no controle da TV.

Instalador valida hashes, faz backup e instala por cima sem limpar dados. Resultado diagnostico-velocidade-controle.txt.
