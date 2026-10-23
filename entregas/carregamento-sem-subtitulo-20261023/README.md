# Carregamento sem subtítulo

Remove a mensagem de etapa abaixo do círculo, conforme a marcação enviada. Mantém IPTV PREMIUM, Carregando seu conteúdo… e o indicador animado. Não altera a importação, o cache ou sua prioridade. Aplica sobre a atualização importacao-dinamica-prioridade-20261023.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/carregamento-sem-subtitulo-20261023'
$script = Join-Path $env:TEMP 'Preparar-Carregamento-Simples.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Carregamento-Simples.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Instalador valida o arquivo e guarda backup fora de app/src. Alteração visual simples verificada por comparação do fonte; sem teste físico na TV. A opção -Testar gera o APK no seu projeto.
