# Capas M3U: consulta em listas automáticas e proteção contra imagem trocada

Aplicar sobre capas-provedor-dois-formatos. O pacote anterior condicionava a consulta das capas da API a forceM3u. Isso cobria M3U direta, mas podia pular uma lista automática já salva como M3U nativa. Esta revisão remove essa condição e impede que imagens compartilhadas por obras incompatíveis sejam usadas como capa confiável na M3U.

## Aplicar e instalar

Na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Após BUILD SUCCESSFUL, com a TV conectada:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

## Atualizar e conferir

Inicie a coleta antes de atualizar a lista:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao.ps1 -Formato M3U -PastaSaida "C:\Users\flavi\Downloads"
```

1. Após Captura iniciada, atualize a MESMA lista M3U na TV, sem excluir o cadastro ou apagar dados. A correção de capas não exige recriar a lista nem editar o formato no site.
2. Confira O Conto da Aia nas categorias em que aparece e O Conto da Raposa de Nove Caudas, se existir.
3. Confira outras séries e os episódios; o carregamento de episódios continua sob demanda.
4. Pressione Enter no PowerShell para salvar e envie o TXT com m3u-capas-auto no nome. A coleta inclui native_covers/rejected e series_covers/matched para verificar o caminho realmente executado.

## O que foi corrigido

A consulta opcional das capas do provedor é permitida tanto na M3U direta quanto na automática já mantida em modo nativo, desde que o endereço permita resolver a API. Essa consulta é somente de metadados e conserva os IDs e links M3U. O caminho Xtream normal continua prioritário e não recebe consultas extras por esta alteração.

Uma URL de imagem declarada por séries com nomes diferentes ou anos conhecidos conflitantes é recusada como capa M3U. A mesma proteção é aplicada às capas lidas da API durante a associação de metadados à M3U, para não recolocar a imagem trocada depois. Edições compatíveis da mesma obra continuam podendo usar sua imagem estável.

Se o provedor não entregar uma capa confiável, o catálogo mostra indisponibilidade em vez de escolher outra série ou um episódio. Não há substituição por busca TMDB. A disponibilidade e correção dos metadados reais precisam ser conferidas na TV; testes automatizados usam o caso anterior da lista com serviço de capas simulado.

Banco permanece versão 6. Filmes, categorias, ordenação, episódios, histórico, favoritos e qualidade de vídeo não foram alterados. A consulta opcional mantém o prazo de 12 segundos e o tratamento de indisponibilidade existentes. Uma M3U sem API utilizável continua importando normalmente.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Depois, atualize a lista para recalcular as capas pela regra restaurada. O script mantém backup e recusa uma base divergente.
