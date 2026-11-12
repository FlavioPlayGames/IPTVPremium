# Validação r20

Mudança focada em quatro fontes Kotlin, dois novos. Base r19 preservada em snapshot antes da edição. Sem alterações Gradle, recursos, assinatura, site, OpenSubtitles, SubDL, posters, histórico ou limites de importação.

## Testes automatizados

- 29 verificações de sincronização, idioma e persistência existentes passaram novamente usando os fontes atuais.
- 39 verificações do cliente SubSource e extração de arquivos: paginação com metadados, página repetida, teto de requisições, busca por IMDb, ambiguidade, tipo/ano, idiomas, variantes de nomes, temporada errada, ZIP de temporada, episódio ausente, arquivo único incorreto, limites de descompactação, troca de conta, autenticação e não exposição de chaves.
- API e SharedPreferences simulados. Parser, cliente, relógio e seleção ZIP são os fontes reais. Sem chamadas à API real nem downloads de uma conta pessoal.
- Aplicação, reaplicação, restauração, bloqueio de base divergente e bloqueio de restauração sobre edição posterior passaram no PowerShell 7.
- Os mesmos fluxos passaram com simulação do formato de arrays JSON do Windows PowerShell 5.1. Não foi executado em Windows real.
- Todos os scripts PowerShell foram analisados pelo parser.
- Coletor aceita os novos contadores e rejeita linhas com campos extras, tokens, URLs e texto privado.

## Validação real pendente

Instalar na TV com a configuração local e pesquisar O Conto da Aia T05E06. API real bloqueada pela política de rede do ambiente; a documentação oficial foi recebida em PDF e o contrato foi exercitado com respostas simuladas. O HTTP 200 dos registros r19 não revela o corpo da resposta, portanto a causa exata naquele conteúdo não foi comprovada. A r20 corrige limitações verificadas no código e torna a próxima medição conclusiva sobre as quantidades retornadas/filtradas.

Os testes não demonstram automaticamente o sincronismo de um arquivo com uma edição específica de vídeo. O ajuste manual permanece disponível e continua persistido por temporada ou filme.

## Compilação final

`compileDesempenhoKotlin` e `lintDesempenho` passaram nos fontes finais (BUILD SUCCESSFUL, 2m16s). Relatório: zero erros, 158 avisos e 6 sugestões. Os avisos existentes não foram tratados nesta entrega. O pacote não inclui APK assinado: o instalador compila com a configuração local.
