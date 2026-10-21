# Home com PNGs e blocos suaves — 1.8

- Os cinco ícones principais da Home agora são PNGs com transparência real. São posicionados nos mesmos retângulos e tamanhos nominais da Home anterior e usam as cinco cores já existentes.
- Os fundos Claro e Escuro foram preparados sem os objetos e nomes antigos. Mármore e arquitetura ficam parados; ícones, sombras e reflexos são camadas separadas. O crescimento continua em 10%, com transição de 180 ms e a base apoiada no chão.
- Os nomes são texto nativo sem serifa, renderizado no tamanho atual da seleção; não são letras ampliadas de uma imagem. A versão vertical também usa os PNGs.
- Realces quentes da pipoca continuam equilibrados durante a primeira decodificação em IO.
- Blocos de categorias voltam a ficar discretos: menos preenchimento, bordas mais suaves e um único contorno fino na seleção. A suavização vale para Claro e Escuro; a aparência interna do Kids permanece independente.
- Cache compartilhado limitado a 18 MiB, imagens reutilizadas entre as cinco cores e sem desfoque contínuo. A Home principal decodificada usa aproximadamente 14 MiB de bitmaps; os dois fundos são alternativos, com descarte pelo cache.
- Tamanhos, áreas clicáveis e regras de navegação preservados. Player, Listas, controle parental, importação e histórico não são alterados nesta entrega.

## Prévias de composição

[Escuro](previa-home-escuro.png) · [Claro](previa-home-claro.png)

As prévias são composições auxiliares com as imagens e medidas utilizadas, não capturas do emulador Android. No aplicativo, o reflexo tem esmaecimento e a pipoca recebe o equilíbrio de realces.

## Verificação

Compilação aprovada: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin. Cinco testes JVM de navegação passaram. Dois novos testes Android verificam transparência dos PNGs, recortes, dimensões dos fundos e orçamento de bitmaps; a seleção completa soma 90 testes e deve ser executada no emulador do usuário. Não há emulador nesta máquina cloud. Instalador validado com backup exato, preservação dos arquivos locais de Kids/configuração privada, reaplicação e rejeição atômica de edições desconhecidas. Nenhum APK publicado.
