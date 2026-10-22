# Carregamento por acesso e ocioso, sem grupos de 7 · 1.11 / 13

Esta é a correção que substitui o teste de importação completa anterior. Restaura o fluxo combinado; retira somente a divisão por grupos de 7 categorias.

## Fluxo

1. Uma lista **Xtream** enviada pelo site é cadastrada/validada e seu índice de categorias é preparado. A Home não aguarda todos os títulos.
2. Abrir **Filmes, Séries ou Canais** dá prioridade ao conteúdo daquela seção. Cada tarefa consulta o endpoint completo da seção, sem segmentação personalizada em 7 categorias e sem conferir novamente todos os itens ao terminar uma fila de categorias.
3. Sem interação por 2,5 segundos, o trabalho ocioso segue **Filmes → Séries → Canais**, uma seção por vez. Uma seção acessada pelo usuário passa à frente.
4. Teclas de navegação interrompem a tarefa ociosa. Abrir detalhes/reprodução, sair do primeiro plano ou mudar de tela também pausa/cancela a etapa em andamento. O conteúdo da seção que o usuário está solicitando continua sendo a prioridade de primeiro plano.
5. Seções concluídas e seus checkpoints são reutilizados, inclusive ao reabrir o app. Uma seção interrompida antes de concluir pode precisar reiniciar sua requisição; não existe paginação remota universal nos endpoints Xtream. Os itens só são publicados depois de a seção estar pronta.
6. Nas atualizações, o catálogo anterior continua disponível. A troca para a nova geração ocorre após concluir as três seções. Falhas não trocam o tipo da lista para M3U nem apagam o catálogo anterior.

Os checkpoints do primeiro teste são migrados: se uma seção já foi integralmente conferida, continua concluída; as restantes usam o endpoint completo, sem a antiga fila.

**Padrões técnicos mantidos:** Xtream 250 itens por lote / M3U 2.000. Esses lotes são de parsing/gravação, não grupos de categorias. Perfis de diagnóstico debug escolhidos manualmente continuam disponíveis.

**M3U nativa:** conserva o fluxo já existente de baixar e ler o arquivo inteiro, pois não oferece endpoints separados para canais/filmes/séries. A importação independente por seção descrita acima é para a origem Xtream, incluindo listas convertidas pelo site e cadastradas como Xtream. O formato cadastrado permanece autoritativo.

## Instalar

Use esta entrega, não o teste anterior de importação completa. Compatível tanto com o primeiro pacote por acesso quanto com o teste completo. Feche o Android Studio para evitar o bloqueio de R.jar e mantenha o emulador aberto.

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script = Join-Path $env:TEMP 'Preparar-Ocioso-Sem-Sete-1.11.ps1'
Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/ocioso-sem-sete-1.11-13-20261022/Preparar-Ocioso-Sem-Sete-1.11.ps1' -OutFile $script
if ((Get-FileHash $script -Algorithm SHA256).Hash -ne '838ca69d2ef5ee569ba62bc24df1580b909c4c8ce1612c6046d1feb3df4ea1ed') { throw 'Arquivo diferente do esperado.' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador emulator-5554 -Testar
```

Não precisa limpar dados nem importar novamente uma lista que já esteja completa. Para observar o novo fluxo, envie uma nova lista Xtream pelo site ou solicite uma atualização da lista atual. Teste entrando em uma seção e, depois, deixando o app sem interação; detalhes/reprodução devem suspender o trabalho ocioso.

O instalador altera quatro arquivos de produção e duas classes de testes. Valida todos os arquivos antes de escrever, cria backup em `.navigation-backups/ocioso-sem-sete-111-13-*` e instala com `adb install -r`. Preserva imagens, cache, listas e histórico. Não altera `app/build.gradle.kts`, mantendo **1.11 / versionCode 13** conforme configurados no projeto.

`-Verificar` somente confere. `-SomenteAplicar` aplica sem compilar/instalar. `-Testar` executa 18 testes no emulador informado (9 de política e 9 de integração Room/DataStore). Diagnósticos na raiz do projeto: `diagnostico-ocioso-sem-sete-111-13-compilacao.txt` e `diagnostico-ocioso-sem-sete-111-13-testes.txt`.

## Verificação

- Compilação debug, AndroidTest e Kotlin release verificada.
- 9 testes da política executados na JVM, incluindo ausência do limite de 7, prioridades e reaproveitamento.
- 14 verificações de transporte executadas: endpoints completos por seção, ordem de transporte Filmes/Séries/Canais, lotes 250/250/1, conservação de itens e erros sem fallback M3U.
- Os 9 testes de integração usam Room/DataStore reais e incluem prioridade da seção, interrupção por reprodução e por navegação, retomada, troca da fonte e atualização atômica. Compilados para Android; não executados em emulador nesta sessão cloud.
- Não foi medido o tempo na TV. Recursos substitutos usados apenas para compilar em cloud não fazem parte desta entrega; nenhuma imagem foi alterada.
