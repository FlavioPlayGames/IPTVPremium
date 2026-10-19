# Encaixe da rolagem em duas fileiras

A grade desliza continuamente e se acomoda no início de uma fileira: duas fileiras completas de 4 capas (8) ou 5 capas (10), quando há itens suficientes. Durante o deslocamento, as fileiras entram e saem da área visível, como na referência enviada.

Esta atualização corrige o arredondamento das medidas em pixels que podia deixar a segunda fileira cortada e fazer a navegação calcular apenas uma fileira visível. O reposicionamento automático do foco deixa de disputar a posição da grade; o destaque acompanha a capa durante a animação. Rolagem por mouse ou arraste também se acomoda na fileira mais próxima quando termina.

O carregador e o cache de capas são preservados. Permanecem as alterações anteriores de ESC, transparência, aparência e prévias de 4/5 colunas.

## Aplicar no PowerShell do Android Studio

Abra o terminal na pasta do projeto que contém `gradlew.bat` e execute:

```powershell
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/encaixe-capas/Atualizar-Encaixe-Capas.ps1"
$arquivo = Join-Path $env:TEMP "Atualizar-Encaixe-Capas.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Instale o novo APK para avaliar o movimento na TV. A atualização é cumulativa, reconhece o ZIP original e as versões publicadas anteriormente, cria backup dos arquivos alterados e interrompe antes de escrever se encontrar alterações locais desconhecidas. Uma segunda execução não reaplica arquivos já atualizados.

## Verificação

- O teste com o componente real reproduziu a segunda fileira cortada nas duas opções de colunas antes da correção.
- 18 testes de interface passaram com renderização nativa no Robolectric: encaixe de 8/10 capas, quadro intermediário da animação, destaque durante a rolagem, repetição/inversão de setas, encaixe após rolagem externa, navegação e ESC.
- APK de depuração e APK de testes compilados. Os testes no aparelho físico não foram executados neste ambiente.
- Atualizador executado com PowerShell: projeto original, atualizações anteriores, finais de linha do Windows, backup, segunda execução e conflitos antes da escrita.

Os testes de posição e foco não garantem a mesma cadência de quadros em toda TV. A comparação final com o vídeo exige executar o novo APK no aparelho.
