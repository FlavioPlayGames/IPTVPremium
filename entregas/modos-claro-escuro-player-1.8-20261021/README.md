# Modo Escuro, Claro e player — 1.8

- Aparência renomeada para **Escuro** e **Claro**, mantendo a preferência salva e as cinco cores metálicas.
- Seleção de categorias com preenchimento mais contrastante e contorno reforçado; no modo Claro, o metal do contorno usa tons mais escuros. Ícones e capas mantêm seus tamanhos.
- Cabeçalhos de detalhes de filmes, séries e configurações usam fonte sem serifa fora do Kids.
- Player regular usa o visual escuro translúcido aprovado nos dois modos: controles brancos, preenchimento neutro e contorno branco, incluindo os menus de áudio, legendas, velocidade e tela. Paleta interna do Kids preservada.
- Não altera importação, histórico, favoritos, motores de vídeo, legendas exibidas no filme ou tempo mínimo de preparação.

## Home: primeiro teste de recorte

[Ver PNG transparente de teste](prototipo-canais-transparente.png). É um estudo em alta resolução do ícone Canais com volume e base metálica; ainda não substitui a Home instalada. O tamanho e a arte atual dos cinco ícones foram preservados neste pacote. Precisamos conferir o recorte antes da substituição de todos; os futuros nomes serão texto nativo para manter a nitidez.

## Verificação

Compilação cloud aprovada: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin. Instalador validado em duas bases, com backup exato, preservação do teste local de Kids e configuração privada, reaplicação e rejeição atômica de edições desconhecidas. Não há emulador nesta máquina: teste Android e conferência visual devem ser feitos no emulador do usuário com -Testar. Sem publicação de APK.
