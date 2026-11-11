# IPTV Premium r15 — teste reversível de lote Xtream 2.000

Teste solicitado sobre a configuração anterior da r14. **Um único arquivo Kotlin é alterado:** `data/cache/ImportTuningPolicy.kt`.

| Parâmetro do perfil PADRAO | Antes | Teste |
| --- | ---: | ---: |
| Limite de registros por lote Xtream | 500 | 2.000 |
| Limiar de descarga por caracteres | 262.144 | 2.097.152 |
| Limite das transações bufferizadas de gravação | 2.000 | 2.000 |
| Lote de M3U nativa | 500 | 500 |
| Limiar de M3U nativa | 524.288 | 524.288 |

O perfil PADRAO é usado pela instalação `desempenho`. Perfis alternativos de diagnóstico debug permanecem como antes. M3U que utiliza API Xtream acompanha o teste Xtream; M3U nativa conserva seus parâmetros separados.

Aumentar somente a contagem mantendo 262.144 caracteres faria o leitor descarregar muitos lotes antes de atingir 2.000 registros. Por isso o teste também amplia o limiar de caracteres. Um lote ainda pode terminar antes de 2.000 quando contém objetos grandes; a descarga ocorre depois de concluir o objeto, sem partir um registro JSON ao meio.

O catálogo continua sendo lido em fluxo, com espera pela conclusão do consumidor. Não é reconstruído inteiro em RAM. Há aumento potencial de memória temporária por lote, a ser medido na TV. Limites de cache/rotação, imagens, dados persistidos e configurações de assinatura/versão permanecem como antes. A meta total de 400 MB não é uma garantia de memória/armazenamento obtida por esse parâmetro.

## Instalar o teste

Extraia em `Downloads\teste-lotes-iptv-premium-r15`. No PowerShell do Android Studio:

```powershell
$pasta = "$env:USERPROFILE\Downloads\teste-lotes-iptv-premium-r15"
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O aplicador valida o hash do arquivo, salva a versão anterior fora do projeto em `Downloads\IPTV-restauracao-r15-...`, compila localmente e instala com `adb install -r`. O pacote não inclui APK. Preserve essa pasta de recuperação para reverter.

## Comparar com a r14

Antes de atualizar a mesma lista na TV:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\teste-lotes-iptv-premium-r15\Coletar-Importacao.ps1" -AoVivo
```

Atualize a lista uma vez, aguarde concluir e pressione Ctrl+C. Envie `Downloads\IPTV-importacao-r15.txt`.

Compare a mesma origem e verifique se as contagens de filmes/séries/canais se mantêm. O registro `DbPerf`, se disponível no buffer, deve mostrar `perfil=PADRAO`, `lote=2000` e `limiteCaracteres=2097152`. Compare também quantidade de lotes, tempo de processamento/gravação, total e `heapPicoObservadoKb`. Esse heap observado não é o PSS/RAM total do aplicativo.

Na medição anterior recebida, duas atualizações da origem M3U por API Xtream duraram 26,264 s e 29,273 s para 44.192 filmes, 9.863 séries e 2.355 canais. A gravação já agrupava até 2.000 registros por transação; portanto este teste muda a leitura/emissão, sem prometer redução equivalente no custo do banco. Ainda não há ganho medido na TV.

## Reverter e reinstalar a configuração anterior

Com a TV conectada, execute:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\teste-lotes-iptv-premium-r15\Reverter-Teste.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O comando encontra o backup r15 mais recente deste projeto, restaura o arquivo anterior e compila/instala a configuração restaurada. Não reaplica o teste. Permite repetir para concluir a instalação se ela falhar depois da restauração. Edições posteriores no fonte são protegidas por verificação de hash.

Se o backup estiver em outro local, use `Desfazer.ps1 -Backup "PASTA_DA_RESTAURACAO" -Instalar`, com o caminho informado ao aplicar. Os dados, listas e histórico da TV são preservados pela instalação sobre o aplicativo existente.
