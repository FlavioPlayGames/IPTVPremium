# Claro mais suave no aplicativo — versão 1.8

- Mármore, fundos e prévia da Home no modo Claro recebem 14% menos componente branco neutro. As diferenças entre os canais de cor do metal são preservadas.
- Fundo, painéis, campos e botões que compartilham a paleta clara usam tons cinza-perolados menos brilhantes. Isso cobre catálogos, Listas e Configurações.
- PNGs dos ícones da Home, nomes nativos, capas, fotos e conteúdo dos vídeos não são escurecidos. Os ícones das categorias continuam em grafite, e logo e seleção mantêm a cor escolhida.
- Modo Escuro e aparência interna do Kids preservados. Tamanho, espaçamento e animação da Home também preservados.
- Ajuste feito na paleta e no filtro de cor já usado pelo fundo; não acrescenta imagens, bitmaps ou desfoque contínuo.

[Prévia de composição do Claro suavizado](previa-claro-suave.png). É uma composição auxiliar com as mesmas imagens e medidas, não uma captura do emulador.

## Verificação

Compilação aprovada: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin. Instalador validado em arquivos atuais e UTF-8 BOM/CRLF, incluindo backup exato, reaplicação, preservação de Kids/configuração privada/PNGs e rejeição atômica de edições desconhecidas. A conferência visual e a execução da seleção de 90 testes Android devem ser feitas no emulador do usuário com -Testar; não há emulador nesta máquina cloud. Nenhum APK publicado.
