# Nitidez: Suave 25%, Médio 50%, Forte 80%

Ajusta somente `PlaybackImageEnhancement.kt`, com os coeficientes RCAS de 0,25 / 0,50 / 0,80. Os percentuais também aparecem nos nomes das opções do painel. As preferências existentes são mantidas porque os nomes internos `SOFT`, `MEDIUM` e `STRONG` continuam iguais.

São percentuais do controle de intensidade, não uma porcentagem de ganho de qualidade. A versão anterior usava 40% / 75% / 100%; portanto, os valores pedidos nesta atualização reduzem a força em relação àquela versão. Não altera o escalonamento nem o caminho de renderização, e não corrige o borrado relatado na TV 4K. Não acrescenta custo de processamento.

No projeto existente, execute pelo PowerShell do Android Studio, na pasta que contém `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Nitidez-Percentuais.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/nitidez-percentuais/Atualizar-Nitidez-Percentuais.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada.' }
.\gradlew.bat assembleDebug
```

O atualizador aceita as duas versões anteriores publicadas do arquivo, cria backup, verifica a gravação e permite reaplicação. Alterações desconhecidas são preservadas e impedem a aplicação. A instalação completa em `imagem-player` também usa estes percentuais.

Validação: `assembleDebug`, verificação dos shaders por EGL/GLES em software com os níveis atuais e teste do atualizador sobre ambas as versões anteriores, incluindo CRLF, backup exato, reaplicação e rejeição de mudanças desconhecidas. Não houve novo teste em TV física.
