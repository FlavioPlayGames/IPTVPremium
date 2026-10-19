# Voltar do controle para a categoria selecionada

O botão Voltar do controle Android envia KEYCODE_BACK, diferente do ESC do teclado. O catálogo agora recebe essa tecla antes de uma view focada poder consumi-la. O mesmo retorno direto é usado em filmes e séries: cancela a navegação da grade e restaura o foco na categoria selecionada, permitindo escolher outra com as setas.

Quando o foco já está nas categorias, o próximo Voltar segue o comportamento normal do Android. Se o teclado de pesquisa estiver aberto, o primeiro Voltar fecha o teclado. Repetições e a liberação de uma tecla tratada não provocam uma segunda ação. A restauração automática de foco dos cards fica desativada enquanto a seleção está nas categorias.

A atualização mantém o teste atual de movimento mais lento (14 rad/s), transparência e encaixe de duas fileiras de 4/5 capas.

## PowerShell no Android Studio

Na pasta que contém gradlew.bat:

```powershell
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/voltar-categorias/Atualizar-Voltar-Categorias.ps1"
$arquivo = Join-Path $env:TEMP "Atualizar-Voltar-Categorias.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Instale o APK gerado para testar o botão Voltar do controle no emulador ou na TV. O atualizador é cumulativo, cria backup e interrompe antes de escrever se encontrar alterações locais desconhecidas.

## Validação

15 testes de interface passaram com renderização nativa no Robolectric. Os três novos casos cobrem Key.Back na tela de filmes, o encaminhamento de KEYCODE_BACK usado pela Activity (incluindo repetição/liberação) e retorno à categoria de séries com seleção da próxima categoria. Também foram executados os testes de ESC e movimento/encaixe.

APK de depuração e APK de testes compilados. Atualizador testado no ZIP original, nas versões publicadas, com finais de linha do Windows, repetição da atualização e conflitos sem escrita parcial. O botão do controle no aparelho físico ainda precisa ser conferido com o novo APK.
