# Capas de séries verificadas

Esta revisão busca a capa da série no TMDB quando ela aparece no catálogo. Não é necessário atualizar nem reimportar a lista.

Vale para séries M3U e Xtream no catálogo normal, Kids, Continuar, Favoritos e nas capas principais do catálogo do player. Fotos explícitas de temporadas e episódios continuam com seu comportamento atual. Filmes continuam com o carregamento de capas atual.

## Aplicar

Base esperada: projeto com a revisão `m3u-capas-automaticas` aplicada. O instalador confere os arquivos antes de alterar qualquer um e guarda os originais. Se houver arquivo diferente, envie a mensagem; não force a aplicação.

Extraia o ZIP para `C:\Users\flavi\Downloads\series-capas-tmdb`. No PowerShell:

```powershell
Set-Location "C:\Users\flavi\Downloads\series-capas-tmdb"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Abra o aplicativo e entre no catálogo de séries. Confira O Conto da Aia e outras séries que mostravam capas trocadas. Na primeira abertura, as capas aparecem progressivamente e precisam de internet. Ao sair e voltar, o cache é reaproveitado. Usa a integração TMDB que o projeto já possui; não há um cadastro novo nesta tela.

A busca valida o nome oficial ou alternativo e, quando disponível, ano e identificadores. Não escolhe o primeiro resultado por popularidade. Se não conseguir identificar a obra com segurança, mantém a imagem padrão. A foto antiga do provedor não será exibida provisoriamente como capa da série.

Os metadados das capas verificadas têm validade de 30 dias no cache em disco, dentro da cota existente. Os arquivos de imagem seguem o cache e os limites de armazenamento atuais. Se o sistema limpar o cache, será necessário buscar novamente. Duas consultas de capa podem ocorrer simultaneamente; pedidos de itens fora da tela são cancelados.

## Diagnóstico de capas, se necessário

Com a TV conectada ao ADB, antes de abrir o catálogo:

```powershell
powershell -ExecutionPolicy Bypass -File .\Coletar-Capas.ps1
```

Abra as séries, aguarde, saia e entre no catálogo novamente. Pressione Enter no PowerShell para salvar o relatório. Envie o arquivo `diagnostico-capas-series-*.txt`. Ele registra apenas campos permitidos sobre identificação e carregamento de imagens, sem títulos, URLs ou credenciais. Não precisa atualizar a lista.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Restaura os fontes anteriores se não tiverem sido alterados depois desta revisão. Não apaga os dados da TV. Banco continua na versão 6; histórico, favoritos, prioridade do Xtream, regras de vídeo e qualidade de reprodução permanecem como estavam. O ZIP contém fontes e scripts, sem APK nem chaves particulares.
