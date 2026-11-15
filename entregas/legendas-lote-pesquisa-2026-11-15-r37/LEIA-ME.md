# Pesquisa manual, lote, arquivos salvos e teclado r37

Base obrigatória: r36 aplicada em C:\Users\flavi\AndroidStudioProjects\IPTVPremium.

## Instalação

Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium" aplica, compila no PC e instala na TV já conectada/autorizada. O script verifica os fontes antes de gravar e cria recuperação externa em Downloads, sem mudar o pareamento, assinatura ou dados do aplicativo. Aplicar.ps1 apenas aplica. Desfazer.ps1 -Backup "pasta indicada" -Instalar restaura os fontes protegendo edições posteriores; não recupera legendas que você venha a excluir pelo aplicativo.

## Pesquisa do envio manual

Usa o fundo estático original e os temas/modos já existentes. Busca e Limpar no topo; resultados à esquerda sem miniaturas, conforme a imagem marcada; painel Selecionado à direita com capa real, nome, ano e Continuar. A busca usa o catálogo da lista ativa com os mesmos filtros de acesso do fluxo existente.

A capa selecionada usa o carregador real do catálogo: RAM/disco, imagem da lista e alternativa verificada, quando disponível. Apenas a capa selecionada é carregada e a operação anterior é cancelada ao mudar de seleção. Não depende só do antigo peek em RAM. Enquanto busca mostra Carregando capa; sem fonte válida mostra Capa indisponível, sem fingir uma imagem encontrada. Falhas/ausência no provedor ou no TMDB ainda podem impedir uma capa real.

## Lote e várias opções por episódio

Conferência com Associar todas, Revisar pendências, Limpar seleção e Salvar. Todas associa os arquivos identificados, mantendo várias versões para o mesmo episódio; não inventa destinos para arquivos sem identificação. Pendentes/nomes ambíguos podem ser associados manualmente. A tela de associação mostra as opções já ligadas ao episódio, sem substituí-las. Limpar seleção só desmarca.

O armazenamento também aceita múltiplos arquivos por episódio e múltiplas versões para um filme. Nomes de versões distintos são preservados mesmo que o texto seja igual; repetições exatas do mesmo arquivo/destino não criam entradas repetidas. Subpastas de ZIP são lidas sem extração. O nome do arquivo tem prioridade sobre dicas da pasta; repetir S05E06 na pasta e no arquivo deixa de descartar a legenda. Identificação conflitante/múltipla vira pendência, em vez de ser descartada ou adivinhada. ZIP dentro de ZIP continua sem suporte.

## Arquivos salvos

Em Configurações → Legendas → Arquivos salvos: listar enviados manualmente e baixados; excluir um arquivo, selecionados ou tudo, sempre com confirmação. Se o arquivo for compartilhado, a confirmação explica que as versões vinculadas também perderão acesso a ele.

Exclusão limitada às legendas locais reconhecidas, sem remover contas, chaves, listas, histórico ou dados de outros caches. Limpa cópias antigas, RAM e referências; respeita arquivos em uso/reservas do orçamento de cache. Um arquivo em uso pode precisar de outra tentativa. As opções dos provedores continuam pesquisáveis: escolher novamente pode realizar um novo download e consumir a cota normal. Excluir uma legenda não apaga o arquivo no provedor/site nem garante interromper uma legenda já carregada na reprodução.

## Compartilhar versões da mesma obra

Arquivos manuais novos compartilham associação quando o ID TMDB da obra está confirmado, sem depender de HD, 4K ou HDR no nome. Episódios mantêm temporada/episódio e normal/Kids separados. Associações antigas podem receber a identidade confirmada ao reabrir o item original; arquivos existentes são preservados.

As associações de downloads dos provedores também passam a usar a obra confirmada, preservando a separação por conta e a leitura do índice anterior. Continua necessário respeitar a conta/sessão do provedor. Sem ID confirmado, mantém-se a associação individual; nome parecido não junta remakes/continuações. IDs já verificados no cache público de capas podem ser reaproveitados, sem consulta de identidade em massa.

Compartilhar o arquivo não garante sincronia entre edições diferentes: os ajustes existentes da r35 permanecem disponíveis e não foram recalculados automaticamente.

## Teclado

Fundo, letras, foco, bordas de destaque e cursor seguem os temas/modos existentes. Corrige o uso das cores antigas no teclado normal, preservando a paleta própria do Kids. Acentos, símbolos, Shift, colar/copiar, edição física, navegação e repetição de apagar mantêm o fluxo anterior.

## Limites e validação

Mantidos o orçamento de 5 MB de arquivos de legenda, seleção de até 4 MB por envio, limites do ZIP/SRT e o orçamento global/rotatividade. O site continua aceitando ZIP até 2 MB/SRT até 1 MB. Sem aumento de limites, nova imagem/biblioteca, mudança de Gradle ou alteração do site. Confira VALIDACAO.md. Não houve teste físico/captura real na TV neste ambiente; conferir pesquisa/capas, teclado, lote e exclusão após instalar.
