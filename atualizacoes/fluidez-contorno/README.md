# Movimento e contorno durante a transição

Mantém a velocidade aprovada (mola de 14 rad/s), o encaixe de 8/10 capas e o retorno à categoria pelo controle.

O cálculo da animação agora preserva o progresso fracionário, em vez de usar novamente o deslocamento inteiro do layout a cada quadro. O primeiro movimento começa no primeiro quadro disponível, removendo uma espera adicional. A animação continua ligada ao relógio de quadros do Compose, sem um temporizador de 30 FPS.

Quando a seleção aponta para uma capa que ainda não entrou completamente na tela, um contorno leve é desenhado na área visível da mesma coluna. Ao terminar de entrar, a própria borda da capa assume o destaque. O contorno temporário é desenhado após o conteúdo, com pincéis em cache e sem adicionar imagens ou recompor a tela por quadro.

## Atualizar pelo PowerShell do Android Studio

Na pasta do projeto que contém gradlew.bat:

```powershell
$url = "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/fluidez-contorno/Atualizar-Fluidez-Contorno.ps1"
$arquivo = Join-Path $env:TEMP "Atualizar-Fluidez-Contorno.ps1"
Invoke-WebRequest -Uri $url -OutFile $arquivo -UseBasicParsing -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -eq 0) { .\gradlew.bat assembleDebug }
```

Instale o APK gerado para comparar. O atualizador reconhece as versões publicadas, faz backup e interrompe antes da escrita ao encontrar mudanças locais desconhecidas.

## Emulador e validação

A sensação de baixa fluidez foi relatada apenas no emulador; o usuário ainda não testou na TV. No Device Manager, confira a opção de renderização gráfica do dispositivo virtual e prefira Hardware ou Automatic quando disponíveis. O resultado também depende da aceleração gráfica e da carga da máquina.

17 testes únicos de interface passaram com renderização nativa no Robolectric, incluindo borda durante a entrada da próxima fileira em 4/5 colunas, encaixe, repetição/inversão de setas, ESC e Voltar Android. Os dois testes de contorno também foram executados com captura do desenho nativo durante a transição para inspeção visual. APK e APK de testes compilados. Atualizador executado nas versões anteriores, com finais de linha do Windows, segunda execução e conflitos sem escrita parcial.

Essas verificações validam posição, desenho e navegação. Não medem nem garantem 60 FPS no emulador ou na TV; a cadência real precisa ser medida no dispositivo.
