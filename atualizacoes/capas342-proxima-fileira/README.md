# Próximas 20 em disco, uma fileira decodificada e TMDB w342

Reduz a preparação antecipada de bitmaps em filmes e séries, mantendo a antecipação de arquivos e o cache rotativo.

## Mudanças

- Continua antecipando **as próximas 20 capas em disco**, além das capas visíveis.
- Decodifica antecipadamente somente **a próxima fileira**: cinco capas na grade de cinco colunas ou quatro na de quatro colunas. As demais capas futuras permanecem como arquivos comprimidos até se aproximarem da tela.
- O limite é aplicado tanto à fila de preparação quanto aos cards pré-compostos pelo LazyGrid. Assim, compor uma fileira mais distante não dispara uma decodificação fora da janela escolhida.
- Quando a grade avança uma fileira, os próximos arquivos já baixados são decodificados sem novo download. Não descarta bitmaps de capas visitadas que ainda caibam no LRU normal.
- Reduz a reserva máxima da próxima fileira de 24 para **12 MiB**. Mantém o cache normal de bitmaps e o orçamento rotativo de disco de 64 MiB, com preferência pelas capas visíveis e pelas próximas 20.
- Novas transferências do serviço oficial `image.tmdb.org` usam **w342** quando a URL pede um tamanho maior ou `original`. URLs menores e de outros provedores continuam iguais; parâmetros e chaves de cache são preservados.

As capas já salvas são reaproveitadas sem limpeza ou download em massa. A troca para w342 ocorre nas novas transferências; os arquivos maiores existentes saem gradualmente conforme a rotação normal. A resolução menor pode suavizar detalhes quando ampliada em uma TV 4K. Fundos, logos e vídeos mantêm suas resoluções.

Preserva o deslizamento curto de 160 ms, o foco, as duas fileiras visíveis, a preparação dos artistas e os ajustes do trailer. Não adiciona uma pausa geral do carregamento durante a rolagem. Não é uma medição de FPS ou velocidade de rede na TV.

## Aplicar no PowerShell

Pacote cumulativo para o ZIP restaurado IPTVPremium (8) e as cinco atualizações anteriores: `capas-janela-prioridade`, `capas20-trailers-internos`, `capas-prontas-trailer-video`, `capas-cache-elenco-trailer` e `deslizamento-curto-fileira` (`e6cc432`).

Na raiz do projeto, onde está `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Capas342.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/capas342-proxima-fileira/Atualizar-Capas342.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Confira a mensagem acima.' }
.\gradlew.bat assembleDebug
```

O script valida todos os arquivos antes de escrever e interrompe em alterações desconhecidas. Aceita LF/CRLF/BOM, preserva configurações e Gradle, cria backup exato em `.navigation-backups/capas342-*`, desfaz as escritas se houver falha e é idempotente.

O patch compara com `e6cc432`. Apenas quatro arquivos de produção mudam: política de tamanho do TMDB, janela de preparação e telas de filmes/séries. Os demais arquivos repetem o pacote cumulativo anterior.

## Validação

11 testes passaram com Robolectric: política restrita de URLs TMDB, proporção/memória dos bitmaps, janela de quatro/cinco colunas, cache de próxima fileira e cancelamento ao abrir um título. O teste HTTP/Compose com imagens de 780 × 1170 confirma download das próximas 20, apenas cinco bitmaps preparados, imagens mais distantes ainda sem decodificar e reutilização do disco ao rolar. Também confirma que a nova próxima fileira é decodificada sem baixar novamente seu primeiro arquivo.

APK de depuração e APK dos testes Android compilados. Atualizador validado nas seis bases aceitas com LF/CRLF, além de backup, idempotência, preservação das configurações, rejeição de edição desconhecida e rollback após falha de escrita. A fluidez final ainda precisa ser avaliada no aparelho; não houve teste de reprodução/medição de FPS na TV.
