# Compatibilidade com testes antigos dos temas — 1.8

O teste local KidsAppearanceDeviceTest.kt ainda usa PremiumTheme.EMERALD. Esse arquivo não estava nos fontes recebidos; a remoção do enum quebrou a compilação dele.

A correção mantém EMERALD e CYAN como aliases de código depreciados para GOLD, sem acrescentar entradas ao enum ou opções ao menu. As cinco cores continuam Dourado, Azul, Rosa, Roxo e Vermelho. Os testes/registros antigos são compatíveis e o comportamento visual do Kids não foi alterado. O teste de contrato existente verifica os aliases e as cinco entradas reais, sem alterar a contagem de 88 testes da suíte selecionada.

O instalador aplica somente PremiumThemes.kt e StandardHomeArtworkDeviceTest.kt, com hashes e backup. Não altera nem apaga KidsAppearanceDeviceTest.kt ou outros testes locais. Mantém os ajustes visuais anteriores.

A captura do Gradle usa UTF-8 único e mantém a leitura do stderr até o fim, evitando a interrupção por NativeCommandError do PowerShell. Se a compilação falhar, diagnostico-marmore-compilacao.txt na raiz do projeto terá a saída completa. O código de saída continua sendo verificado; não ignora erros nem desativa testes.

Debug, pacote de testes Android e Kotlin release foram compilados aqui. Testes Android serão executados no emulador pelo parâmetro -Testar, não foram executados neste ambiente. O teste local específico do usuário não foi recebido nem executado aqui; foi reproduzida a referência de código antiga no contrato compilado.
