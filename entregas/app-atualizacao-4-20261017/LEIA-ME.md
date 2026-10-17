# Aplicativo com atualização por arquitetura — versão 1.02 / código 4

Executar Preparar-app-atualizacao-4.ps1 na raiz Android, ou informar -Projeto.
O pacote verifica os hashes de AppUpdateManager.kt e AppUpdatePolicy.kt, aplica o ajuste de arquitetura, prepara a versão e compila assembleDebug.
São os mesmos dois arquivos Kotlin já testados para o painel por arquitetura. Temas, player, catálogo, assinaturas e Gradle não são modificados.

Se APP_VERSION_CODE for menor que 4, prepara código 4 e nome 1.02.
Se já existir uma versão maior ou igual a 4, preserva seu código e nome; a execução repetida não aumenta a versão automaticamente.
Somente APP_VERSION_CODE e APP_VERSION_NAME são ajustados em local.properties, conservando os demais bytes.
O original desse arquivo é salvo em .iptv-backups/versao-app-... e nunca é enviado ao GitHub.
A assinatura é a configuração local existente. Nenhum APK compilado na nuvem é distribuído.

Gerar juntos e publicar: app-universal-debug.apk, app-armeabi-v7a-debug.apk, app-arm64-v8a-debug.apk.
As saídas x86 e x86_64 podem ser omitidas do painel.
Antes da publicação, verificar instalação sobre a versão existente no aparelho.
Clientes antigos recebem o APK universal. O aplicativo novo escolhe pelo Android SUPPORTED_ABIS nas próximas atualizações.
Todos os arquivos da publicação devem ter o mesmo código, nome de versão e assinatura.
