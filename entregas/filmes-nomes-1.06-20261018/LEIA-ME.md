# Filmes e séries: nomes oficiais e artes alternativas — 1.06 / interno 8

Filmes passam a usar a mesma identificação segura das séries: ID TMDB, IMDb, ou nome original/traduções/títulos alternativos com validação de ano e correspondência única. A ficha é buscada em pt-BR, incluindo título textual, sinopse, gêneros e elenco disponível. Traduções originais -> inglês -> português brasileiro são aprendidas dos dados oficiais; quando original e inglês coincidem, não repete tentativa. Inglês de outra região é aceito quando não houver en-US. Sem nomes oficiais/ID, primeiro é necessário identificar a obra a partir do nome da lista.

## Fontes e prioridade

- Imagens válidas da lista continuam prioritárias; TMDB complementa dados e artes.
- TVmaze atende séries e seus episódios. Continua usando IMDb quando possível, depois original/inglês/pt-BR confirmados. Não consulta TVmaze para filmes.
- Fanart.tv é consultado por identificadores, sem busca textual: TMDB/IMDb em filmes e TVDB confirmado no fluxo TVmaze em séries. Não confunde namespaces. Filmes passam a buscar logos/fundos Fanart quando ausentes ou quando as imagens principais falham na decodificação. Logos priorizam português, neutro e inglês. Filmes podem tentar IMDb se a consulta Fanart por TMDB não fornecer arte; IDs conflitantes são rejeitados.
- Mantém os caches existentes, cancelamento e limites de resposta. Fanart de filmes usa o mesmo cache público limitado das séries. Não baixa vídeos, extrai frames ou altera o player.

## Caminhos de filmes

Xtream/get_vod_info agora preserva o nome do provedor e aceita tmdb_id/tmdb numéricos válidos. Se faltar nome/ano, consulta somente esse filme no catálogo local. A M3U nativa pode enriquecer somente o item local aberto, sem inventar ID Xtream nem consultar get_vod_info indevidamente. Sincronização do catálogo não baixa as fichas de todos os filmes.

O ID de reprodução, stream, favorito e progresso ficam inalterados. O nome localizado é apresentação; o nome da lista é preservado separadamente. As artes na troca de filme usam estado próprio por referências/dimensões para não exibir uma imagem antiga quando as novas falham.

## Executar

Preparar-Filmes-Nomes-1.06.ps1 -Projeto CAMINHO -CapturarLog -Emulador emulator-5554

Compila cinco APKs e, com captura, instala por cima somente no emulador e coleta diagnostico-filmes-series-artes-logcat.txt. Não desinstala, exclui listas ou limpa dados. Não é necessário recolocar chaves: usa TMDB_READ_TOKEN e FANART_API_KEY já existentes no local.properties. Sem Fanart configurado, mantém TMDB e dados do provedor. O compartilhamento permanece ativo.

Pacote cumulativo: 36 entradas (27 arquivos e nove exclusões antigas). Sobre a entrega anterior: sete arquivos novos/alterados do resolvedor, repositório, parser, modelo, navegação, tela e Fanart de filmes. Verificação integral antes da aplicação, backup, rollback, idempotência e rejeição de alterações desconhecidas. Reconhece LF/CRLF/BOM dos textos conhecidos. Preserva a configuração privada de assinatura e mantém versão 1.06/interno 8. Não publica versão no servidor.

## Validação

Testes controlados de identidade de filmes, traduções, original/inglês/pt-BR, ano/remakes, IDs Fanart, idioma dos logos e cancelamento; regressão de séries. Provas contra API TMDB real: O Padrinho identifica O Poderoso Chefão (238) em pt-BR, e A Armadilha permanece identificada como 283371 com logo e seis miniaturas. Teste do repositório de produção usa resposta Xtream controlada + TMDB real e valida M3U nativa com catálogo local simulado. Não usa a conta privada do provedor.

A chave Fanart não está configurada neste ambiente; sua integração de filmes foi testada com respostas controladas. O emulador do usuário ainda precisa confirmar as artes Fanart reais e apresentação visual. Compilação, lint e integridade dos APKs são verificados separadamente desses limites.
