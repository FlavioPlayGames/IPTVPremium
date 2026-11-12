# Testes

`node testes/worker-check.mjs` testa o Worker completo com armazenamento R2 e autenticação simulados. Não conecta à sua conta, não usa seus segredos e não substitui teste no Cloudflare real.

`ManualSubtitleArchiveCheck.kt` executa testes dos fontes reais de leitura SRT/ZIP e de destino. Requer Kotlin/JVM, org.json e um stub da estrutura ExternalSubtitleCue, pois essa estrutura normalmente pertence ao player Android. O relatório inclui a execução realizada na entrega.

Também foram executados testes de interface web com JSDOM 26, compilação Android isolada e testes do instalador PowerShell com pastas temporárias. Nenhum teste aqui instala ou apaga o aplicativo da sua TV.
