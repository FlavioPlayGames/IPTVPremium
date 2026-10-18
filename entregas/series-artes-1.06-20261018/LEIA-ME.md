# Artes alternativas de séries — 1.06 / interno 8

TVmaze complementa miniaturas dos episódios; Fanart.tv complementa logos e fundos. As imagens válidas da playlist são prioritárias, seguidas pelo TMDB e pelas alternativas. O fundo da série continua nos cards sem foto válida. Nenhum extrator de vídeo é usado.

Não muda temporadas/episódios disponíveis, IDs de reprodução, importação, player, temas ou compartilhamento de progresso/favoritos. O compartilhamento permanece ativo por padrão. Não publica uma versão no servidor de atualização.

## Credenciais

TVmaze não exige chave. Fanart.tv exige uma chave própria: configure FANART_API_KEY= SUA_CHAVE (sem espaço após o sinal) no local.properties do seu projeto e recompile com o mesmo script. O pacote não contém chave nem cria uma. A chave TMDB_READ_TOKEN existente é preservada. Sem Fanart configurado, TVmaze continua funcionando; logos alternativos do Fanart não serão consultados. Obtenha a chave na conta oficial em https://fanart.tv/get-an-api-key/ .

## Identificação segura

Procura primeiro pelo IMDb fornecido pela lista ou pelos external_ids do TMDB. Sem ele, exige nome normalizado e ano exatos, incluindo o nome original fornecido pelo TMDB. Não usa primeiro resultado de busca imprecisa. IMDb conflitante, resultados ambíguos e episódios duplicados com mesma numeração são rejeitados. O número da temporada e do episódio deve coincidir com o item real da playlist. Usa o TVDB ID confirmado pelo TVmaze para consultar o Fanart; não confunde TVDB com TMDB ou com o ID do provedor.

## Desempenho

As artes não bloqueiam episódios. A consulta de metadados TMDB tem orçamento de quatro segundos, a temporada três, e alternativas oito em segundo plano. A janela de entrada de dois a cinco segundos permanece; arte pendente pode chegar depois, sem prometer tempo de rede. Cancelar fecha a conexão TMDB, evitando que o limite da tela fique preso em leitura de rede antiga.

Consulta episódios TVmaze em uma resposta por série, cacheada; usa apenas as fotos da temporada e números solicitados. A consulta é iniciada se faltarem referências específicas ou se as fotos primárias falharem. Não baixa o catálogo IPTV nem vídeos. Fanart só é chamado quando logo/fundo faltam ou falham e há chave. Candidatas a logo priorizam português, neutro e inglês. As fontes têm limites próprios e cobertura variável.

Cache dos JSONs complementares em cacheDir/series_public_artwork_v1: seis horas, até 48 arquivos e 12 MiB; resposta limitada a 4 MiB. Chaves dos arquivos são hashes. Limpar cache do Android remove esse cache; ele é reconstruído sob demanda. Não grava consultas com erro como resultado válido. Consultas simultâneas da página e prévia compartilham bloqueios/cache por URL. As imagens continuam usando o carregador e cache de imagens existentes.

Créditos discretos para TVmaze/Fanart.tv são exibidos na tela quando há artes complementares. TVmaze fornece dados sob CC BY-SA; consulte https://www.tvmaze.com/api e os termos de uso das demais fontes antes da distribuição comercial. Não inclui TVDB API paga nem alega cobertura completa.

## Executar

Preparar-Series-Artes-1.06.ps1 -Projeto CAMINHO -CapturarLog -Emulador emulator-5554

Atualizador cumulativo: 30 entradas, 21 arquivos e nove exclusões antigas. Confere todos os hashes antes, faz backup e rollback; não altera local.properties além dos dois campos de versão já usados. A chave Fanart é lida do arquivo local na compilação. Não inclui APKs, credenciais de lista, configurações de assinatura ou chaves privadas no pacote.

O modo normal compila os cinco APKs 1.06/interno 8. -CapturarLog instala por cima no emulador, executa os testes de carregamento existentes e coleta SeriesDetails, PlaylistConnection, SeriesDetailsTmdb e SeriesArtwork. Arquivo: diagnostico-series-artes-logcat.txt. -TestarApresentacao executa as provas visuais existentes. -Verificar não modifica nada; -NaoCompilar aplica sem gerar APK. Não desinstale nem limpe dados.

## Limites da validação

Testes controlados de identificação/numeração/idioma/falhas/cancelamento, transporte HTTP local e regressão das miniaturas; compilação e lint Android. Este ambiente não tem emulador nem acesso funcional ao provedor real. O efeito nas artes da sua lista precisa ser confirmado no aparelho. O Fanart real também depende da sua chave e da existência de arte.
