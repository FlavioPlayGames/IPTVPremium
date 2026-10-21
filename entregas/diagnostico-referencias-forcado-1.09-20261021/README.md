# Diagnóstico de referências — 1.09

Força :app:compileDebugKotlin com --rerun-tasks e --no-configuration-cache. Registra o resultado completo, código de saída, caminhos/pacotes/hashes dos arquivos relacionados aos avisos. O build anterior do usuário terminou com sucesso, mas a etapa Kotlin foi reutilizada como UP-TO-DATE; esta verificação coleta uma compilação nova.

Execute Diagnosticar-Referencias-1.09.ps1 na raiz IPTVPremium, onde fica gradlew.bat. Envie diagnostico-referencias-forcado-1.09.txt. Não requer emulador. O script não altera o código, não instala APK e não limpa dados do aplicativo. O Gradle atualiza seus próprios arquivos de compilação. -SomenteInventario coleta somente os caminhos/hashes/pacotes sem chamar o Gradle.

O script não lê local.properties nem divulga seu conteúdo. Testado com inventário, compilação simulada com sucesso e falha simulada em stderr, preservando o conteúdo dos arquivos existentes.
