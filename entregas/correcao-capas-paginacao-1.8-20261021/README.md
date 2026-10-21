# Correção da paginação das capas — 1.8

Correção incremental da entrega `icones-capas-125-1.8-20261021`, sobre o diagnóstico enviado pelo usuário: 22 testes executados, 5 falhas, todas em `PosterCategoryRoomDeviceTest`. Os demais 17 testes não falharam nessa execução.

A consulta independente das próximas capas usava `LoadParams.Refresh` para cada lote. O Room reposiciona o último lote curto para trás: em 160 itens, a leitura solicitada na posição 150 começava na 110. Isso repetia capas já lidas. Em resultados de 80 itens (Kids ou controle parental), eram retornadas 100 ocorrências dos mesmos itens permitidos. O diagnóstico não indica inclusão de itens bloqueados; indica duplicação dos permitidos.

A leitura passa a usar `LoadParams.Append`, preservando o deslocamento solicitado, inclusive ao começar de uma posição adiante na categoria. Não modifica o Paging visível, os filtros, ícones, limites de 125 arquivos por tipo, seleção, listas ou histórico. Não muda o banco/schema nem apaga dados.

Dois arquivos atualizados: repositório do catálogo e testes Room. Mantém os testes que falharam e acrescenta duas regressões para lote final curto e lote final filtrado em filmes e séries. Resultado esperado no emulador: **OK (24 tests)**.

## Executar

Na raiz do projeto: `Corrigir-Capas-Paginacao-1.8.ps1 -Emulador emulator-5554 -Testar`.

O instalador confere os hashes da entrega anterior, faz backup e instala com `adb install -r`, sem desinstalar. Log: `diagnostico-correcao-capas-paginacao-testes.txt`. Opções `-Verificar`, `-SomenteAplicar`, `-Projeto`, `-PacoteLocal` disponíveis. Não altera versão ou configurações privadas. Exige a entrega anterior já aplicada.

## Validação aqui

- `assembleDebug assembleDebugAndroidTest compileReleaseKotlin`: compilação concluída.
- 14 testes JVM existentes do cache: aprovados.
- Utilitário real de cálculo de offset do Room/Paging, carregado das dependências do projeto na JVM: reproduziu o recuo da versão anterior e verificou a leitura com Append em 12 tamanhos de catálogo, sem repetição ou lacunas. Não é execução do SQLite Android.
- PowerShell validado em cópia da entrega instalada: aplicação dos dois arquivos, backup exato, preservação dos demais arquivos e rejeição de alterações desconhecidas.
- Os 24 testes Android foram compilados, mas não executados aqui: este ambiente não tem emulador. O comando acima executa-os no emulador do usuário.
