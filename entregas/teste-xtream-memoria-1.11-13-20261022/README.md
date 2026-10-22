# Teste Xtream em memória · 1.11 / 13

Experimento baseado no `java (2).zip` e no diagnóstico anterior. Objetivo: comparar com o modelo observado no IPTV Plus, com foco no Xtream. Não é uma implementação idêntica ao JavaScript deles nem uma promessa de ganho antes de medir na TV.

## O que foi adaptado

- Após autenticar, consulta os três índices de categorias em paralelo; aplica os resultados ao sink em ordem, sem gravações concorrentes nele.
- Catálogo em RAM: Room/SQLite em memória mantém as consultas e Paging usados pelas telas, em vez de gravar o catálogo em disco.
- Cada seção Xtream é recebida em lotes mantidos em memória. Depois de validar a resposta inteira e a fonte atual, publica diretamente em uma transação. Remove a geração intermediária e a cópia de merge para esse caminho experimental.
- Na Home ociosa, inicia os downloads restantes de Filmes, Séries e Canais sem aguardar a conclusão da seção anterior. Há no máximo três carregadores. As transações de publicação continuam serializadas para proteger o catálogo.
- Ao navegar, a rotina existente cancela o trabalho ocioso. A seção acessada tem prioridade; detalhes/reprodução suspendem a importação pelo mecanismo existente.
- Processamento de JSON continua em Dispatchers.Default, separado da interface; HTTP continua em IO. Mantém fluxo assíncrono, cancelamento, gzip, classificação, formato Xtream e reutilização das seções concluídas durante a mesma execução.
- Os resumos ImportTiming continuam disponíveis para comparar com o teste anterior.

Esta entrega muda a regra ociosa **somente na compilação debug de teste**: downloads podem acontecer juntos, em vez de uma seção por vez. Release mantém o caminho anterior em disco e o agendamento serial.

## Limites importantes do experimento

Usamos Room em memória para preservar a integração das telas existentes. Ainda existem índices e operações SQL, diferentemente dos objetos JavaScript do outro app. A compatibilidade é preservada no código; busca, favoritos, histórico, categorias, ordenação e reprodução ainda precisam ser exercitados no dispositivo. Não há validação visual ou de reprodução feita nesta máquina.

Ao encerrar o processo, o catálogo experimental desaparece e precisará ser importado novamente. Sair de uma seção e entrar durante a mesma execução reutiliza a seção concluída. Downloads interrompidos podem recomeçar. As listas cadastradas e as configurações do app continuam no armazenamento existente; o arquivo `playlist_cache.db` anterior não é aberto pelo caminho debug experimental, nem apagado. O progresso do experimento usa uma pasta separada.

Segurar os lotes e o banco em RAM aumenta o pico de memória. Testar na TV é necessário; velocidade no emulador não comprova o resultado na TV. Este experimento altera armazenamento, publicação e sobreposição ao mesmo tempo: se melhorar, uma etapa posterior isola a contribuição de cada mudança.

**Teste somente Xtream, inclusive lista convertida pelo site.** Não há adaptação M3U nesta entrega. Como o banco de catálogo é compartilhado pelas seções e tipos de lista, o backend em RAM da compilação debug também estaria presente se uma M3U fosse selecionada; não usar isso como teste de importação M3U. O processamento remoto M3U observado no outro app exigiria serviço próprio e ficou fora do escopo. Nenhuma lista é enviada à API do IPTV Plus.

## Instalar o teste

O instalador aceita os fontes do ZIP enviado ou do diagnóstico anterior, verifica os hashes e faz backup. Se você alterou os mesmos arquivos depois, ele interrompe antes de escrever.

Feche o Android Studio se houver bloqueio de R.jar, mantendo o emulador disponível. Execute no PowerShell:

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/teste-xtream-memoria-1.11-13-20261022'
$script=Join-Path $env:TEMP 'Preparar-Teste-Xtream-Memoria.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Teste-Xtream-Memoria.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium\app\build\outputs\apk\debug\app-debug.apk'
```

O script compila debug; não instala automaticamente. Use adb install -r para preservar os dados. O pacote não altera build.gradle ou versionCode, nem inclui APK, imagens ou credenciais.

## Comparar

Abra o coletor já utilizado, ou baixe a cópia incluída:

```powershell
$collector=Join-Path $env:TEMP 'Coletar-Importacao.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Coletar-Importacao.ps1" -OutFile $collector
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $collector -Emulador emulator-5554
```

1. Use a mesma lista Xtream da medição anterior. Se o aplicativo não iniciar a importação sozinho com o catálogo em RAM vazio, selecione a lista e peça atualização. Não apague dados.
2. Primeiro teste abrir Filmes e aguardar concluir, depois Séries e Canais. Anote também o tempo percebido até conseguir navegar.
3. Volte e reabra as seções já concluídas. Verifique busca, categorias, ordenação, favorito, detalhes e reprodução.
4. Em outra execução do processo, observe a Home sem interação para testar o carregamento simultâneo. Não force a parada enquanto uma importação ainda estiver sendo medida.
5. Envie o TXT e informe se o teste foi na TV ou no emulador. Ctrl+C encerra o coletor.

No novo fluxo, MAP_AND_WRITE inclui principalmente guardar os lotes na memória; MERGE passa a incluir a gravação direta em RAM e a publicação. Compare **totalMs**, não apenas a queda de uma coluna. Nas seções simultâneas, não some os tempos individuais como se fossem tempo total de parede. No índice, HEADERSMs também soma medições sobrepostas e pode superar totalMs.

## Voltar ao padrão

```powershell
$restore=Join-Path $env:TEMP 'Voltar-Xtream-Padrao.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Voltar-Xtream-Padrao.ps1" -OutFile $restore
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $restore -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium\app\build\outputs\apk\debug\app-debug.apk'
```

Isso restaura o caminho em disco e a ordem ociosa serial do diagnóstico anterior, preservando o banco anterior. O pequeno arquivo MemoryCatalogBuffer pode permanecer sem uso; não afeta o modo padrão. Backups por aplicação ficam em `.xtream-test-backups`.

Consulte `verificacao.txt` para os checks executados e seus limites.
