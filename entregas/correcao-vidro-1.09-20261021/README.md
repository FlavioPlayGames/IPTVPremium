# Correção visual — versão 1.09

A indicação de foco/clique das categorias agora fica recortada no mesmo formato arredondado do bloco, evitando a sobra cinza com cantos quadrados. Abrange as linhas de categorias de filmes, séries e canais no catálogo normal.

No modo Claro, o painel de categorias, seus preenchimentos e o painel das capas ficaram mais transparentes. Textos, ícones, contagens e imagens continuam com a opacidade original. O modo Escuro conserva sua paleta; o Kids conserva o visual anterior. A seleção metálica das capas da opção 2 e a versão 1.09 permanecem.

Execute Corrigir-Vidro-1.09.ps1 na raiz do projeto, após a entrega 1.09 anterior. Use -Emulador emulator-5554 -Testar com o emulador aberto para compilar, instalar sobre o aplicativo existente e executar os testes. O script verifica a compatibilidade antes de escrever e cria backup, sem limpar dados.

Validação: assembleDebug, assembleDebugAndroidTest, compileReleaseKotlin e testes do instalador (backup, reaplicação, verificação sem escrita, rejeição atômica de edição desconhecida, preservação de arquivos privados e UTF-8/BOM/CRLF). Os testes instrumentados são compilados na nuvem; sua execução e a aprovação visual dependem do emulador local.
