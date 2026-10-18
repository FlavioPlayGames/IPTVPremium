# Correção de consultas de séries — 1.06 / interno 8

## Evidência e correções

O log real do usuário mostra HTTP 200 em aproximadamente 0,5–0,9 s, enquanto algumas consultas completas levam 6–7 s. Quatro consultas mais as pausas anteriores de 0,4 + 1,2 + 3 s explicam aproximadamente esse intervalo. O log anterior misturava ações de filmes/séries; tamanho repetido não prova conteúdo idêntico nem identifica a série. Portanto, não afirmamos que a resposta específica de Marília/NCIS foi reproduzida sem ela.

Defeitos reproduzidos com o código anterior:

1. O parser aceitava mapa de temporadas e lista simples, mas ignorava arrays agrupados e objetos indexados que painéis PHP podem emitir. Uma resposta válida com episódio resultava em zero e novas consultas. Agora normaliza essas estruturas dentro de episodes, preserva IDs reais e temporadas explícitas. Limites: profundidade 6, 30 mil nós/chaves e 10 mil episódios. Não inventa ID a partir da chave nem cria temporadas vazias.
2. Quando foco e clique se sobrepunham e a consulta retornava null/erro, o segundo consumidor repetia toda a recuperação. Agora compartilham também o desfecho vazio/erro apenas enquanto essa consulta está em uso. Uma tentativa explícita posterior permanece livre para consultar novamente; cancelamento não é memorizado como falha.
3. Respostas com espaços/quebras de linha antes do BOM Unicode eram rejeitadas pelo org.json do Android. Agora remove somente esses caracteres iniciais antes de ler JSON. Não descarta HTML ou avisos arbitrários para forçar uma resposta válida.
4. O prazo especial de leitura de 8 s abortava uma resposta saudável de 9 s. Removido: volta ao prazo normal de conexão 15 s/leitura 30 s de inatividade, mantendo fechamento imediato ao cancelar. Aumentar o prazo não torna a rede rápida; evita cortar respostas válidas.
5. Uma recuperação automática curta, com no máximo duas chamadas e pausa de 400 ms, substitui até quatro chamadas/4,6 s de pausas. Erros 401/403/404 não são repetidos. 408/429/5xx, I/O e resposta vazia/inválida recebem uma recuperação. Cancelamento encerra imediatamente. Resposta que falha duas vezes ainda pode exigir Tentar: não há repetição infinita nem garantia contra erro real do servidor.

Diagnóstico SeriesDetails agora correlaciona a série por hash opaco e informa tentativa, tempo de rede, tempo de processamento, tipos do nó episodes e quantidade, sem corpo, títulos, URLs, usuário ou senha. PlaylistConnection distingue XTREAM_SERIES_INFO de XTREAM_VOD_INFO e carrega o mesmo hash. fluxo=series-r2 identifica esta revisão e mede também a abertura completa.

## Preservado da entrega anterior

Pacote cumulativo: cache persistente de detalhes por lista/geração (40 séries / 12 MiB / seis horas), antecipação no foco, prioridade da temporada em andamento, imagens só da temporada visível, extrator Media3 + alternativa MediaMetadataRetriever somente para cards sem referência, TV dinâmica e animação final aprovada. As fotos válidas continuam prioritárias. Fundos/temas não são sobrescritos. Favoritos, progresso, listas, assinatura/configuração privada e versão 1.06/interno 8 são preservados.

get_series_info fornece as temporadas juntas. Separar requisições por temporada repetiria o mesmo endpoint: a prioridade é abrir os dados recebidos e carregar imagens da temporada visível.

A correção de consultas não prova extração dos vídeos reais do provedor. Codec/DRM/Range/contêiner ainda podem impedir a miniatura. O teste -TestarFrames continua disponível para MP4, MKV e entrada animada (três provas locais). Não limpa dados nem reinstala do zero.

## Instalação e testes

Preparar-Series-Causa-1.06.ps1 aplica 18 arquivos conhecidos com preflight integral, backup, rollback e idempotência. Aceita a última entrega e o src.zip enviado, sem exigir aplicar patches intermediários. -Verificar confere sem alterar; -NaoCompilar aplica sem build. A execução padrão gera os cinco APKs para instalar por cima, mantendo a assinatura local.

-TestarSeries -Emulador emulator-5554 compila também os testes Android, instala por cima SOMENTE nesse emulador e verifica três regressões reais via HTTP local: resposta de 9 s, arrays PHP agrupados e compartilhamento de erro entre foco/clique. Salva diagnostico-series-emulador.txt. Não usa lista/credenciais do usuário nem servidor externo. -TestarFrames mantém o teste anterior; ambos podem ser usados juntos. Recusa telefone como destino dos testes e recusa testes com -NaoCompilar.

Validação cloud: testes JVM de parser, repetição, cancelamento, cache compartilhado e demais regressões relevantes; compilação assembleDebug + assembleDebugAndroidTest + lintDebug; conferência dos cinco APKs assinados/Android 9; instalador PowerShell testado para preflight, base enviada, idempotência e preservação de bytes privados.

Os testes Android foram compilados, mas não executados nesta máquina, que não possui emulador. A lista real precisa ser conferida no aparelho; não declaramos o caso Marília resolvido só porque compilou.

## Referência de desenvolvedores consultada

IPTVnator, revisão 5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0, fontes lidas via Git:

- Uma consulta getSeriesInfo por ID: https://github.com/4gray/iptvnator/blob/5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0/libs/portal/xtream/data-access/src/lib/services/xtream-api.service.ts#L303
- Compartilhamento de Promise em andamento, com remoção em finally, no catálogo: https://github.com/4gray/iptvnator/blob/5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0/libs/portal/xtream/data-access/src/lib/data-sources/electron-xtream-data-source.ts#L330
- Enriquecimento TMDB da temporada selecionada, idempotente por série/temporada: https://github.com/4gray/iptvnator/blob/5a94ca2c2ca82ad41cf5b6c57ade4b5ca23bc9b0/libs/portal/xtream/feature/src/lib/serial-details/serial-details-seasons.service.ts#L77

São referências de arquitetura. Não provam o formato da resposta privada do usuário nem substituem os testes deste projeto. Não executamos código externo nem adicionamos biblioteca de rede. O arquivo referencias-desenvolvedores.md explica o que foi observado e seus limites.
