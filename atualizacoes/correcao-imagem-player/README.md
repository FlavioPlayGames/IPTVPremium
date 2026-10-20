# Correção do triângulo preto no player

Corrige a cobertura incompleta do vídeo ao ativar Leve ou FSR 1 + RCAS na atualização `imagem-player`.

O código usava `GL_TRIANGLE_FAN` com os quatro vértices de `GlUtil.getNormalizedCoordinateBounds()` do Media3 1.11.1. Essa sequência está organizada para `GL_TRIANGLE_STRIP`; o modo anterior deixava uma região triangular sem desenho. A correção troca o modo de desenho compartilhado pelos dois efeitos para `GL_TRIANGLE_STRIP`.

O atualizador modifica somente `PlaybackImageEffects.kt`, confere a versão antes de escrever, cria backup e aceita reaplicação. Arquivos com alterações desconhecidas são preservados e a atualização é interrompida. Não modifica os algoritmos nem as configurações de reprodução.

Na raiz do projeto no PowerShell do Android Studio:

```powershell
$arquivo = Join-Path $env:TEMP 'Corrigir-Imagem-Player.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/correcao-imagem-player/Corrigir-Imagem-Player.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Correção não aplicada.' }
.\gradlew.bat assembleDebug
```

A atualização completa em `imagem-player` também recebeu essa correção.

## Validação

O teste GLES anterior usava vértices em ordem de perímetro e não reproduzia a sequência real do Media3. O teste `validar_shaders.py` agora usa os vértices do Media3 1.11.1 e lê o modo de desenho do arquivo Kotlin do projeto. Ele falha com a versão anterior e passa após o atualizador, verificando todos os pixels dos quadros de cor constante, inclusive alfa opaco, além de bordas, cores e orientação do FSR em dois passes.

Executado com EGL/OpenGL ES por software no Linux, sem medir desempenho em Mali. Também foram verificadas aplicação, backup, reaplicação e rejeição de mudanças desconhecidas no atualizador PowerShell. A compilação `assembleDebug` foi concluída. A confirmação visual no aparelho do usuário ainda é necessária.

Para repetir o teste gráfico em Linux com Python 3 e bibliotecas EGL/GLES instaladas, execute na raiz do projeto:

```bash
python /caminho/para/validar_shaders.py
```
