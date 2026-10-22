# Preparar APK completo para análise

O arquivo iptv-plus.apk enviado excedeu o limite de leitura de 32 MiB por arquivo desta ferramenta. O script divide o APK em ZIPs de até aproximadamente 24 MiB, sem excluir qualquer parte do código e sem alterar o APK original ou o emulador. Não abre o aplicativo, não lê dados instalados e não modifica nosso projeto.

Execute no PowerShell:

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script = Join-Path $env:TEMP 'Dividir-IPTVPlus-Analise.ps1'
Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/dividir-apk-analise-20261022/Dividir-IPTVPlus-Analise.ps1' -OutFile $script
if ((Get-FileHash $script -Algorithm SHA256).Hash -ne '035ed0a6ceb5524dc3ea8ba1798d022e49adbaea6ff7183d3ec1d3c3e9d640af') { throw 'Arquivo diferente do esperado.' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script
```

Por padrão, lê `Downloads\iptv-plus.apk` e cria uma pasta nova `Analise-IPTVPlus-*` ao lado dele. Se o arquivo estiver em outra pasta, acrescente `-Apk 'C:\caminho\iptv-plus.apk'` ao comando final.

Envie no chat **todos** os arquivos `iptv-plus-parte-*.zip` e o `manifesto-apk.json` dessa pasta. Pode enviar em várias mensagens. O manifesto permite conferir a ordem, os tamanhos e os hashes SHA256 de cada parte e do APK completo. Não é preciso enviar o APK original novamente.

As partes serão reconstruídas apenas no ambiente de análise. Nenhuma parte do APK, dados da lista ou credenciais é publicada neste GitHub; esta pasta contém somente o script e sua verificação com arquivos de teste aleatórios.

Verificação: sintaxe PowerShell aprovada; reconstrução byte a byte testada com arquivo pequeno, tamanho exato e última parte incompleta; APK de origem preservado; pasta existente rejeitada para não sobrescrever arquivos.
