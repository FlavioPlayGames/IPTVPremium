# Capas prontas e reutilização no carrossel

Atualização cumulativa para o ZIP restaurado **IPTVPremium (8)** e para as atualizações `capas-janela-prioridade` e `capas20-trailers-internos`. Não inclui upscale.

## Carregamento das capas

- Mantém o cache persistente existente, compartilhado entre catálogo e animação de abertura. Não apaga as imagens ao selecionar uma categoria ou abrir um título.
- Corrige o carrossel que consultava apenas RAM: agora também decodifica capas já salvas no disco, sem novos downloads nessa animação. Logo/título personalizado e fundo continuam com prioridade de rede ao abrir os detalhes.
- Ao selecionar a categoria, carrega as capas visíveis e antecipa as próximas 20 URLs. Conforme a navegação avança, conserva os arquivos anteriores e prepara os seguintes. O cache em disco pode conter mais de 20 capas, dentro do orçamento de armazenamento existente; 20 é a janela de antecipação, não o limite do cache.
- Prepara também os bitmaps das duas próximas fileiras, para evitar começar a decodificação somente quando aparecem. Essa reserva usa até 24 MiB, priorizando as capas mais próximas; sob pressão de memória, mantém a antecipação dos arquivos sem reservar esses bitmaps.
- Solicita ao Paging os metadados necessários para a antecipação e permite que a fileira que está entrando já carregue suas imagens. Mantém as duas fileiras e as opções de quatro ou cinco colunas.
- Preserva transferências sobrepostas durante a rolagem. Abrir um filme/série cancela as requisições do catálogo e libera a reserva futura de RAM antes da navegação. Os arquivos baixados permanecem em disco.

A velocidade continua dependendo da conexão e do servidor das imagens. Não há bloqueio da seleção esperando todas as capas baixarem.

## Trailer

Remove o botão visível Voltar. O botão do controle e ESC retornam aos detalhes.

Usa iframe oficial já presente na página, acompanha o início real da reprodução e oferece reprodução manual se o autoplay for bloqueado. A API pronta deixa de ser interpretada como vídeo tocando. Erros de página, resposta HTTP e processo do WebView têm mensagem de falha.

Mantém a composição por hardware e trata a superfície nativa de vídeo entregue pelo WebView, colocando-a acima do conteúdo e ocultando a camada WebView enquanto essa superfície é usada. A superfície e seu callback são removidos na troca de trailer e no encerramento. Isso trata uma possível causa de áudio com imagem preta; reprodução real no aparelho ainda precisa ser confirmada. A busca continua preferindo português e solicitando legendas disponíveis, sem gerar tradução.

## Aplicar no PowerShell

Abra o terminal na raiz do projeto, junto de `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Capas-Prontas.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/capas-prontas-trailer-video/Atualizar-Capas-Prontas.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Confira a mensagem acima.' }
.\gradlew.bat assembleDebug
```

O script valida todos os arquivos antes de escrever, aceita LF/CRLF/BOM e interrompe em alterações desconhecidas. Cria backup exato em `.navigation-backups/capas-prontas-*`, desfaz escritas em caso de falha e é idempotente. Preserva configuração local, credenciais e Gradle. O patch mostra as diferenças em relação à atualização anterior.

## Validação

28 testes passaram com Robolectric/Compose, incluindo uma categoria real com servidor HTTP local: as próximas 20 imagens são solicitadas e a fileira seguinte está decodificada antes da rolagem; depois de limpar RAM, a capa é recuperada do disco sem nova requisição. Também valida retorno ao menu, favoritos, prioridade da arte principal, camada nativa do trailer e ESC.

APK de depuração e APK dos testes Android compilados. Atualizador executado em seis cenários (ZIP, atualização inicial e atualização de trailers, cada um com LF/CRLF), além de idempotência, backup, preservação das configurações, rejeição de edição desconhecida e rollback após falha de escrita.

A imagem real dos trailers em TV/emulador não foi validada neste ambiente. Não é uma medição de desempenho na TV.
