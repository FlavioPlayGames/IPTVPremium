# Cache rotativo de capas, elenco antes da abertura e trailer

Atualização cumulativa para o ZIP restaurado **IPTVPremium (8)** e as atualizações `capas-janela-prioridade`, `capas20-trailers-internos` e `capas-prontas-trailer-video` (`58928ba`). Não inclui upscale.

## Capas e cache rotativo

O disco mantém o orçamento rotativo existente de **64 MiB**, compartilhado pelas pastas de capas e imagens dos detalhes. Remove arquivos menos recentes ao ultrapassar esse orçamento; não limpa o catálogo ao selecionar uma categoria. As capas visíveis e as próximas 20 recebem preferência de retenção, e arquivos sendo baixados/decodificados não são removidos. A janela de antecipação é de 20 capas, não um limite de 20 arquivos no cache.

As duas próximas fileiras continuam preparadas em RAM (até 24 MiB reservados). Os bitmaps do catálogo agora são limitados proporcionalmente a **512 × 768**, sem ampliar imagens pequenas. Antes, uma imagem de 780 × 1170 podia ocupar mais de 3 MiB; dez dessas excediam a reserva e expulsavam capas que deveriam estar prontas. O novo limite permite que dez capas verticais caibam nessa reserva.

Para novas transferências exclusivamente do serviço oficial `image.tmdb.org`, capas maiores que `w500` ou `original` usam `w500`. Preserva URLs menores, URLs de provedores, parâmetros e a chave original do cache. Assim, reaproveita os arquivos já existentes e diminui a transferência de capas grandes do TMDB. Não altera a resolução do fundo, logo ou vídeo.

A animação de abertura continua reutilizando as capas de RAM/disco, sem iniciar downloads concorrentes. O cache de disco não é apagado ao abrir os detalhes. A velocidade final ainda depende do servidor e da conexão; não é uma medição de desempenho na TV.

## Ordem na abertura do filme

1. Resolve os dados e prepara título personalizado/logo e fundo com prioridade.
2. Ainda na tela de carregamento, prepara as fotos dos **seis primeiros artistas com imagem disponível**, usando o mesmo cache e as mesmas dimensões dos cards da ficha.
3. Depois dessa etapa, segue para a animação de abertura da ficha.

O elenco volta a ser incluído no preparo: antes a lista de URLs calculada era substituída por `emptyList()`. Reserva até três segundos adicionais de orçamento de corrotina para essa etapa, depois do orçamento da arte principal. Imagens ausentes ou com falha não prendem a abertura indefinidamente; permanecem os timeouts de rede existentes. Não carrega filmografias completas nem adiciona uma nova seção de artistas à ficha de séries.

## Trailer

Remove o aviso persistente “Trailer em português priorizado” e os avisos equivalentes de idioma. A preferência de idioma continua aplicada na busca/legendas. Mantém mensagens de carregamento, reprodução manual e erro, além do retorno pelo controle/ESC.

Retira a camada gráfica adicional forçada no WebView: mantém a aceleração por hardware da Activity e usa `LAYER_TYPE_NONE`, com fundo nativo transparente e HTML preto. O suporte à superfície nativa de vídeo continua presente. Isso trata uma possível causa da tela preta com áudio; a imagem real no aparelho ainda não foi confirmada. Não afirma que o problema esteja comprovadamente resolvido.

## Aplicar no PowerShell

Na raiz do projeto, junto de `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Cache-Elenco.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/capas-cache-elenco-trailer/Atualizar-Cache-Elenco.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Confira a mensagem acima.' }
.\gradlew.bat assembleDebug
```

Valida todos os arquivos antes de escrever, aceita LF/CRLF/BOM, preserva configurações/Gradle, cria backup exato em `.navigation-backups/cache-elenco-*`, rejeita alterações desconhecidas e restaura as escritas em caso de falha. Executar novamente não reaplica a atualização.

O patch mostra diferenças em relação a `58928ba`; arquivos novos ou ainda não presentes nessa atualização são comparados ao ZIP restaurado.

## Validação

33 testes passaram com Robolectric/Compose. Incluem prioridade da arte principal e espera da foto do artista antes da conclusão do preparo, rotação por limite de disco, preferência da janela atual, proteção de arquivos em uso, otimização restrita ao TMDB, dimensões/budget dos bitmaps, próxima fileira pronta antes da rolagem, retorno ao menu e trailer/ESC.

Também foi verificada a preparação de duas próximas fileiras com imagens de 780 × 1170: todas as dez ficam prontas, com bitmaps dentro do novo limite, antes de rolar. APK de depuração e APK dos testes Android compilados.

O atualizador foi executado nas quatro bases aceitas, com LF e CRLF, mais idempotência, backup, preservação de configurações, rejeição de edição desconhecida e rollback após falha de escrita. Reprodução real do YouTube e desempenho na TV não foram validados neste ambiente.
