# Carregamento direto de séries — 1.06 / interno 8

Inicia a consulta de episódios no clique da série, antes da transição de navegação. Clique e destino compartilham a mesma consulta/cache para não duplicar pedidos. Prioriza episódios reais já importados no Room; só chama o provedor se não houver episódios locais. Cache completo em memória continua sendo reutilizado por playlist/geração/série.

Define o estado Carregando antes de qualquer espera: produceState conserva o estado anterior ao mudar suas chaves. Resposta vazia não vira uma página pronta. Exibe mensagem de falha sem afirmar que o provedor não disponibiliza temporadas; o botão passa a Tentar novamente e repete a carga sem sair/procurar a série no catálogo. Não inventa temporadas/episódios.

A imagem fornecida pelo provedor começa a carregar sem aguardar o TMDB. A prioridade da imagem TMDB, quando disponível, permanece. Extração de frame continua somente depois de todas as imagens normais falharem e da consulta de imagens terminar.

Mantém animação da TV, lotes de 15 capas, recuperação do player, cache limitado, favoritos/progresso e identificação por nomes alternativos. Não altera o catálogo nem a assinatura. Primeiro acesso sem cache ainda depende do tempo de resposta do provedor; não promete abertura instantânea nem mediu desempenho no aparelho do cliente. Registro seguro SeriesDetails informa apenas contagem e duração da consulta, sem IDs/URLs/credenciais.

Requer o patch Player-Series anterior da 1.06. Script faz backup e compila localmente, versão 1.06 / código 8, sem instalar/publicar APK. -Verificar só confere; -NaoCompilar aplica sem compilar; fonte desconhecido interrompe antes de sobrescrever. Instale por cima sem limpar dados.

Validação: testes JVM da política de carga real com local pronto/ausente/vazio, provedor vazio, cancelamento, imagem do provedor sem TMDB e prioridades/URLs. Compilação, lint, instalador e metadados dos APKs. Falta medir o tempo real na TV: clicar numa série ainda não aberta, confirmar Carregando sem mensagem falsa, abrir novamente e verificar cache; Tentar novamente não exige sair da ficha.
