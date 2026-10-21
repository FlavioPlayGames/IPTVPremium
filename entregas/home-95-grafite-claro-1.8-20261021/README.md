# Home 5% menor e ícones em grafite no Claro — 1.8

- Os cinco PNGs principais da Home foram reduzidos em 5%, com arredondamento de pixel. As bases ficam alinhadas e os espaços entre ícones têm 42–43 pixels na referência de 1672 pixels de largura; a seleção ainda tem folga para crescer 10%.
- Os nomes continuam nítidos e centralizados sob os ícones. As áreas clicáveis acompanham a redistribuição. O tamanho das imagens também foi reduzido em 5% na Home vertical.
- Ícones pequenos das categorias de filmes e séries usam grafite acetinado no modo Claro. O logo, o contorno de seleção e as cinco cores da Home permanecem na cor escolhida. Escuro e Kids mantêm seus materiais.
- Sem alteração dos PNGs existentes, cache, player, importação, histórico ou versão 1.8.

[Prévia de composição da Home no modo Claro](previa-home-claro.png). É uma composição auxiliar das medidas e imagens usadas, não uma captura do emulador Android.

## Verificação

Compilação aprovada: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin. Cinco testes JVM de navegação passaram, incluindo distância entre ícones, crescimento dentro das áreas de ação e preservação dos alvos do controle. A seleção de 90 testes Android está compilada e será executada por -Testar no emulador do usuário; não há emulador nesta máquina cloud. Instalador validado em arquivos atuais e UTF-8 BOM/CRLF, com backup exato, reaplicação, preservação de Kids/configuração privada/PNGs e rejeição atômica de edições desconhecidas. Nenhum APK publicado.
