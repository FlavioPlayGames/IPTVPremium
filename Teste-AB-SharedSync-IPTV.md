# Teste A/B: importação sem SharedSync

Objetivo: verificar se o compartilhamento de sincronizações introduzido no pacote Otimizar-sincronizacao-segura participa da falha observada no emulador. A versão anterior funcionar na TV não basta para confirmar a causa, pois são aparelhos/ambientes diferentes. Compare no mesmo emulador e com a mesma lista.

## Única alteração

M3uSourceSynchronizer.kt deixa de usar SharedSync nas entradas M3U e Xtream e executa a operação diretamente no contexto de quem chamou. Não cria um Job independente e não compartilha ou serializa pedidos. O arquivo SharedSync.kt fica no projeto, sem ser utilizado por esse sincronizador.

Transação por lote, tamanho dos lotes, HTTP, fallback, limpeza, proteção de gerações READY/ACTIVE, Firebase, portal, parsers, telas e teclado permanecem iguais ao pacote atual. A instrumentação de tempo/heap foi mantida e a entrada passa a registrar:

    SYNC_AB modo=DIRETO compartilhamento=DESATIVADO tipo=M3U

ou tipo=XTREAM. A presença desse marcador prova que a versão de teste chegou à importação. Sua ausência, junto ao restante dos logs, ajuda a investigar etapas anteriores; não é prova isolada de falha do Firebase.

## Aplicar

Na raiz do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Testar-sem-SharedSync.ps1
```

```powershell
.\gradlew.bat :app:assembleDebug
```

Compile e instale com a mesma assinatura sobre o app existente, sem desinstalar ou limpar dados. O instalador modifica apenas esse arquivo após validar a versão conhecida, preserva BOM/quebras de linha e cria backup em .iptv-backups/teste-sharedsync-*. Pode ser repetido. -SomenteVerificar valida sem escrever.

## Comparar

1. Use o mesmo emulador, rede e link do teste anterior.
2. Mantenha o aplicativo aberto e o auxiliar Diagnosticar-envio-lista.ps1 capturando.
3. Envie uma lista pelo site e aguarde cerca de 30 segundos sem sincronizar manualmente.
4. Se não houver início, sincronize uma vez manualmente e aguarde o resultado.
5. Encerre a captura com Ctrl+C e guarde o TXT, o horário e o status exibido no site.

Se continuar falhando, não atribuir automaticamente o defeito ao SharedSync nem alterar outras partes sem evidência. Se funcionar, comparar os logs e repetir antes de decidir uma correção definitiva. Essa configuração é diagnóstica e permite pedidos duplicados simultâneos como o fluxo anterior.

## Restaurar depois da comparação

Baixe Restaurar-SharedSync.ps1 deste mesmo commit. Na raiz do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Restaurar-SharedSync.ps1
```

```powershell
.\gradlew.bat :app:assembleDebug
```

O restaurador troca apenas o mesmo arquivo de volta à configuração com compartilhamento. Também valida o conteúdo e cria backup; os demais ganhos do pacote permanecem.

## Verificações

- Compilação debug das quatro arquiteturas e universal concluída.
- Diferença de fontes limitada a M3uSourceSynchronizer.kt.
- Teste do executor direto real com adaptadores Android em JVM e coroutines reais: Job do chamador preservado em M3U/Xtream, duas operações iguais não compartilhadas, cancelamento com limpeza, exceção original propagada e métricas sem credenciais.
- Payloads de ida e volta validados: um arquivo, hashes aceitos das duas versões e nenhuma mudança no mecanismo de validação/backup/gravação do instalador já utilizado. Não houve nova execução do PowerShell nesta instância; o runtime portátil anterior não persistiu.
- O comportamento de envio no emulador precisa ser validado pelo usuário; nenhum sucesso de recebimento é presumido pela compilação.
