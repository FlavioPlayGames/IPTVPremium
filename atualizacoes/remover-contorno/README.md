# Remover apenas o contorno temporário

Retira somente a borda adicional desenhada durante a transição e os dois testes específicos dela. Remove o arquivo CatalogMovingOutline.kt. A borda normal da capa continua como antes.

Preserva os ajustes fracionários do movimento, o início no primeiro quadro disponível, a velocidade de 14 rad/s, o encaixe das 8/10 capas e a correção do botão Voltar.

Execute no PowerShell do Android Studio, na raiz do projeto:

```powershell
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/remover-contorno/Remover-Contorno.ps1"
$arquivo = Join-Path $env:TEMP "Remover-Contorno.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Instale o novo APK. O script verifica os três arquivos antes de alterar qualquer um, cria backup, preserva alterações desconhecidas e permite repetir a execução.
