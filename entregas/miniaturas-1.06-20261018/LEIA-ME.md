# Miniaturas dos episódios — 1.06 / interno 8

Mantém fotos específicas do TMDB/provedor. Sem foto, tenta extrair um frame do próprio episódio. Não usa capa/backdrop genérico da série para substituir a imagem do card. O parser agora ignora campos de imagem inválidos como 0/[] e continua procurando uma URL válida, além de aceitar episode_image, thumbnail, thumbnail_url, image e stream_icon. Preserva a ordem das fotos válidas existentes.

Frames já salvos são lidos antes de repetir tentativas de URLs indisponíveis, sem extrair novamente. Uma foto específica que chegar depois continua prioritária. Extração nova só entra após verificar as imagens do episódio. O intervalo de falha caiu de uma hora para dois minutos por sessão, permitindo tentar de novo ao reabrir depois desse prazo. Não faz tentativas contínuas na tela.

O leitor de vídeo aceita HTTP 206 por faixas e HTTP 200 em prefixo progressivo, dentro do mesmo orçamento de até 6 MiB de dados lidos por tentativa e prazo de 10 segundos (leitura HTTP em andamento pode terminar no timeout da conexão). No modo progressivo, mantém um prefixo limitado em RAM e permite retroceder dentro dele. Não percorre toda a cauda de arquivos grandes. Formatos que exigem dados além desse prefixo ou codecs sem suporte ainda podem não gerar imagem. O extrator tenta 1 s, 0 s e 5 s do mesmo vídeo; um instante que falhar não impede os próximos. Um extrator por vez, fora da UI.

Não guarda vídeos em disco. JPEGs ficam no mesmo cache compartilhado de imagens de 64 MiB. Não amplia armazenamento Cloudflare nem altera player, episódios, temporadas, favoritos/progresso, temas, animações ou aquecimento por foco. Preserva versão 1.06 / interno 8.

Preparar-Miniaturas-1.06.ps1 exige a base do último patch Series-Foco, valida seis fontes, faz backup e compila com a assinatura local. -Verificar só confere; -NaoCompilar aplica sem compilar. Arquivos desconhecidos interrompem antes de sobrescrever. Não instala nem publica APK. Instalar por cima sem limpar dados.

Validação: testes JVM do parser e do fluxo de escolha das imagens com respostas simuladas; leitor real contra servidor HTTP local com/sem Range, limite de prefixo e cancelamento; regressão de foco/retry/cache; compilação, lint e instalador. Esses testes não executam MediaMetadataRetriever em uma TV nem comprovam extração nos streams reais do cliente. Confirmar os episódios 3 e 4 de NCIS Origins no aparelho.

Diagnóstico seguro: tag EpisodePreview informa frame_extraido, frame_ausente, url_episodio_indisponivel ou classe da falha de extração, sem URL, usuário ou senha. Se ainda não houver foto, esse registro é necessário para investigar o dispositivo/stream em vez de afirmar sucesso apenas pela compilação.
