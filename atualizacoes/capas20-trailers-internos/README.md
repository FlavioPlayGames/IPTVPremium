# Próximas 20 capas, arte principal prioritária e trailers internos

Atualização cumulativa para o **IPTVPremium (8).zip restaurado**. Também aceita o projeto que já recebeu `capas-janela-prioridade`. Não inclui os efeitos de upscale/nitidez.

## Capas e favoritos

A tela mantém duas fileiras: até 10 capas com cinco colunas, ou oito com quatro. Agora antecipa **as próximas 20 capas abaixo da janela visível**, em ambos os modos. Os arquivos continuam em cache; não há limpeza das capas já carregadas.

A fila preserva os downloads que continuam dentro da janela ao mover o foco ou avançar uma fileira. Não cancela e reinicia os mesmos arquivos a cada tecla. Há até quatro transferências de antecipação, dentro de um orçamento total de oito downloads. A antecipação guarda arquivos comprimidos; só os cards visíveis decodificam suas imagens.

Downloads de capas usam execução cancelável: a corrotina e o bloqueio da URL são liberados sem esperar o timeout de leitura. Fechamento de conexão ocorre fora da interface, e cada transferência usa um arquivo temporário próprio. Ao abrir um título, as requisições do catálogo são canceladas antes da navegação.

O coração permanece junto da nota. Fica vazado quando não é favorito e **preenchido** quando é favorito. Toque e comando de manter OK pressionado continuam disponíveis.

## Prioridade na abertura

O **logo/título personalizado e o fundo** são as imagens prioritárias. O carrossel de preparação reutiliza somente capas em RAM, sem iniciar downloads concorrentes. As fotos do elenco aguardam as tentativas da arte principal. Em séries, imagens de temporadas/episódios ficam para depois dessa primeira apresentação; a abertura não espera todas as miniaturas para revelar a ficha.

Quando uma produção não oferece logo ou fundo, mantém as alternativas e os limites de espera existentes. O título em texto permanece como alternativa. A prioridade não garante que uma imagem ausente no provedor passe a existir.

## Trailers dentro do aplicativo

Os botões de filmes, séries e prévias de filmes na filmografia abrem uma tela interna, sem encaminhar ao YouTube ou navegador externo.

- Busca sob demanda no TMDB usando o ID do título; sem ID, procura o nome e o ano e exige correspondência do nome. Não busca trailers enquanto o usuário navega nas capas.
- Prioriza trailers em português, com preferência por versões identificadas como dubladas ou legendadas, e versões oficiais. Também aceita o trailer válido fornecido pelo provedor.
- Trailers do YouTube usam o **player incorporado oficial** em WebView, com interface em português e solicitação de legendas em português (`cc_lang_pref=pt`, `cc_load_policy=1`). Não extrai URLs de streaming do YouTube.
- Arquivos de vídeo diretos, como MP4 e HLS, usam Media3 dentro dessa tela e preferem faixas de áudio e legendas em português.
- Voltar retorna aos detalhes. No player do YouTube, OK alterna pausa/reprodução; esquerda/direita buscam dez segundos. ESC também fecha a tela.
- Vídeos removidos, restritos ou que bloqueiam incorporação exibem uma mensagem e a opção **Tentar outro**. A tela não abre outro aplicativo automaticamente.

A preferência **não cria dublagem, tradução ou legendas**. A língua e as legendas dependem do vídeo e das faixas disponíveis; o idioma informado no catálogo nem sempre comprova a língua falada. A incorporação também depende do suporte do WebView/YouTube no aparelho. A busca usa a configuração TMDB já existente no projeto, preservando o trailer do provedor quando a consulta falha.

## Atualizar pelo PowerShell

Na raiz do projeto no Android Studio, onde está `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Capas20-Trailers.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/capas20-trailers-internos/Atualizar-Capas20-Trailers.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Veja a mensagem acima.' }
.\gradlew.bat assembleDebug
```

O atualizador confere todos os arquivos antes de escrever, aceita LF/CRLF e BOM, cria backup em `.navigation-backups/capas20-trailers-*` e restaura os originais se a gravação falhar. Pode ser reaplicado. Alterações desconhecidas interrompem a aplicação, preservando o projeto local.

São 17 arquivos de produção e cinco de testes. Não modifica Gradle, assinatura, credenciais, dados da playlist ou o player principal. O manifesto registra somente uma nova Activity interna de trailers. O patch permite revisar as mudanças em relação ao estado anterior.

## Validação

- 23 cenários executados em testes Android no host: janela de 20, preservação de pedidos em andamento, cancelamento de download bloqueado em servidor local, prioridade do logo/fundo antes do elenco, favoritos, foco/ESC, seleção de trailers por idioma/tipo, identificação correta de séries, validação de URLs, player interno e parâmetros de legendas.
- Compilação do APK de depuração e do APK dos testes Android concluída.
- PowerShell validado com o ZIP restaurado e com a atualização anterior, em LF e CRLF: aplicação, integridade, backup exato, reaplicação, preservação da configuração local, rejeição de alterações desconhecidas e restauração após falha de gravação.

A busca do TMDB foi testada com respostas de exemplo; os testes de rede usaram servidor local. **Não foi reproduzido um trailer real do YouTube nem medido o desempenho na TV física neste ambiente.** Confirme no aparelho a velocidade das capas, a primeira apresentação dos detalhes e a compatibilidade do player incorporado.
