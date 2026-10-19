# Movimento mais suave

Mantém a navegação atual e suaviza a aceleração e a desaceleração da grade. A resposta da mola passa de 32 para 20 rad/s, aumentando o tempo de acomodação sem oscilação. Setas repetidas continuam mudando o destino sem reiniciar a animação. O encaixe permanece em duas fileiras de 4 ou 5 capas (8/10 itens completos quando há itens suficientes).

A sugestão anexada usa animação de 150 ms e cancela/reinicia a cada comando. Ela também não contém as interfaces atuais de retorno ao pesquisar/categorias, entrada/restauração e destaque durante o movimento. Por isso foi preservada a implementação atual e ajustada apenas a resposta do movimento.

## Aplicar no terminal PowerShell do Android Studio

Na pasta que contém `gradlew.bat`:

```powershell
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/movimento-suave/Atualizar-Movimento-Suave.ps1"
$arquivo = Join-Path $env:TEMP "Atualizar-Movimento-Suave.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Instale o novo APK para comparar o movimento na TV. O atualizador é cumulativo, reconhece as versões publicadas, faz backup e interrompe antes de escrever ao encontrar modificações locais desconhecidas.

## Validação

18 testes de interface com renderização nativa no Robolectric: movimento intermediário, encaixe de 8/10 capas, destaque durante a transição, repetição e inversão de setas, rolagem externa e ESC. APK e APK de testes compilados. Atualizador testado com o ZIP original, versões anteriores, finais de linha do Windows, segunda execução e conflitos sem escrita parcial.

A cadência percebida no aparelho físico ainda precisa ser conferida com o novo APK.
