# Home com ícones clássicos e reflexo — versão 1.09

Usa os cinco desenhos originais do ZIP enviado: Canais, Filmes, Séries, Kids e Listas. As imagens são incluídas com os bytes originais, preservando qualidade e transparência. Um filtro aplicado na renderização aproxima o metal da tonalidade atual, sem o brilho excessivo do dourado original, e combina com Dourado, Azul, Rosa, Roxo e Vermelho.

A Home mantém os modos Claro e Escuro, o fundo de mármore atual, logo e botões superiores. Os ícones ficam nas mesmas larguras e posições horizontais aprovadas após o ajuste de 5%; sua altura respeita a proporção do desenho antigo. A base de todos fica apoiada na mesma linha do chão. Nomes continuam como texto nativo e a seleção mantém o crescimento de 10% sem ampliar o mármore.

O reflexo independente usa a mesma cor do ícone, é invertido e comprimido verticalmente e desaparece gradualmente sobre o mármore. Há uma sombra curta de contato para manter a profundidade. A Home Kids interna não muda; apenas seu ícone de entrada na Home geral é o urso antigo solicitado.

Os cinco arquivos substituídos marble_icon_*.png são removidos quando correspondem às versões conhecidas. O pacote adiciona apenas os cinco originais agora solicitados, com nomes home_classic_*, e não restaura outras imagens excluídas. Imagem personalizada ou referência adicional recebe a proteção do instalador. O total dos cinco arquivos passa de 6668595 para 4955446 bytes.

Execute Preparar-Home-Classica-1.09.ps1 na raiz do projeto, após a limpeza anterior. Com o emulador aberto, use -Emulador emulator-5554 -Testar. Backup fora de res e instalação sobre o aplicativo existente, sem limpar dados. A versão continua 1.09.

Verificação: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin; cinco testes JVM reais de geometria/navegação; dimensões, transparência, proporção e orçamento de 18MiB; testes do instalador em arquivos normais/BOM/CRLF, imagens substituídas já excluídas, arquivo personalizado e referência XML adicional. Os testes instrumentados não rodam na nuvem; -Testar executa os 97 testes existentes no emulador.

A imagem previa-classicos-claro-escuro.png é uma composição de referência em Dourado com os arquivos originais e filtros correspondentes, não uma captura do Android. A aparência e o foco precisam ser conferidos no aparelho.
