# Intensidades da nitidez e modo atual visível

Atualização para quem já instalou o botão Nitidez de `imagem-player`. Altera somente `PlaybackImageEnhancement.kt` e `PlaybackScreen.kt`; pode ser aplicada antes ou depois da correção do triângulo preto.

Os níveis anteriores multiplicavam o coeficiente RCAS por 0,35 / 0,55 / 0,75. O ajuste era conservador. Agora os níveis são **Suave: 0,40**, **Média: 0,75**, **Forte: 1,00**, levando Forte ao limite original do algoritmo, mantendo a proteção contra excesso de contraste do RCAS. Aplica-se tanto ao modo Leve quanto ao estágio de nitidez do FSR 1. Não acrescenta passes ou leituras de textura.

O painel mostra **Modo atual**, com o modo e a intensidade selecionados. Quando a melhoria é desativada automaticamente, o motivo permanece no painel durante a sessão do vídeo. Isso permite conferir se o modo ficou selecionado ou se o aparelho retornou à imagem original por incompatibilidade ou perda de quadros.

Nitidez realça detalhes existentes. O resultado depende do vídeo; cenas lisas ou fontes já nítidas podem apresentar pouca diferença. O EASU só amplia quando a resolução de entrada está abaixo do limite de saída, que nesta versão é de até 1080p. Não há garantia de desempenho em Mali sem teste no aparelho.

## Aplicar no PowerShell

Na raiz do projeto no Android Studio:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Intensidade-Nitidez.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/nitidez-intensidade/Atualizar-Intensidade-Nitidez.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada.' }
.\gradlew.bat assembleDebug
```

O atualizador cria backup, verifica a versão de todos os arquivos antes de escrever e preserva alterações desconhecidas interrompendo a aplicação. A reaplicação é segura. A instalação completa em `imagem-player` também contém o ajuste.

## Validação

Oito testes de integração, preferências e controles passaram, assim como `assembleDebug`. O teste GLES por software em `../correcao-imagem-player/validar_shaders.py` usa os níveis configurados no projeto e verifica cobertura, cores e resposta crescente de detalhes. Em um padrão sintético de bordas suaves e textura, a diferença média por canal de 8 bits contra intensidade zero foi 0,647 / 1,636 / 3,460 para Suave / Média / Forte. Essa medida descreve o padrão de teste e não representa uma porcentagem de melhoria ou avaliação visual de filmes.

O atualizador PowerShell foi validado em fontes LF e CRLF, incluindo backup exato, reaplicação e interrupção antes de qualquer escrita quando há alterações desconhecidas. A percepção e o desempenho em vídeo real ainda dependem do teste no dispositivo.
