# Diagnóstico dos testes de retorno/rolagem — 1.07

O log enviado registra dez testes, seis falhas. Os dois testes das telas reais voltando à última capa e os dois testes de rolagem com a moldura real passaram. As falhas são os quatro casos sintéticos de setas e os dois de roda do mouse.

Os casos sintéticos não aplicavam LocalBringIntoViewSpec da grade real. O foco padrão do Compose podia reposicionar a grade enquanto o navegador também a rolava. Agora usam exatamente a mesma instância de política da moldura de produção; somente sua visibilidade Kotlin passa de private para internal, sem alterar o comportamento.

A roda usava 66 pixels fixos e esperava avançar à próxima fileira. No log, o card de 100dp mede 200px, portanto a fileira com espaço mede 220px: 66px são apenas 30%, e o alinhamento correto é voltar à fileira zero. O teste passa a mover 60% da fileira real medida em pixels, preservando a expectativa de avançar uma fileira em qualquer densidade. As verificações de foco, deslocamento intermediário, reversão, oito/dez capas completas e offset final zero permanecem.

Acrescentados dois casos de roda na moldura real. São agora doze testes: dez de rolagem/alinhamento e dois de retorno. App e APK de testes compilados. A nuvem não tem emulador: a nova suíte precisa ser executada no dispositivo; não afirmar que os doze passaram antes desse resultado.

Somente dois arquivos, com hashes, backup e rollback. Não altera o algoritmo de navegação, curva de rolagem, geometria, cache, seis downloads, site ou dados. -TestarNavegacao com -Emulador compila/instala app e testes por cima e executa os doze. Salva diagnostico-retorno-rolagem-corrigido.txt em UTF-8. Falha continua interrompendo o instalador e preservando o APK, sem mascarar resultados.
