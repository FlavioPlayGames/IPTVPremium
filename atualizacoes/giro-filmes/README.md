# Giro suave na abertura dos filmes

Reduz o giro de 2,5 para 0,8 posições por segundo, com partida gradual de 400 ms. A parada usa o caminho mais curto até a capa escolhida, sem obrigar o carrossel a percorrer outra volta completa. O alinhamento conserva a velocidade de entrada e desacelera até zero, seguido do destaque e da expansão da capa.

A apresentação tem um orçamento visual de 3 segundos, incluindo a parada e a expansão. Quando os dados ficam prontos antes, a parada começa antes. Uma proteção encerra a apresentação no limite mesmo se a lista de capas mudar durante o carregamento. A navegação é emitida uma única vez.

A preparação remota mantém seu orçamento original. Se ainda não estiver pronta ao terminar a apresentação, a tela de detalhes abre e carrega os dados restantes pelo fluxo já existente. Erros continuam mostrando a opção de tentar novamente; o temporizador não oculta um erro. O catálogo, o Voltar e a remoção do contorno extra são preservados.

## Atualizar no PowerShell do Android Studio

Na raiz do projeto que contém gradlew.bat:

```powershell
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/giro-filmes/Atualizar-Giro-Filmes.ps1"
$arquivo = Join-Path $env:TEMP "Atualizar-Giro-Filmes.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Instale o novo APK para comparar a animação. O atualizador é cumulativo, reconhece as versões publicadas, cria backup e interrompe antes da escrita se encontrar alterações locais desconhecidas.

## Validação

15 testes passaram: quatro verificam o orçamento da apresentação, a aceleração inicial, o caminho curto sem volta obrigatória e a velocidade inicial/final do alinhamento; os demais verificam navegação da grade e Voltar. APK e APK de testes compilados. Atualizador testado nas versões anteriores, com finais de linha do Windows, repetição e conflitos sem escrita parcial.

A animação completa com os dados reais do provedor precisa ser conferida no emulador ou na TV.
