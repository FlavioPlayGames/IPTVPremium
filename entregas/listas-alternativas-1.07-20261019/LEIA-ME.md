# Xtream primeiro, M3U alternativa e diagnóstico dos dois formatos — 1.07

Esta entrega mantém os dois objetivos: enviar as duas configurações na mesma lista e medir por que ambos os importadores demoram. Não altera a velocidade aprovada da rolagem, os 3 segundos obrigatórios dos filmes nem o orçamento de memória.

## O que já sabemos do diagnóstico enviado

| Formato | Importação observada | Parcela observada de gravação |
|---|---:|---:|
| M3U | 93,3 segundos | 75,5 segundos em chamadas ao Room, aproximadamente 81% |
| Xtream | 18,5–23,6 segundos nas 8 importações bem-sucedidas | Em uma execução de 18,6 segundos: 8,0 segundos nos filmes e 2,2 segundos nas séries |

A M3U transferiu 85,2 MB e processou 327.372 entradas, incluindo 282.008 episódios, em 1.307 lotes. Xtream baixa os catálogos e consulta episódios depois, quando necessário. Os volumes são diferentes: comparar somente o total não mede a eficiência de cada importador.

Gravação é tempo dentro das chamadas ao banco: inclui preparação de argumentos, espera pelo executor/SQLite, transação e confirmação. Não prova que todo esse tempo seja escrita física no armazenamento. Já existem transações por lote; não faria sentido propor adicioná-las novamente. O restante de cada endpoint inclui leitura de rede e enquadramento/processamento do texto, não é download puro. O diagnóstico também contém tentativas Xtream com resposta truncada, seguidas de nova consulta, acrescentando alguns segundos.

O servidor enviou `identity`, sem Gzip, apesar do suporte do aplicativo. A central e a ativação do catálogo não dominaram a última importação M3U registrada. Esses dados vêm do diagnóstico enviado; ainda precisamos comparar na mesma TV. Não foi prometido atingir 15–20 segundos nem reduzir o tempo de gravação com esta entrega.

## Regra de envio e importação

- Um link M3U reconhecível como Xtream envia servidor/usuário/senha e `fallbackM3uUrl`, na mesma lista e com o mesmo ID. Xtream direto também recebe uma URL get.php alternativa com credenciais corretamente codificadas. M3U comum continua M3U.
- O aplicativo termina a importação Xtream antes de decidir. Sucesso ignora completamente o download M3U. Falha de provedor/rede/formato limpa a geração parcial e tenta a M3U fornecida. Não depende de um novo comando do site.
- Cancelamento, erro fatal ou falha local de armazenamento não iniciam outro download. Se ambas falharem, o catálogo ACTIVE anterior é preservado. A alternativa só fica pronta em uma nova geração, ativada pela regra existente. Não cria lista duplicada.
- O link alternativo acompanha comandos encriptados, snapshots da central e armazenamento local do aplicativo. Clientes/payloads antigos sem esse campo continuam compatíveis, mas não ganham fallback retroativamente. Após atualizar, reenvie/edite a lista existente para receber a alternativa.
- A consulta inconclusiva no site continua pendente de confirmação no aparelho; detalhes técnicos permanecem restritos ao administrador. A validação do site não garante que uma importação inteira terminará. O provedor pode bloquear a rede do site e aceitar a TV.

## Aplicar primeiro no aplicativo

PowerShell, na raiz do projeto. O instalador confere todos os arquivos antes de alterar, salva backup, compila e instala por cima, sem limpar dados. Requer a revisão anterior de abertura coordenada; arquivos diferentes interrompem a aplicação sem sobrescrevê-los. Não mude hashes para contornar incompatibilidade.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/listas-alternativas-1.07-20261019'
$arquivo = Join-Path $env:TEMP 'Preparar-Listas-Alternativas-1.07.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Preparar-Listas-Alternativas-1.07.ps1" -OutFile $arquivo
if ((Get-FileHash $arquivo -Algorithm SHA256).Hash -ne '6660e77a5e9c9e059646483a705a32c061faa5089f0f5949ddd7ae8ab9389d6a') { throw 'Download diferente do esperado.' }
& $arquivo -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador 'emulator-5554' -TestarImportacao
```

Esperado no aparelho: `OK (16 tests)`. Não publique os APKs se os testes falharem; envie `diagnostico-listas-alternativas.txt`. O parâmetro Emulador também aceita o serial ADB de uma TV conectada. `-Verificar` apenas verifica compatibilidade; `-SomenteAplicar` apenas aplica o código com backup.

## Publicar o site depois do aplicativo

Usa seu login Wrangler local, Node.js 22+, sem migração de banco. Não foi publicado ao vivo nesta nuvem. Para apenas extrair/revisar, acrescente `-NaoPublicar`.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/listas-alternativas-1.07-20261019'
$arquivo = Join-Path $env:TEMP 'Publicar-Listas-Alternativas-1.07.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Publicar-Listas-Alternativas-1.07.ps1" -OutFile $arquivo
if ((Get-FileHash $arquivo -Algorithm SHA256).Hash -ne '919133ae7a8651ca536d159f46e1bb14c0d4f2957224134d88ff9f87a2088fb4') { throw 'Download diferente do esperado.' }
& $arquivo
```

Recarregue o site, edite a lista existente, valide e envie novamente. O envio carrega a alternativa, não modifica outras listas automaticamente.

## Medir M3U e Xtream na mesma TV

O medidor funciona também na versão anterior que já emite as métricas básicas. O app desta entrega acrescenta contagem de lotes e maior duração de gravação de um lote nos endpoints Xtream e pico de gravação na M3U. Não muda o tamanho dos lotes sem antes medir no aparelho.

1. Use a mesma TV, rede e lista. Feche outros testes de importação; aguarde uma importação terminar antes de começar a seguinte. Não limpe dados entre elas.
2. Rode o medidor **antes** de enviar/trocar/atualizar a lista. Ele não importa nem converte nada sozinho. Em seguida provoque UMA importação no app/site.
3. `-Formato` é apenas o rótulo do arquivo: não força o aplicativo a usar um protocolo. Confira `tipo/formato` no resultado; um link M3U convertido pelo site poderá importar em Xtream. Para medir M3U nativa, use o cadastro M3U do app. Para Xtream, use o cadastro Xtream. Preserve o nome/ID das listas existentes ao editar; não é necessário apagar o catálogo.
4. Repita para o outro formato. Envie os dois arquivos gerados, informando TV ou emulador. O script encerra a captura ao receber `SYNC_RESUMO`; se atingir 10 minutos ou for interrompido, salva o que capturou. O prazo de captura não altera os timeouts da importação.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/listas-alternativas-1.07-20261019'
$medidor = Join-Path $env:TEMP 'Medir-Importacao-1.07.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Medir-Importacao-1.07.ps1" -OutFile $medidor
if ((Get-FileHash $medidor -Algorithm SHA256).Hash -ne '4df8cb9772e44a74c7f00b1f0d1f449bfef0592f0b60a6ef0dd74657774e5a8d') { throw 'Download diferente do esperado.' }
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
$dispositivo = 'emulator-5554' # substitua pelo serial da TV para medir a TV
& $medidor -Dispositivo $dispositivo -Formato XTREAM -PastaSaida 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
# Depois de terminar a primeira medicao, execute a segunda:
& $medidor -Dispositivo $dispositivo -Formato M3U -PastaSaida 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
```

Arquivos: `diagnostico-importacao-xtream.txt` e `diagnostico-importacao-m3u.txt`. Cada execução substitui apenas o arquivo do seu rótulo. Não limpa logcat, dados/cache nem encerra o aplicativo. Interrompe somente a captura ADB que criou. O relatório reconstrói uma lista permitida de métricas, sem copiar URLs, credenciais, corpos ou mensagens arbitrárias de exceção. Heap Java não é memória total do aplicativo.

A análise seguinte comparará quantidade/volume de dados, duração por endpoint, repetições, gravação total, média por lote e maior lote. Uma melhoria de lotes deve ser validada com o banco real e com a TV; aumentar concorrência ou RAM sem essa medida não garante ganho.

## Validação desta entrega

- App e APK de testes Android compilados.
- 12 testes JVM passaram: sequência de alternativas, limpeza antes do fallback, ausência de fallback após sucesso/cancelamento/erro fatal, bloqueio por falha local, preservação dos erros, codec e persistência em DataStore real. A adaptação JVM troca somente o diretório de cache Android pelo temporário local.
- Quatro testes adicionais de integração com Room compilados para Android: sucesso sem M3U, falha Xtream parcial seguida de M3U, ambas falhando com catálogo antigo preservado e cancelamento/falha de escrita sem novo download. **Não foram executados nesta nuvem, que não tem emulador/TV.** O instalador executa os 16 no aparelho.
- 45 testes de servidor, 12 verificações do navegador e Wrangler dry-run passaram. A fila criptografada real do Worker foi exercitada com banco simulado e payload fictício; o link alternativo foi preservado em um único comando. Nenhuma credencial real/provedor real foi usado.
- Instaladores verificados/aplicados em cópia, hashes conferidos, publicador extraído sem deploy; medidor validado com ADB simulado incluindo exclusão do evento antigo e remoção de dados não permitidos.
