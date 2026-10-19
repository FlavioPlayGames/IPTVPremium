# Aproveitamento da conexão — capas — 1.07

Aumenta de três para seis downloads simultâneos de capas, compartilhados por filmes e séries. Antecipação usa no máximo duas dessas conexões, deixando capacidade para até quatro solicitações visíveis mesmo com antecipação em andamento; sem antecipação, até seis visíveis. Limite não se aplica a reprodução de vídeo ou API de catálogo.

O lote antecipado de até 15 capas passa a guardar somente o arquivo comprimido em disco, sem decodificar imagens ainda fora da tela. Decodificação permanece em duas tarefas; cache de bitmaps continua 20 MiB e orçamento de disco permanece o mesmo. Mantém bloqueio por URL para reaproveitar o mesmo download/cache e não baixa novamente arquivos disponíveis. Não aumenta buffer do player nem o limite de memória configurado. O total real de RAM precisa de medição no aparelho; não há garantia de atingir uma taxa de internet específica. A velocidade efetiva depende do servidor e da conexão.

Inclui a entrega anterior de retorno à última capa e rolagem em duas fileiras (8/10 capas), para manter compatibilidade com as telas. Mantém site, Fechar nos erros, histórico, favoritos e espera obrigatória dos filmes.

Sete testes JVM da classe de orçamento usada no app verificam limite seis, reserva de duas antecipações, liberação por erro e cancelamento, sem bloquear novas solicitações. Não medem Mbps do provedor. APK compilado; sem teste de velocidade no dispositivo na nuvem. Instalador com hashes, backup e rollback, preservando configurações locais. Instala por cima sem limpar dados. Opção anterior -TestarNavegacao continua disponível para os dez testes de retorno/rolagem no emulador, sem necessidade de repeti-los para essa mudança de rede.
