# IPTV Premium - Minhas Listas (17/10/2026)

Entrega aprovada: lixeira ao lado de cada card, fora do contorno; foco por setas (direita para lixeira, esquerda para lista); confirmacao antes de excluir; cores derivadas de ThemeColors para os sete temas.

## Instalacao
O arquivo `Preparar-Minhas-Listas.ps1` e autossuficiente: incorpora um ZIP com `Aplicar.ps1`, `manifest.json`, relatorio e o `PlaylistListScreen.kt` corrigido.

Ao executar no computador do usuario, verifica hashes SHA-256, nao sobrescreve versoes desconhecidas, faz backup em `.iptv-backups`, modifica apenas `app/src/main/java/br/com/flaviotorres/iptvpremium/ui/playlist/PlaylistListScreen.kt` e tenta `gradlew.bat assembleDebug`. Em caso de falha de aplicacao ou compilacao, tenta restaurar o arquivo original.

## Validacao
- Conferido hash SHA-256 do ZIP incorporado e do Kotlin corrigido.
- Verificado que o arquivo anterior aceito corresponde ao codigo entregue no `java.zip`.
- Nao compilado neste ambiente (Android SDK e PowerShell 5.1 indisponiveis).
- Necessario validar build e navegacao na TV com o controle remoto.

Nao atualiza site, Cloudflare, banco, chaves, APK publicado, versionCode ou versionName.