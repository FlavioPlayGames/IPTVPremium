# Validação da correção r38

- :app:assembleDesempenho aprovado: Kotlin, lint vital, R8 e APKs. Fontes do manifesto conferidos após compilação.
- 24 verificações em classes reais compiladas de produção. Context/SharedPreferences hospedados em memória no teste JVM; regras de progresso, gravação, projeção e mapeamento não substituídas por mocks. Vínculo entre listas injetado por reflexão para reproduzir o caso sem depender de rede.
- Filmes e episódios: salvamento/reabertura; duração curta suspeita não conclui; falta de duração preserva a conhecida; capa da temporada não altera progresso; 95% verdadeiro continua concluindo; retomar antes do fim reaparece; evento real de fim conclui; normal/Kids isolados; duração igual à posição sem evento de fim não conclui; duração inicial do catálogo protege contra amostra curta do decoder.
- Séries compartilhadas: resposta vazia durante carregamento não esconde; resposta válida usa ID real do episódio; resposta vazia posterior preserva mapa conhecido; outra lista não substitui vínculo; próximo episódio pendente permanece em progresso.
- Aplicar, reaplicar e desfazer byte a byte aprovados em PowerShell 7 e simulação da forma JSON do Windows PowerShell 5.1. Base divergente/edições posteriores bloqueadas. Scripts aprovados no parser. Windows e ADB físicos não executados aqui.
- Cinco fontes alterados/adicionados; treze guardas de dependências. Recursos originais comparados byte a byte: nenhuma mudança. Sem alteração de Gradle, chaves, dados do site, assinatura ou limites de memória/cache.

Não houve reprodução física na TV; a falha de série foi reproduzida e corrigida em teste determinístico local. A proteção de duração cobre inconsistências detectáveis, sem garantir que toda informação incorreta de um provedor seja identificável. Duração genuinamente muito menor pode manter o item até o fim real. Não reclassifica automaticamente históricos antigos concluídos. Nenhum dado de usuário é apagado.
