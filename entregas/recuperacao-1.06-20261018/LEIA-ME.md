# Recuperação automática de episódios — 1.06 / interno 8

Corrige uma interrupção no fluxo: uma falha com duração de 10 segundos ou mais encerrava a recuperação automática. A próxima consulta só começava pelo botão Tentar. Agora timeout e resposta inválida lenta também permitem recuperação automática. A tela continua carregando até o término das tentativas.

Política: até quatro tentativas em falhas transitórias, com pausas de 400 ms, 1,2 s e 3 s. JSON válido sem episódios recebe até três consultas. Duas falhas lentas encerram o ciclo; não inicia outra consulta quando o ciclo já atingiu 45 segundos incluindo a próxima pausa. Esse é um orçamento para INICIAR chamadas, não um timeout total: uma requisição em andamento ainda pode consumir o prazo HTTP existente. Não repete 401/403/404 ou cancelamento. Não promete resposta instantânea quando a rede falha.

Remove a tarefa adicional disparada no clique. A tela é dona da abertura; o aquecimento por foco existente continua sendo reaproveitado. Locks das séries passam a usar a chave completa, sem colisões entre títulos. São removidos quando não há usuários, inclusive em cancelamento. Mantém compartilhamento da consulta bem-sucedida entre foco e tela, cache LRU/TTL e limites de RAM. Se o aquecimento falhar, a tela ainda pode realizar sua própria tentativa de abertura.

Revisão focada nos caminhos de episódios: abertura normal/Favoritos/Continuar, pré-carga, cache, parser, consulta HTTP, migração de histórico e troca de episódio no player. Não é uma certificação de todas as funções do projeto. Preserva imagens/extração de frames do último patch, player, temas, animações, favoritos, progresso e listas. Sem alterações no Cloudflare.

Instalador exige fontes do último patch Miniaturas, confere todos antes de alterar, guarda backup e preserva assinatura/configuração local. Versão continua 1.06 / interno 8. -Verificar apenas confere; -NaoCompilar aplica sem compilar. Não instala nem publica APK. Instalar por cima; não limpar dados.

Validação: 19 casos de recuperação e 14 de foco/cache em JVM, incluindo timeout de 30 segundos seguido de sucesso, HTML lento, vazio temporário, recuperação na quarta chamada, limite de tentativas, cancelamento, HTTP permanente, colisão de hash e reutilização do cache. Compilação assembleDebug, lintDebug, cinco APKs com assinatura compatível com Android 9 e testes PowerShell do instalador. Rede/provedor e interface reais da TV não foram executados aqui. Confirmar abrindo a mesma série que exigia Tentar, sem apertar o botão.
