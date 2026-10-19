# Teste de movimento mais lento

Reduz a resposta da mola de 20 para 14 rad/s: o movimento leva aproximadamente 43% mais tempo para se acomodar. Mantém a navegação, o cache e o encaixe em duas fileiras de 4/5 capas. A suavidade percebida precisa ser comparada na TV.

Na pasta do projeto no PowerShell do Android Studio:

```powershell
$nome = "Atualizar-Teste-Mais-Lento"
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/teste-mais-lento/$nome.ps1"
$arquivo = Join-Path $env:TEMP "$nome.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Para voltar à velocidade aprovada anteriormente, use o mesmo bloco com a primeira linha:

```powershell
$nome = "Restaurar-Movimento-Anterior"
```

Instale o APK gerado após aplicar ou restaurar. Os scripts são cumulativos, verificam os arquivos antes da escrita, preservam alterações desconhecidas e fazem backup.

Validação: 18 testes de interface passaram, incluindo encaixe de 8/10 capas, animação, repetição/inversão de setas e ESC. APK compilado. Atualizador testado nas versões anteriores, com finais de linha do Windows e conflitos. Restauração, segunda restauração e reaplicação do teste verificadas.
