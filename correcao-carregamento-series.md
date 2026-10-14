A tela de carregamento de series foi corrigida no projeto fornecido.

- A imagem de fundo e exibida com Crop, preservando sua proporcao.
- O interior da TV foi medido no asset original de 3840 x 2160: x=1289..2702, y=395..1194. A mesma escala e centralizacao do fundo posicionam a imagem e a mascara.
- A imagem da serie preenche apenas esse painel, sem esticar nem cobrir a moldura. O texto acompanha o tamanho do painel.
- A ligacao agora mostra uma pausa com a TV apagada, um ponto luminoso, uma linha horizontal, abertura vertical e entrada gradual da imagem. O brilho desaparece antes da aproximacao da camera.
- A aproximacao preserva a proporcao da sala e coloca a moldura e os cantos arredondados fora do viewport ao terminar.
- A animacao usa o ID estavel da serie; uma atualizacao tardia do titulo nao reinicia a abertura.
- A navegacao continua com uma unica tela de detalhes, preservando foco, retorno do player, botao Voltar e limite habitual de cerca de 2 a 2,5 segundos com a escala normal de animacao do dispositivo.

Arquivos de codigo alterados:
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/series/SeriesLoadingOverlay.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/series/SeriesOpeningGeometry.kt (novo)
- app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/AppNavigation.kt (openingKey = seriesId)

Validacao:
- Compilacao Kotlin e assembleDebug aprovados na versao final.
- Harness Kotlin independente: 16.111 verificacoes aprovadas, incluindo 1280x720, 1920x1080, 1024x768 e 2560x1080, 1.001 quadros de ligacao, limites da imagem e inclusao dos cantos finais na mascara arredondada.
- Lint: os mesmos 29 erros Media3 preexistentes, 56 avisos e 4 sugestoes. Nenhum erro novo na comparacao com o projeto original; o lint continua falhando pelos erros antigos.
- A renderizacao e a aparencia da animacao nao foram testadas em dispositivo/emulador Android.

O ZIP conserva a limpeza anterior e inclui local.properties.example com o campo TMDB_READ_TOKEN vazio. local.properties com a credencial, caches, builds e configuracoes locais do IDE ficam fora da entrega.
