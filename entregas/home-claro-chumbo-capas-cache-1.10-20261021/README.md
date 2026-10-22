# IPTV Premium 1.10 — Home clara/chumbo e capas em cache

As partes pretas dos cinco ícones clássicos da Home viram branco suave no modo Claro e cinza chumbo no modo Escuro. Detalhes mantêm a cor atual de Dourado, Azul, Rosa, Roxo ou Vermelho. O mesmo tratamento vai para os reflexos, sem gerar imagens adicionais. Larguras, posições, nomes nativos e crescimento da seleção continuam os mesmos. O visual interno do Kids não muda.

Ao abrir a preparação de um filme, o escolhido continua em primeiro lugar e tem prioridade. As capas secundárias são selecionadas entre os itens já presentes no Paging: primeiro as prontas na RAM, depois as já salvas no disco. Nunca são baixadas só para preencher o carrossel. A consulta de existência de arquivo usa no máximo 125 URLs conhecidas, sem varrer toda a biblioteca, e a apresentação fica limitada a 20 capas. Se não houver capa em cache, não se inventa uma imagem: o filme escolhido continua sua preparação normal. O intervalo mínimo existente de preparação permanece.

Corrigido o bloqueio que impedia as capas secundárias de serem lidas do disco enquanto a prioridade da mídia selecionada estava ativa. A Home também reutiliza imediatamente suas camadas da RAM quando disponíveis; durante uma decodificação fria mostra uma preparação no visual do modo atual, em vez do menu simples cinza.

Os quatro logs enviados pelo usuário terminam com compilação e testes aprovados. Os avisos eram de EMERALD/CYAN nos testes locais antigos. Esses nomes permanecem como aliases suportados de GOLD, sem recolocar duas cores no menu e sem modificar os testes privados do usuário. A comparação de compilação estrita antes/depois confirma a remoção desses avisos.

Versão 1.10, versionCode mínimo 12 (códigos maiores locais respeitados). local.properties não é alterado. Não há imagens no pacote e nenhuma imagem excluída será restaurada. Aplicar sobre a Home clássica 1.09 anterior.

Execute Preparar-Home-Cache-1.10.ps1 na raiz do projeto. Com o emulador aberto, use -Emulador emulator-5554 -Testar. O instalador verifica arquivos conhecidos antes de escrever e cria backup. Instala sobre o aplicativo atual, sem limpar dados.

Validação: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin; sete testes JVM da política real de seleção por cache; compilação estrita dos aliases antigos antes/depois; verificação do instalador em LF e UTF-8/BOM/CRLF, preservando arquivos privados, imagens e testes Kids locais. -Testar executa a suíte selecionada de 104 testes no emulador. A nuvem compila a suíte mas não a executa por não ter emulador.

previa-home-claro-chumbo.png é uma composição de referência em Dourado, não captura do Android. Aparência, foco e carregamento precisam da conferência no aparelho.
