# Foco de entrada — 1.07 / interno 9

Corrige a entrada na grade: o contêiner estável transfere foco real à capa assim que ela está pronta. A transferência temporária durante navegação rápida continua protegida. Selecionar novamente a categoria atual também solicita a primeira capa, incluindo Continuar assistindo. Séries aguardam a nova página antes de solicitar a primeira capa, para não focar um título da categoria anterior. O contorno neon fino +25% acompanha o foco real, sem sublinhado.

Atualiza versionName para 1.07 e versionCode para 9, incluindo os padrões do Gradle e as duas propriedades locais do instalador. Permite instalar sobre 1.06/interno 8 com a mesma assinatura. Bloqueia downgrade de projetos com interno maior que 9. Listas, favoritos, progresso, chaves e configurações ficam preservados.

Quatro capas por fileira: oito pôsteres verticais 2:3, com o maior tamanho que cabe em duas fileiras completas. O próprio quadro e o contorno têm a largura da imagem, eliminando o espaço lateral preto causado pelo quadro largo. Os cards ficam centralizados em quatro células iguais, com distâncias regulares. Nome e nota ficam abaixo de cada pôster; notas ausentes aparecem como ★ —. Texto ampliado, margens, placeholders e brilho entram no cálculo. Nomes longos continuam rolando apenas no card focado. Cinco colunas mantêm o modo anterior. Mantém prioridade das primeiras capas, cache, limite de memória e Atualizar/reinício.

Compilação dos cinco APKs e testes Android, lint e verificação de assinatura. Nove testes nativos de controle compilados, incluindo entrada pela grade com foco na primeira capa. Não executados aqui, por ausência de emulador/KVM. -CapturarLog instala por cima, executa os nove testes uma vez e captura o diagnóstico.

Instalador cumulativo com hashes, backup e rollback. -Verificar apenas confere compatibilidade.
