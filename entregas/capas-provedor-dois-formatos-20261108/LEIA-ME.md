# Capas do provedor nos dois formatos

Aplicar sobre o projeto atual, depois de xtream-restaurar-estavel. Esta atualização segue a última orientação: usar a regra do Xtream como referência tanto para Xtream quanto para M3U. Não restaura o importador M3U antigo nem a escolha da última imagem do episódio.

## Aplicar e instalar

Na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Somente após BUILD SUCCESSFUL, com a TV conectada:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Atualize a mesma lista, sem excluir o cadastro nem apagar dados. Confira capas de filmes e séries no catálogo normal, Kids e player; confira também os episódios. Para uma lista automática que esteja salva como M3U, edite e reenvie a mesma lista pelo site em modo automático para solicitar nova verificação Xtream. M3U direta continua sendo uma escolha explícita do administrador.

## Regra comum

Xtream: filme usa stream_icon; série usa cover. O catálogo carrega a URL vinculada ao item, assim como no arquivo antigo. Não pesquisa uma capa alternativa pelo título no TMDB quando a imagem falha. O cache/carregador central é o mesmo código da versão antiga comparada; o visual das telas não foi revertido.

M3U: filme usa tvg-logo. Para séries de uma lista M3U mantida em modo direto, quando o endereço get.php permite identificar a API Xtream, o aplicativo pode consultar get_series_categories e get_series para obter o campo cover do provedor. A correspondência usa nome normalizado, categoria e ano, sem associação aproximada nem escolha arbitrária entre candidatas conflitantes. Só a URL da capa é aplicada; IDs, categorias, links dos vídeos e formato de importação permanecem M3U.

A consulta é somente de metadados, não baixa todas as capas, tem limite de 12 segundos e ocorre fora da transação de banco. Se a negociação automática já confirmou que a API não está disponível, não é repetida na importação nativa. No caminho Xtream normal nenhuma dessas consultas extras é acrescentada.

Uma M3U pura não possui necessariamente a capa separada da série. Sem API utilizável, é conservada uma imagem estável do provedor; imagens variáveis entre episódios e formatos conhecidos de cena não são promovidos a capa. Sem uma candidata confiável, aparece o visual de indisponibilidade. Imagens dos episódios continuam preservadas individualmente. Uma imagem errada já enviada no próprio campo cover da API continua sendo dado do provedor, como na regra antiga.

Episódios sob demanda, ordem do provedor, prioridade Xtream, favoritos, histórico e banco versão 6 são preservados. Nenhuma alteração na qualidade de vídeo.

## Diagnóstico opcional

Inicie antes de atualizar:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao.ps1 -Formato Automatico -PastaSaida "C:\Users\flavi\Downloads"
```

Depois de concluir e conferir as capas, pressione Enter no PowerShell e envie o TXT com capas-provedor no nome. Em M3U direta compatível, stage=series_covers e matched mostram quantas séries receberam capa da API. Em Xtream normal são usados diretamente os campos do catálogo. O rótulo do coletor não força nem identifica o transporte; confira transporte/endpoint nos eventos.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Depois, atualize a mesma lista para recalcular as capas pela regra restaurada. O script guarda os arquivos originais e bloqueia alterações em uma base divergente.
