# Teste de importação completa · 1.11 / 13

Desliga a importação por acesso e a fila de 7 categorias, restaurando a sincronização completa existente no aplicativo. Mantém o formato cadastrado: Xtream continua Xtream, inclusive quando convertido pelo site. M3U continua usando o importador M3U nativo.

## O que muda

- Não aguarda abrir Filmes, Séries ou Canais para decidir quais seções baixar.
- Uma nova importação/atualização baixa todo o catálogo antes de concluir. Xtream usa os endpoints completos; se um endpoint apresentar o problema de JSON já tratado no projeto, continua disponível a recuperação original por categoria. Não existe mais a fila personalizada de 7 nem o agendamento ocioso por acesso.
- Os lotes técnicos de streaming/gravação continuam com padrão **250 no Xtream / 2.000 no M3U**; esses lotes não são limites de quantidade de títulos nem a regra de 7 categorias. Perfis de diagnóstico debug eventualmente escolhidos pelo usuário continuam disponíveis.
- O catálogo anterior é mantido durante a atualização. Só passa para a nova geração quando ela está pronta. Falhas não acionam uma conversão silenciosa de Xtream para M3U.
- Desliga também a retomada automática dos checkpoints parciais do teste anterior, evitando que um segundo importador continue em paralelo.
- Reabrir uma lista completa reutiliza seu catálogo salvo. O teste não força uma nova importação a cada entrada.

Esta entrega altera **somente dois arquivos de produção** (AppNavigation e M3uSourceSynchronizer), além de incluir três classes de testes de regressão já existentes. Não modifica imagens, listas, histórico, cache, recursos Kids ou `app/build.gradle.kts`; portanto preserva **versão 1.11 / versionCode 13** já configurados no seu projeto.

## Instalar e comparar

Feche o Android Studio para evitar o bloqueio do R.jar. Mantenha o emulador aberto. Execute no PowerShell:

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script = Join-Path $env:TEMP 'Preparar-Importacao-Completa-1.11.ps1'
Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/teste-importacao-completa-1.11-13-20261022/Preparar-Importacao-Completa-1.11.ps1' -OutFile $script
if ((Get-FileHash $script -Algorithm SHA256).Hash -ne '0b90159fec97d3ff216a7daf80c3988ddf839ae5458f537550f87050707e6e6b') { throw 'Arquivo diferente do esperado.' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador emulator-5554 -Testar
```

**Depois de instalar, abra o aplicativo e use Atualizar lista uma vez.** Isso substitui o catálogo parcialmente carregado pelo teste anterior por uma importação completa. Sem essa atualização, o app continua reutilizando o catálogo ativo já salvo.

Compare o tempo da atualização completa e o tempo de entrar em cada seção depois dela, usando a mesma lista. O fluxo completo pode demorar mais para liberar a primeira abertura; este pacote serve justamente para comparar o comportamento real na TV. Não foi medida uma redução de tempo nesta sessão.

O instalador valida todos os arquivos antes de alterar qualquer um e cria backup exato em `.navigation-backups/importacao-completa-111-13-*`. A instalação usa `adb install -r`; não apaga dados. Se detectar outra alteração feita após o último pacote, interrompe sem sobrescrever. Para reverter este teste, restaure os dois arquivos de produção do backup indicado pelo instalador.

`-Verificar`: confere sem alterar. `-SomenteAplicar`: aplica sem compilar. `-Testar -Emulador emulator-5554`: compila, instala e roda as 18 verificações de lotes/perfis/autenticação incluídas.

Diagnósticos na raiz do projeto: `diagnostico-importacao-completa-111-13-compilacao.txt` e `diagnostico-importacao-completa-111-13-testes.txt`.

## Verificação

- Compilação debug e Kotlin release verificadas; APK de testes compilado para execução local.
- 18 testes de lotes/perfis/autenticação executados na JVM com sucesso.
- 12 verificações do transporte real com respostas de teste: incluem os três endpoints completos, 501 filmes sem perda, lotes 250/250/1 e ausência de fallback M3U em falha Xtream.
- Emulador/TV não disponíveis nesta sessão cloud. Os testes nativos devem ser executados pelo comando acima.
- A cópia cloud usa aliases de compilação para imagens não presentes no java.zip; esses aliases e os APKs não são entregues. A aparência local não foi alterada.
