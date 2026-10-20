# IPTVPremium 1.8 — Xtream 250 / M3U 2.000

Atende à versão solicitada **1.8** (exatamente esse nome), com **versionCode 10**, superior ao código 9 da versão anterior. Após o pedido de voltar o Xtream para 250 na TV, o padrão foi ajustado para **Xtream até 250 e M3U até 2.000**. O perfil de 500 não é o padrão desta versão.

A configuração passa a valer também em release, sem depender de um comando de teste. Journal e synchronous permanecem no modo automático/padrão do Android; nenhum ajuste extra de WAL/NORMAL é forçado. O gravador Xtream mantém o limite interno anterior de 500, mas recebe blocos de até 250 do produtor, como na configuração original solicitada. Recuperações segmentadas mantêm o comportamento anterior. A M3U usa até 2.000 por coleção/lote, com limiar de texto de 2 Mi caracteres. Isso não equivale a um teto de RAM; 350 MB totais não foram declarados medidos nesta entrega.

Em debug, a preferência de diagnóstico usa um novo namespace privado da versão 1.8. Preferências antigas de 500/650/1.000 não são reaproveitadas automaticamente; o valor ausente/inválido é PADRAO (250/2.000). Release ignora qualquer preferência experimental e sempre usa PADRAO. Os perfis anteriores continuam disponíveis apenas para diagnóstico no debug, caso solicitados depois.

## Instalar no emulador

Com emulator-5554 aberto, cole no PowerShell:

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/versao-1.8-20261020'
$arquivo = Join-Path $env:TEMP 'Preparar-Versao-1.8.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Preparar-Versao-1.8.ps1" -OutFile $arquivo
if ((Get-FileHash $arquivo -Algorithm SHA256).Hash -ne '725affabc69c72c34819621ac881ef32419f2d1064ec1746bf04c8b35897b6cc') { throw 'Download diferente do esperado.' }
& $arquivo -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador 'emulator-5554' -TestarBanco
```

O instalador valida todos os hashes antes de alterar, cria backup, atualiza fontes e somente as duas propriedades locais de versão, compila e instala por cima. Não limpa dados/cache nem desinstala o aplicativo. Não envie listas durante os testes automáticos. O teste usa bancos temporários e dados fictícios; não modifica seu catálogo real.

Resultado esperado: **OK (27 tests)**. Envie **diagnostico-versao-1.8-testes.txt**, salvo na raiz do projeto, se houver falha. Não publique APKs que falhem na validação. Se passarem, o script confirma explicitamente PADRAO no emulador usando o APK de testes e deixa o aplicativo fechado. Abra-o normalmente; deve exibir versão **1.8**. Não inicia uma medição de importação nem exige novo envio da lista.

A instalação no emulador usa o APK compatível com sua ABI; também aceita o caso de apenas um APK gerado pelo Android Studio. Antes de instalar, confere applicationId, versionName=1.8 e versionCode=10. O app mantém o mesmo ID. Os APKs devem continuar sendo gerados/assinados com a configuração local já usada; não foi distribuída outra chave nem solicitado desinstalar.

`-Verificar` verifica compatibilidade sem gravar. `-SomenteAplicar` aplica fontes e propriedades com backup, sem compilar/instalar. `-TestarBanco` exige um serial ADB. Os arquivos da entrega anterior de 650 são aceitos; revisões desconhecidas interrompem sem sobrescrever alterações locais. Backup em `.navigation-backups\versao-1.8-*`, incluindo uma cópia privada de local.properties quando existente. Esse backup pode conter suas configurações locais: não é incluído nesta entrega pública.

## Propriedades de versão

Os defaults do Gradle são 10 e 1.8. Como o projeto também aceita APP_VERSION_CODE e APP_VERSION_NAME em local.properties, o instalador migra essas duas chaves para 10/1.8; caso contrário, uma propriedade antiga poderia continuar gerando 1.07 apesar de alterar o default. Mantém SDK, chaves e demais propriedades locais. Os valores privados nunca são incluídos no pacote ou impressos pelo instalador.

Se encontrar código local superior a 10, interrompe antes de modificar qualquer arquivo para não forçar downgrade. Confira a revisão do projeto nesse caso. Nenhuma versão assinada de produção foi publicada ao vivo nem enviada à central automaticamente. Depois da validação no emulador, os APKs para distribuição devem ser gerados e assinados pelo seu processo local habitual.

## O que permanece

Mesmo esquema de banco e migrações; não exige reimportar um catálogo já válido apenas por mudar a versão. Histórico, favoritos, licença, listas e cache existentes são preservados. Não muda o site, conversão M3U/Xtream, fallback, rolagem aprovada, temas, player, caches de imagens ou concorrência. Não afirma medir desempenho na TV: os testes do usuário permanecem no emulador, como combinado. O retorno do Xtream a 250 atende ao pedido do usuário, sem exigir conexão ADB com a TV.

## Validação

- App debug e APK de testes Android compilados. A tarefa compileReleaseKotlin também passou, com BuildConfig de release em DEBUG=false, versão 1.8 e código 10; não foi gerado/publicado um APK de produção assinado nesta nuvem.
- Os cinco APKs debug (universal, armeabi-v7a, arm64-v8a, x86 e x86_64) tiveram metadados conferidos: **1.8 / código 10**, applicationId preservado.
- **9 testes JUnit JVM e 7 cenários de framing/compatibilidade JSON passaram.** Incluem default 250/2.000, release ignorando os experimentos e limites/cancelamento dos lotes.
- **27 testes compilados para Android**. A execução real desta revisão fica para o emulador do usuário; não foi afirmado que passaram nesta nuvem.
- Instalador aplicado/verificado em cópia da revisão anterior. Hashes conferidos; atualização de versão local, preservação das outras propriedades e backup exato testados com dados fictícios; código superior a 10 bloqueado sem alteração.
- Controlador PADRAO testado com ADB simulado, confirmando configuração e fechamento sem iniciar captura/limpar dados. O fluxo real de instalação Windows permanece para o computador do usuário.
