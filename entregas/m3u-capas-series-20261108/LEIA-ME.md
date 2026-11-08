# Capas das séries M3U

Aplicar sobre m3u-oficial. Corrige a escolha da capa do catálogo sem usar automaticamente a imagem do último episódio.

## Aplicar e instalar

Na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Após BUILD SUCCESSFUL, com a TV conectada:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Atualize uma vez a mesma lista M3U para substituir as capas salvas anteriormente. Não apague dados nem recrie a lista. Confira O Conto da Aia nas categorias em que aparece, outra série afetada e as imagens dos episódios.

## Regra corrigida

A imagem de cada episódio continua guardada para a tela de episódios. A capa da série usa uma imagem de retrato reconhecida ou uma imagem estável do provedor. Imagens variáveis sem identificação de capa e a versão w1280 de cenas do CDN TMDB deixam de virar capa por serem a última entrada da lista. Isso é uma política conservadora de seleção, não uma detecção visual de todas as imagens.

Uma capa compartilhada por nomes incompatíveis é descartada. É possível aproveitar uma capa de outra categoria quando o nome normalizado corresponde, os anos não conflitam e existe uma única candidata compatível. Remakes/identificações ambíguas não são misturados. Quando não há candidata, o carregamento visível do catálogo usa a busca oficial existente; sem correspondência confiável, permanece o visual de capa indisponível, sem escolher outra série.

A busca remove o ano duplicado em nomes como O Conto Da Aia 2017 (2017), sem mudar o título exibido, IDs ou agrupamento. Filmes e importação Xtream permanecem com as regras existentes. A normalização de ano duplicado aplica-se apenas à busca de arte de séries; nomes comuns não são alterados.

A escolha local das capas não faz chamadas HTTP durante a importação. A busca oficial continua restrita às capas visíveis, com os limites existentes. Banco permanece na versão 6; episódios sob demanda, ordem, favoritos e Continuar são preservados.

## Diagnóstico opcional

Inicie antes da atualização da lista:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -PastaSaida "C:\Users\flavi\Downloads"
```

Depois de concluir, teste séries e pressione Enter no PowerShell. O TXT inclui stage=artwork, kept (capas próprias mantidas), reused (capas reaproveitadas entre edições compatíveis) e missing (sem candidata local; elegíveis à busca oficial). Não inclui nomes, links ou credenciais.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Depois, atualize a lista para recalcular as capas pela regra restaurada. O script mantém backup e recusa sobrescrever fontes divergentes.
