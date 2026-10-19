# Navegação de filmes e séries — 1.07 / interno 9

Voltar, quando as capas estão em foco, retorna à barra de categorias. O grupo de categorias restaura a última categoria focada, mantendo sua posição de rolagem. Outro Voltar, já fora da grade, mantém a navegação normal da tela. Com o teclado aberto, Voltar continua fechando o teclado primeiro.

Cima na primeira fileira direciona explicitamente ao buscador. Baixo no buscador retorna à grade, preservando a posição/coluna do catálogo. Esquerda na primeira coluna também retorna às categorias; os demais comandos não escapam lateralmente nem fazem wrap para a primeira coluna. Funcionam nas grades de quatro/cinco capas de filmes e séries, incluindo Favoritos e Continuar.

Setas entre capas inteiramente visíveis pedem foco diretamente, sem passar pelo host e esperar quadros da tela. Rolagem para itens ainda não compostos usa um único trabalhador que acompanha o último destino solicitado. Repetições atualizam esse destino em vez de cancelar/recriar uma tarefa a cada comando. Mantém todos os comandos na conta e a coluna escolhida. Cancelar/Voltar impede que uma tarefa antiga roube foco depois. Uma seta durante a entrada inicial substitui a restauração automática, sem deixar o foco parado no host.

A estrutura da navegação foi comparada com o vídeo de referência enviado. A abordagem mantém resposta imediata aos comandos enquanto a arte é carregada: não espera downloads para mover o foco. Não muda o carregador de capas, prioridades de 15 em 15, Paging/caches, limites de memória, foco neon, tamanho de quatro/cinco capas, reprodução ou regras de busca/histórico. Mantém o histórico global em segundo plano, aviso somente na Home após troca de lista e reaberturas silenciosas.

Validação: 12 testes JVM exercitam a classe real de navegação com grade/foco/tempo simulados; não medem FPS nem comportamento do Android. A suíte nativa inclui 13 testes Compose (quatro novos), compilados, disponíveis no instalador para emulador/TV. A nuvem não tem dispositivo Android; a fluidez e a restauração reais precisam ser conferidas nele. Compilação e lint verificados, com minSdk28.

Cumulativo 1.07/interno 9. Instalador com hashes, backup/rollback e -Verificar. Preserva listas, chaves, favoritos e histórico, aceita APK x86 único e mantém assinatura/configuração local.
