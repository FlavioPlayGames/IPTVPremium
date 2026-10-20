# Deslizamento curto por fileira

Substitui o movimento de mola do catálogo por um deslizamento curto e remove o zoom da seleção, em filmes e séries.

## Comportamento

- Ao precisar revelar outra fileira, desliza no máximo uma fileira por etapa, com duração de **160 ms** e curva `FastOutSlowInEasing`.
- Não usa a simulação de mola nem o multiplicador de velocidade ao segurar a seta. As capas não aumentam ou diminuem ao receber foco; o contorno de seleção permanece.
- Ao soltar uma seta mantida pressionada, conclui somente a fileira em movimento e descarta repetições acumuladas. Não continua deslizando por inércia.
- Ao parar, alinha a grade exatamente ao início da fileira, com duas fileiras completas: oito capas em quatro colunas ou dez em cinco, quando há itens suficientes. Durante o deslizamento, a fileira entra progressivamente.
- Preserva a coluna selecionada, os limites da última fileira incompleta, o retorno à categoria por Voltar/ESC e a ida à pesquisa ao subir da primeira fileira.
- Roda/arrasto do mouse também se acomoda no início de uma fileira com o mesmo deslizamento curto.

A animação anterior era calculada por quadro com velocidade e amortecimento. O novo caminho executa uma animação de duração fixa por fileira; o zoom e suas camadas gráficas adicionais foram retirados dos cards. A duração não garante 60 FPS em um aparelho: a fluidez real depende da CPU/GPU e da carga de imagens.

Esta alteração não modifica o carregador de capas, sua qualidade, a antecipação de 20 imagens ou o cache rotativo em relação a `388028d`. Também preserva a preparação do elenco e os ajustes anteriores do trailer.

## Atualizar pelo PowerShell

Atualização cumulativa: aceita o ZIP restaurado IPTVPremium (8), `capas-janela-prioridade`, `capas20-trailers-internos`, `capas-prontas-trailer-video` e `capas-cache-elenco-trailer` (`388028d`).

Na raiz do projeto, junto de `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Deslizamento-Curto.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/deslizamento-curto-fileira/Atualizar-Deslizamento-Curto.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Confira a mensagem acima.' }
.\gradlew.bat assembleDebug
```

O script valida todos os arquivos antes de escrever, aceita LF/CRLF/BOM e rejeita alterações desconhecidas. Preserva Gradle, credenciais e configurações locais. Cria backup exato em `.navigation-backups/deslizamento-curto-*`, desfaz as escritas se ocorrer falha e não reaplica arquivos já atualizados.

O patch compara com `388028d`, usando o ZIP restaurado para os arquivos que ainda não estavam no pacote anterior. Apenas três arquivos de produção mudam: o navegador compartilhado e os cards de filmes/séries; os outros arquivos de produção repetem o pacote cumulativo.

## Validação

28 testes de navegação/rolagem/cache passaram com Robolectric/Compose: 14 verificam movimento intermediário, acomodação em oito/dez capas, inversão de direção, repetição/soltura da seta e roda do mouse; dez cobrem foco e navegação por coluna; três verificam retorno à categoria; um confirma as próximas capas preparadas e reutilizadas do disco.

APK de depuração e APK dos testes Android compilados. Atualizador executado nas cinco bases aceitas, cada uma com LF e CRLF, incluindo backup exato, idempotência, preservação de configurações, rejeição de alteração desconhecida e rollback após falha de escrita.

Dois cenários adicionais do antigo fixture de navegação (foco inicial do teclado e escolha automática entre os dois itens de menu) falham também com o navegador original neste host e não entram nos 28 testes acima. Os testes de transição foram executados separadamente e os de integração usaram Java 21 para contornar uma falha nativa do Robolectric/Java 25 ao localizar buffers NIO. Não houve alteração de JDK ou Gradle no projeto distribuído. Não foi feita medição de FPS em TV ou emulador.
