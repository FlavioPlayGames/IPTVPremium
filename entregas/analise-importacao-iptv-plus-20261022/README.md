# Importação do IPTV Plus: achados e próxima coleta

O [relatório](relatorio-importacao.md) documenta o que foi confirmado no APK 3.0.4 e distingue os pontos ainda desconhecidos. O APK completo foi reconstruído e validado com o manifesto; não foi publicado aqui.

A descoberta principal é uma interface web carregada por JavaScript externo, com ponte nativa para HTTP assíncrono e uma rotina opcional de download seguido de upload. Isso não comprova ainda qual caminho é usado em cada tipo de lista nem explica, sozinho, o tempo de importação.

## Obter o arquivo que coordena a importação

O endereço abaixo é o mesmo referenciado pelo APK. O script só baixa o arquivo público e registra tamanho/data/hash; não executa JavaScript, não abre o aplicativo e não lê dados de listas. O domínio não estava acessível na rede do ambiente de análise; por isso essa coleta deve ser feita no seu Windows.

Execute no PowerShell:

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script = Join-Path $env:TEMP 'Baixar-Interface-IPTVPlus.ps1'
Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/analise-importacao-iptv-plus-20261022/Baixar-Interface-IPTVPlus.ps1' -OutFile $script
if ((Get-FileHash $script -Algorithm SHA256).Hash -ne '728a6c072512c97895bb5c742f10b546207af1f5d5d8ee856a80daab3883bad7') { throw 'Arquivo diferente do esperado.' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script
```

Será criada uma pasta `Interface-IPTVPlus-*` nos Downloads. Envie no chat:

- `iptvplus-interface.js`
- `origem-interface.json`

Não é necessário reenviar o APK, informar URL da lista, usuário ou senha, nem limpar dados do emulador. Se o servidor recusar o download, envie somente o erro; não altere proteções ou credenciais.

O arquivo remoto pode mudar independentemente da versão do APK. A análise posterior manterá seu hash/data separados da evidência do APK.

## Verificação

Script validado com respostas simuladas: JavaScript preservado sem executar; hash/tamanho conferidos; HTML, HTML com BOM e arquivo vazio rejeitados. A obtenção real desse JavaScript continua pendente. Nenhum código do nosso projeto ou versão 1.11/13 foi alterado.
