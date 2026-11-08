# Importação M3U — lotes de 250 e transações medidas

Esta é uma implementação candidata validada localmente. A meta de 25 segundos na TV **ainda não foi comprovada**. O site não muda e o banco continua na versão 5, sem migração nova ou limpeza de dados.

A configuração padrão é G8: até 8 chamadas de 250 entradas, no máximo 2.000 entradas por transação. O limite de texto de 512 Ki caracteres pode encerrar o grupo antes. As transações recebem somente dados já preparados; não aguardam leitura ou parsing. O catálogo inteiro continua sendo persistido antes de READY. Xtream mantém seu caminho.

## Aplicar e compilar

Extraia o ZIP, abra o PowerShell na pasta extraída e execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Instale apenas após BUILD SUCCESSFUL. O aplicador verifica a base anterior m3u-relatorio-detalhado, salva backup e não toca em local.properties, credenciais ou banco da TV. Se houver divergência, nada será aplicado; envie a mensagem.

## Medir na TV — três atualizações da mesma lista

Para a comparação controlada, o roteiro abaixo compila e instala a estratégia selecionada, sem apagar dados. A cada rodada, ele pede Enter antes da captura; então atualize UMA vez a mesma lista M3U direta na TV e aguarde concluir. Mantenha o mesmo cenário: atualização deve ser comparada com atualização. Não remova/recrie a lista entre rodadas.

```powershell
powershell -ExecutionPolicy Bypass -File .\Benchmark-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Estrategia G8
```

Os TXT são salvos em Downloads com estratégia, rodada e horário. Envie os três. Para comparar os demais, repita o mesmo comando trocando G8 por ORIGINAL, G1 ou G4. ORIGINAL usa chamadas de 2.000 e uma transação por lote, como a captura de 130 segundos, com o código atual de diagnóstico. Este modo é referência de medição, não uma versão antiga completa do APK.

Se houver vários aparelhos, acrescente -Dispositivo com o serial do adb devices. O roteiro não importa sozinho, não muda a lista, não apaga dados e não desinstala o app.

Para coletar só uma execução com o APK já instalado:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -Estrategia G8 -Rodada 1 -PastaSaida "C:\Users\flavi\Downloads"
```

A estratégia passada ao coletor é um rótulo, não muda o APK; confira stage=config. O roteiro Benchmark-TV compila o perfil correto. O resumo final do sincronizador é o tempo da importação, não apenas das inserções. O arquivo também captura a finalização da interface quando ela ocorrer na janela final.

## Interpretar

Os campos beforeBodyUs e afterBodyUs separam antes/depois do corpo transacional. beginUs inclui a abertura do Room, sincronização das notificações e aquisição da transação; endUs inclui o encerramento e agendamento das notificações. Nenhum deles é medida exclusiva de fsync. beginCalls/endCalls confirmam se os ganchos foram executados; zero significa não observado, não custo zero. Campos internos se sobrepõem e não devem ser somados indiscriminadamente.

heapPeakObservedKb é amostragem de heap Java ao concluir grupos; não inclui memória nativa e pode perder picos entre amostras. Conteúdo e títulos não são registrados. Não envie logs brutos de outro coletor com credenciais.

RELATORIO.html e RELATORIO.md descrevem a investigação, os arquivos, resultados locais e limitações. validacao/ contém as evidências. Comparar-Coletas.py é opcional e requer Python 3; ele resume somente capturas completas e bem-sucedidas, mas não comprova identidade do arquivo M3U ou igualdade das condições da TV.

## Restaurar código

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

A restauração confere alterações posteriores e recupera os arquivos do backup. Depois compile e reinstale o APK restaurado. Como o esquema segue na versão 5, esta atualização não exige desfazer migração.
