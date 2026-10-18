# Artes de séries: identificação e consultas repetidas — 1.06 / interno 8

O log real enviado pelo usuário confirma respostas HTTP 200 do TMDB, TVmaze e Fanart.tv, com alternativas fornecendo até 49 miniaturas e dois logos. Não é uma falha geral de chave. A Armadilha continua sem identificação na abertura registrada. HTTP 200 não garante correspondência da obra nem existência do logo.

## Ajustes

- Importação de séries aceita tmdb_id e tmdb, numéricos positivos, sem mudar o series_id do provedor. tmdb_id válido tem prioridade. Campos nulos, booleanos, negativos, URLs e valores fora do limite não viram ID. Logos explícitos do painel usam o mesmo extrator já existente para filmes.
- TMDB usa IMDb para localizar a série quando não há um ID TMDB utilizável. Normaliza marcadores de qualidade/idioma e ano no título. Não inventa ID, não troca série por nome parecido e não escreve esses resultados no histórico compartilhado.
- Consultas idênticas simultâneas do cliente TMDB compartilham o resultado. Cache de respostas em memória: dez minutos, até 64 entradas/8 MiB, isolado por instância de cliente. Candidatas de logo em português continuam prioritárias.
- As alternativas buscam números reais da temporada atual em um conjunto estável. Falhas sucessivas dos cards não reiniciam a busca a cada novo número ausente. Resultados complementares posteriores preservam logos/fotos já obtidos nessa temporada.
- Respostas 404 das alternativas ficam em memória por cinco minutos, sem repetir a mesma consulta a cada mudança de card. Não guarda 401/403/429/5xx como ausência.
- A busca não consulta o título provisório Série antes de existirem metadados/episódios. Logs agora distinguem presença de IDs e término da busca versus prazo esgotado, sem expor URLs, chaves ou credenciais.

## Aplicação e teste real

Preparar-Series-Artes-Rapidas-1.06.ps1 -Projeto CAMINHO -CapturarLog -Emulador emulator-5554

Instala por cima exclusivamente no emulador com -CapturarLog. Atualize a lista pelo aplicativo depois de instalar: a correção de importação só preenche IDs/logos na próxima sincronização; não atualiza retroativamente a geração já salva. Depois abra A Armadilha pela categoria/busca e aguarde as artes. Envie diagnostico-series-artes-rapidas-logcat.txt.

Não exclua listas nem limpe dados. Favoritos, progresso, chaves locais, temas e assinatura são preservados. Compartilhamento ativo por padrão. Cache de episódios continua separado por lista; não altera sua consulta nem player. Imagem da lista, TMDB, alternativas e fundo substituto mantêm sua prioridade. Extrator de vídeo continua removido. Janela de entrada de dois a cinco segundos preservada; não promete prazo de resposta dos serviços.

TVmaze não exige chave. A chave FANART_API_KEY do local.properties é preservada; o log do usuário já confirma respostas do Fanart. Nenhuma chave é embutida no pacote. O cache de JSON das alternativas permanece no cacheDir, com seis horas/48 arquivos/12 MiB. Não foi incluída nova fonte paga.

## Pacote e limites

Cumulativo com 31 entradas, 22 arquivos e nove exclusões antigas. Sobre a entrega anterior, altera quatro fontes: cliente TMDB, cache HTTP complementar, tela de detalhes e parser Xtream. Verificação integral de hashes, backup, rollback, idempotência e rejeição de fontes desconhecidos. Versão 1.06/interno 8; sem publicação no servidor de atualização.

Compilação Android, lint e testes controlados de coalescência/cache/cancelamento e importação. Não há emulador neste ambiente. O logo específico de A Armadilha e o ganho na lista real ainda precisam ser confirmados no aparelho; as fontes podem não oferecer a arte para determinada produção.
