# Relatório detalhado da importação M3U

Abra **relatorio.html** no navegador para ver a análise completa. O PDF traz o resumo e o gráfico. Nenhuma instalação é necessária para ler o relatório. As planilhas e métricas usadas estão em relatorio/; não incluem os logs originais ou dados de acesso.

A importação medida caiu de 142,07 para 122,44 segundos. Os lotes só de episódios responderam por 104,35 segundos das chamadas de gravação. Os tempos se sobrepõem: não devem ser somados. Ainda não é possível separar SQL, espera e commit com a captura antiga.

## Medição mais detalhada na TV (opcional)

Este pacote acrescenta cronômetros por tipo de conteúdo e dentro/fora do corpo transacional. Mantém o banco na versão 5 e não muda o site, os índices ou as regras de importação. Não inclui uma nova otimização de velocidade. Aplique sobre o projeto já atualizado com m3u-banco-episodios; o instalador confere os arquivos e faz backup antes de alterar.

No PowerShell, dentro da pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Após BUILD SUCCESSFUL, instale o APK na TV conectada:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Inicie a coleta **antes** de atualizar a mesma lista M3U direta na TV:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -PastaSaida "C:\Users\flavi\Downloads"
```

Depois atualize UMA lista e aguarde concluir. Envie diagnostico-importacao-m3u-atual-detalhado.txt. Se houver vários aparelhos, informe -Dispositivo com o serial do adb devices. Não apague os dados do app.

Os novos campos banco*Us são microssegundos por lote. bancoForaCorpoUs inclui esperas e abertura/fechamento da transação; não significa apenas commit. As chamadas por tipo também incluem esperas e agendamento. A captura permite direcionar a investigação, sem medir CPU ou armazenamento fisicamente. Nenhuma medição nova na TV foi feita para este relatório.

Desfazer.ps1 permite restaurar apenas os arquivos desta atualização usando o backup indicado pelo Aplicar.ps1; consulte seus parâmetros. Não há nova migração de banco neste pacote.
