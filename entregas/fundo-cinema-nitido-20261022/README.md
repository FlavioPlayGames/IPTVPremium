# Fundo de cinema mais nítido

Substituição da arte genérica exibida quando Filmes/Séries não têm backdrop disponível. Base: java.zip mais recente enviado nesta solicitação. Mantém o lado esquerdo escuro para títulos e sinopse; concentra películas e luz dourada à direita, com detalhe vermelho discreto. A imagem é estática, sem letras ou logotipos.

![Nova arte](cinema_details_fallback.webp)

## Definição e tamanho

WebP de **1672×941**, **162.492 bytes (aproximadamente 159 KiB)**, qualidade 94. Não é 4K nativo e não foi ampliado artificialmente para um arquivo 4K. É uma arte substituta gerada para esta entrega e comprimida mantendo sua resolução original.

O código anterior reduzia uma imagem desse tamanho para 836×470 antes de ampliá-la na TV. Agora mantém 1672×941, decodifica em ARGB_8888 e usa filtragem de alta qualidade no desenho. Uma única imagem fica compartilhada em memória, cerca de 6 MiB de pixels, sem decodificação a cada quadro.

O componente também é usado por Séries: o fallback genérico muda nas duas telas. Imagens próprias do conteúdo continuam tendo prioridade. Não altera player, importação, fontes dos títulos, legendas ou a interface de seleção. O desfoque intencional ao abrir a sinopse completa permanece.

## Aplicar

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/fundo-cinema-nitido-20261022'
$script=Join-Path $env:TEMP 'Preparar-Fundo-Cinema-Nitido.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Fundo-Cinema-Nitido.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O instalador verifica o ZIP, a imagem e o código enviado; faz backup em `.cinema-backups`. Atualiza somente CinemaDetailsFallback.kt e substitui as variantes do recurso `cinema_details_fallback` por um único WebP em drawable-nodpi. Evita manter PNG e WebP com o mesmo nome na mesma configuração. Não mexe nos outros recursos. Não cria arquivos de testes no aplicativo.

Depois de BUILD SUCCESSFUL, gere e instale o APK normalmente na TV. O script não instala automaticamente. Confira um filme sem fundo próprio e um com fundo próprio; a arte genérica não deve substituir o segundo.

## Validação

Consulte verificacao.txt. O ZIP de fontes não inclui recursos ou configuração Gradle completos: na estrutura Android isolada foram usados aliases temporários de compilação para cinema_title e cast_missing_prata, ausentes nesta máquina. Esses aliases não entram no pacote e não substituem os recursos do seu projeto. Não executado na TV nem validado visualmente no aparelho.
