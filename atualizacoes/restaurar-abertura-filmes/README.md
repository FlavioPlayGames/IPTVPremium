# Restaurar a abertura dos filmes

Desfaz somente a última atualização do giro dos filmes. Restaura AppNavigation.kt e MovieOpeningPolicy.kt exatamente aos bytes anteriores à atualização e remove o teste criado para ela. Isso retira o limite visual de 3 segundos, o novo caminho de alinhamento e as alterações de preparação/navegação dessa atualização.

A navegação do catálogo, a velocidade aprovada, a correção do Voltar e a remoção do contorno temporário permanecem no estado anterior.

No PowerShell do Android Studio, na raiz do projeto:

```powershell
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/restaurar-abertura-filmes/Restaurar-Abertura-Filmes.ps1"
$arquivo = Join-Path $env:TEMP "Restaurar-Abertura-Filmes.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Instale o novo APK. O script verifica os três arquivos antes de escrever, faz backup, preserva alterações locais desconhecidas e permite uma segunda execução.

Verificação: restauração exata comparada com a cópia anterior à atualização; script executado com a última versão, finais de linha do Windows, segunda execução e conflito sem escrita parcial. APK de depuração compilado.
