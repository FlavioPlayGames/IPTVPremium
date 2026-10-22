# Correção dos ícones originais — versão 1.10

Mantém os cinco desenhos originais e a cor metálica de cada tema. A máscara altera somente superfícies neutras: branco suave no Claro e cinza chumbo no Escuro. Transparência, tamanho, posição, reflexos e legendas são preservados. Nenhuma imagem é restaurada nem substituída.

Inclui cumulativamente as correções da 1.10 para capas em cache e compatibilidade de referências dos temas. Não altera recursos Kids.

Execute o instalador na raiz do projeto com `-Emulador emulator-5554 -Testar`. O emulador deve estar ligado. Faz backup dos arquivos conhecidos e recusa alterações locais desconhecidas antes de gravar. Instala com `adb install -r`, preservando dados. `-Verificar` verifica sem alterar; `-SomenteAplicar` aplica sem compilar.

Validação: compilação debug/AndroidTest/release Kotlin aprovada; cinco testes JVM da máscara aprovados; instalador verificado em bases 1.09, 1.10 e arquivos Windows BOM/CRLF. Os 109 testes de instrumentação serão executados pelo instalador no emulador; não foram executados na nuvem.
