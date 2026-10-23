# Capas, categorias e vídeo — 1.11 (13)

- Foco inicial na lista lateral, preferindo o ultimo canal assistido; Direita vai ao video e Esquerda retorna a linha anterior. Sem importar paginas anteriores apenas para recuperar o canal salvo.
- Remove a seta e o nome da categoria do cabeçalho de Canais. Voltar continua retornando às categorias; em tela cheia, primeiro retorna à prévia.
- Vídeo contido na moldura, com cantos menores; prazo de conexão de 7 segundos por tentativa, sem reiniciar o prazo ao repetir buffering.
- Controle de imagem dentro da tela cheia, no canto inferior direito: Encaixar, Preencher ou Esticar. Direita leva ao controle; OK alterna; Esquerda retorna ao vídeo. Mantém o mesmo player.
- Nome do canal no canto superior esquerdo, oculto após 3 segundos sem interação.
- Categorias com menos transparência, leve saturação e contorno branco fosco mais forte; seleção conserva a cor do tema.
- Capas reais têm prioridade. Somente quando ausentes ou após falha são usadas capas genéricas: duas opções gerais estáveis por item, uma para anime e outra para desenhos/animações. Quatro WebPs de 512×768, aproximadamente 304 KiB no total.

## Aplicar

Este pacote inclui o ajuste anterior de margem e espera de 7 segundos. Não é necessário aplicá-lo separadamente. Destina-se ao projeto enviado e à atualização anterior de Canais; alterações locais nos mesmos arquivos são protegidas por hash, antes de qualquer escrita. Cria backup fora de app/src. Não altera versão nem dados das listas e não adiciona testes ao aplicativo.

Execute no PowerShell:

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/capas-categorias-video-20261022'
$script = Join-Path $env:TEMP 'Preparar-Capas-Categorias-Video.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Capas-Categorias-Video.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Depois instale o APK gerado sobre o aplicativo existente. A compilação isolada e as verificações de política de capas e conexão são registradas em VERIFICACAO.txt. A apresentação na TV e a navegação com controle físico ainda precisam ser verificadas no aparelho.
