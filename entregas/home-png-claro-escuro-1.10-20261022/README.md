# Home com PNGs próprios — versão 1.10

Corrige o preenchimento manchado da entrega anterior. Agora são dez PNGs prontos: cinco com interiores branco suave para Claro e cinco com interiores cinza chumbo para Escuro. O app não aplica mais a máscara de superfícies neutras. Os PNGs mantêm transparência e o metal usa a cor escolhida entre os cinco temas.

Mantém os espaços e dimensões aprovados dos ícones, legendas nativas, foco e reflexos no mármore. A área Kids interna não muda. Os PNGs foram editados a partir dos desenhos originais; não são os mesmos arquivos binários dos originais. A prévia é uma composição de referência com os recursos reais, não uma captura do Android.

Inclui as correções cumulativas da 1.10 para capas em cache e compatibilidade de temas. Remove somente as cinco imagens clássicas substituídas cujos hashes são conhecidos e a política de máscara obsoleta. Não restaura outras imagens excluídas manualmente. Faz backup antes de alterar e recusa arquivos de código desconhecidos.

Execute `Preparar-Home-PNG-1.10.ps1 -Projeto C:\Users\flavi\AndroidStudioProjects\IPTVPremium -Emulador emulator-5554 -Testar`. O emulador precisa estar aberto. O instalador baixa o pacote e verifica o SHA256, compila, instala com `adb install -r` e executa a suíte de 106 testes. Não apaga dados, listas, histórico ou cache do usuário. `-Verificar` não modifica nada; `-SomenteAplicar` não compila.

A compilação debug/AndroidTest e release Kotlin passou. O instalador foi testado em bases 1.09, nas duas entregas 1.10 e com BOM/CRLF do Windows. Instrumentação foi compilada, mas não executada na nuvem.
