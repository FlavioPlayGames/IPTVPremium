# Aviso antes da primeira abertura · 1.11 / 13

A mensagem aparece **sobre a Home, durante a espera, antes de abrir a seção**, em vez de ficar dentro de uma tela que só apareceria depois de carregar.

## Comportamento

- Ao clicar em **Filmes, Séries ou Canais** na Home, consulta primeiro o conteúdo salvo. Se já existir, abre direto.
- Quando a seção precisa da primeira importação Xtream, permanece na Home e mostra um painel discreto na parte inferior: indicador circular e, abaixo, **Carregando seu conteúdo…**, com reticências animadas. A cor acompanha o tema Claro/Escuro e a cor de metal selecionada.
- A seção só abre depois da importação e publicação do conteúdo. Uma resposta válida sem itens também termina a espera; uma falha encerra o aviso e permite tentar novamente.
- **Voltar** cancela essa abertura. Não apaga itens já concluídos. Trocar a lista também cancela a espera antiga.
- Um pequeno intervalo de 120 ms evita piscar o aviso quando o cache já está pronto. Depois de visível, os pontos mudam a cada 420 ms sem deslocar o texto.
- Preserva o fluxo de importação por acesso e ociosa **Filmes → Séries → Canais**, uma seção por vez e sem grupos de 7. A seção solicitada tem prioridade; navegar ou reproduzir interrompe o trabalho ocioso.

A alteração é para os três botões normais da Home. Não modifica o Kids, as imagens ou o player. Mantém os padrões 250/Xtream e 2.000/M3U e o formato cadastrado. M3U nativa conserva sua leitura integral do arquivo; a preparação independente por seção é do Xtream, inclusive convertido pelo site.

## Instalar

Compatível com os pacotes anteriores de importação por acesso, teste completo e ocioso sem sete. Esta entrega inclui as dependências necessárias para manter o fluxo ocioso corrigido.

Feche o Android Studio para evitar o bloqueio de R.jar; mantenha o emulador aberto. Execute no PowerShell:

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script = Join-Path $env:TEMP 'Preparar-Aviso-Primeira-Carga-1.11.ps1'
Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/aviso-primeira-carga-1.11-13-20261022/Preparar-Aviso-Primeira-Carga-1.11.ps1' -OutFile $script
if ((Get-FileHash $script -Algorithm SHA256).Hash -ne 'b329b1a0b5e02f04612e9c89be8b118b6fab385bdf5f17235c951fb75ec3e9e4') { throw 'Arquivo diferente do esperado.' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador emulator-5554 -Testar
```

Não limpe dados ou cache para testar. Use uma nova lista Xtream enviada pelo site e clique em uma seção ainda não importada. Uma seção já disponível deve abrir direto. Teste também o botão Voltar durante a primeira carga.

O instalador verifica os arquivos antes de escrever, faz backup em `.navigation-backups/aviso-primeira-carga-111-13-*` e instala com `adb install -r`. São cinco arquivos de produção (quatro existentes e um novo componente de aviso) e duas classes de testes. Preserva dados, imagens e `app/build.gradle.kts`, mantendo **1.11 / versionCode 13** configurados no projeto.

`-Verificar` só confere; `-SomenteAplicar` aplica sem compilar. `-Testar` compila e roda 21 testes no emulador informado (9 de política e 12 de integração). Diagnósticos: `diagnostico-aviso-primeira-carga-111-13-compilacao.txt` e `diagnostico-aviso-primeira-carga-111-13-testes.txt` na raiz do projeto.

## Verificação

Compilação debug, AndroidTest e Kotlin release verificada. Os testes Android incluem espera pelo conteúdo realmente publicado, sucesso com seção vazia e término da espera em falha, além das verificações anteriores de prioridade, cancelamento, retomada e atualização. Estão compilados; não foram executados em emulador nesta sessão cloud. A animação e o posicionamento ainda precisam de conferência visual na TV.

Não há nova dependência de Material móvel: o componente utiliza Compose/TV já presentes no projeto. Recursos substitutos usados para compilar em cloud não entram na entrega. Nenhum APK ou imagem é publicado.
