# Capa do provedor primeiro; temporada ao assistir

Vale para séries de listas Xtream e M3U, no catálogo normal, Kids, Favoritos, Continuar e catálogo do player.

- O catálogo usa diretamente a imagem do provedor. Não pesquisa a série no TMDB para montar a lista de capas.
- Depois que um episódio começa a reproduzir, prepara em segundo plano a capa da temporada no TMDB. Aguarda a prioridade de abertura do vídeo e usa a identidade real da série do catálogo.
- Quando a imagem estiver disponível, passa a usá-la nas capas da série conforme a temporada do episódio salvo. Sem capa da temporada disponível, mantém a capa do provedor.
- Enquanto uma imagem de temporada preparada está sendo carregada, a imagem do provedor pode aparecer imediatamente. O cache de imagens continua sendo usado.
- Kids e normal escolhem a temporada a partir de seus respectivos históricos. Os metadados públicos das imagens podem compartilhar cache, sem compartilhar o progresso.

As capas iniciais são exatamente as imagens fornecidas pela lista. A verificação do TMDB fica reservada à substituição por temporada quando a pessoa assiste. Filmes, reprodução, áudio, legendas e versão do banco não foram alterados.

## Instalar

Aceita os fontes das revisões `series-capas-temporada`, `series-capas-tmdb` e `m3u-capas-automaticas`. O pacote inclui os ajustes anteriores necessários. Valida os arquivos da base e cria backup antes de alterar qualquer arquivo.

Extraia para `C:\Users\flavi\Downloads\capas-provedor-temporada`. No PowerShell:

```powershell
Set-Location "C:\Users\flavi\Downloads\capas-provedor-temporada"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

**No M3U, atualize a lista uma vez depois de instalar.** As versões anteriores descartavam algumas imagens fornecidas pela lista; a atualização repõe esses campos usando a regra nova. Não precisa excluir a lista nem apagar os dados. No Xtream, a capa fornecida já pode ser usada sem atualizar.

Depois, abra Séries. Para conferir, reproduza um episódio por cerca de 20 segundos para registrar seu progresso e aguarde a preparação da capa. Ao voltar, a série poderá usar a imagem da temporada. Mudar para uma temporada anterior também acompanha o episódio salvo mais recente. Se o TMDB não identificar a obra ou não tiver imagem da temporada, a imagem do provedor permanece.

O download de uma imagem nova ainda depende da rede. Esta mudança elimina a pesquisa TMDB da abertura do catálogo; não promete carregar imagens não armazenadas instantaneamente. As capas ficam no cache existente para as próximas aberturas. A qualidade do vídeo não é alterada.

## Diagnóstico opcional

Com a TV conectada ao ADB, antes de testar uma série:

```powershell
powershell -ExecutionPolicy Bypass -File .\Coletar-Capas.ps1
```

Abra a série e reproduza um episódio; depois volte ao catálogo. Pressione Enter para salvar. O arquivo contém apenas métricas permitidas, sem títulos, URLs de lista ou credenciais. Sem reprodução pode não haver evento TMDB novo, pois o catálogo não inicia mais essa busca.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Restaura os fontes anteriores se não tiverem sido modificados depois da instalação. Não apaga os dados da TV. O pacote contém fontes e scripts, sem APK ou chaves particulares.
