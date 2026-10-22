# Diagnóstico de importação · 1.11 / 13

Instrumentação baseada no `java (2).zip` enviado em 22/10/2026. Não altera os lotes, a ordem ociosa, a prioridade por acesso, imagens ou a versão do projeto. Não envia dados a nenhum servidor de diagnóstico.

## Aplicar e compilar

Feche o Android Studio se houver bloqueio de R.jar. No PowerShell:

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/diagnostico-importacao-1.11-13-20261022'
$installer=Join-Path $env:TEMP 'Preparar-Diagnostico-Importacao.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Diagnostico-Importacao.ps1" -OutFile $installer
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $installer -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O instalador verifica o hash do ZIP, confere os arquivos do projeto contra o código enviado, faz backup em `.diagnostic-backups` e aplica cinco arquivos. Se houver mudanças posteriores, interrompe antes de escrever. Não apaga cache ou dados. `-Testar` compila assembleDebug, mas não instala automaticamente.

Instale a compilação no emulador preservando os dados:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium\app\build\outputs\apk\debug\app-debug.apk'
```

## Coletar

```powershell
$collector=Join-Path $env:TEMP 'Coletar-Importacao.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Coletar-Importacao.ps1" -OutFile $collector
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $collector -Emulador emulator-5554
```

O coletor mostra o caminho de um TXT na Área de Trabalho. Deixe aberto, faça o teste e pressione Ctrl+C. Para TV física conectada ao ADB, substitua emulator-5554 pelo identificador retornado por adb devices. Não há acesso remoto automático à TV.

1. Com o coletor aberto, envie uma nova lista pelo site e selecione-a no aplicativo. Use uma lista autorizada que você já utiliza.
2. Abra Filmes e espere terminar; faça o mesmo com Séries e Canais.
3. Volte à Home e entre novamente em cada seção. Registre se abriu direto ou exibiu espera, e em qual seção. Não limpe os dados.
4. Deixe na Home sem interação para observar o carregamento ocioso. Navegar pode gerar CANCELLED: é esperado pela regra aprovada.
5. Envie o TXT. Se testar M3U nativa e Xtream, informe qual teste corresponde a cada formato; não envie as credenciais.

Um cache reutilizado sem nova importação não gera resumo de importação. A ausência de linhas, sozinha, não prova cache: primeiro confirme que a instalação está atualizada e que uma importação nova produz registros. Os resumos aparecem ao concluir, falhar ou cancelar uma etapa, não continuamente durante a espera.

## Interpretar os números

Cada resumo contém run (contador local ao processo), kind (índice Xtream, Filmes, Séries, Canais ou M3U), outcome e totalMs. O coletor aceita apenas esse esquema fechado e numérico do tag ImportTiming. Não coleta mensagens de erro, URLs, títulos, IDs de listas, usuário, senha ou corpos de resposta. Não exporta os outros logs já existentes no projeto.

- HEADERSMs: tempo até obter os cabeçalhos HTTP, incluindo conexão, redirecionamentos e espera do servidor. Não é uma medição exata do primeiro byte do corpo.
- READMs: tempo gasto nas leituras do corpo, incluindo espera de rede, buffering e descompactação. No Xtream mede leituras do stream descompactado; no M3U mede readLine. Não representa uma medição isolada de banda de rede.
- PARSEMs: conversão dos lotes JSON Xtream em modelos, incluindo espera pelo dispatcher. Não inclui todo o enquadramento dos registros.
- MAP_AND_WRITEMs: mapeamento e gravação do lote no sink Xtream; no M3U, chamada de gravação no Room.
- MERGEMs: transação que incorpora a seção Xtream ao catálogo. Inclui trabalho de merge e possíveis esperas do banco.
- READYMs: marcação/ativação inicial do índice ou marcação pronta M3U. A ativação de uma atualização Xtream completa ocorre depois das três seções e não está abrangida pelo resumo de uma seção.
- Calls: quantidade de medições acumuladas, não necessariamente quantidade de registros ou requisições. Para READ Xtream, conta streams; para M3U, chamadas readLine.

O tempo restante inclui enquadramento, classificação, preparação, limpeza e outras esperas. Para M3U, leitura e gravação ficam separadas, mas interpretação/classificação permanecem no restante; não chamar esse restante de parse puro. Não somar etapas como se fossem cronômetros independentes em todos os fluxos.

Os números medem a importação e a publicação no catálogo, não o desenho da primeira capa, o download de imagens ou o instante em que a tela fica visualmente pronta. A instrumentação não transforma cache em importação e não muda os pontos de abertura de tela.

## Verificação

Consulte `verificacao.txt`. Compilação em projeto Android isolado com os fontes atualizados; a execução real no emulador/TV ainda depende deste teste. O projeto enviado contém apenas Java/Kotlin; recursos e configuração de build vêm da estrutura Android disponível para verificação, não de um novo projeto completo enviado.
