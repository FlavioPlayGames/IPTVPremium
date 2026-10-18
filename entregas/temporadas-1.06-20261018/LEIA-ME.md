# Abertura direta de séries — 1.06 / número interno 8

A ficha resolve a série diretamente por ID na lista ativa, sem depender de uma busca ou da página visível do catálogo. A consulta de temporadas observa a geração ACTIVE do Room e reinicia quando ela muda. O cache guarda apenas respostas com episódios; respostas vazias não impedem uma consulta posterior.

Não altera favoritos, progresso, assinatura, temas, animações ou formato do banco. Não baixa o catálogo completo para abrir uma série. Mantém o reaproveitamento dos episódios completos no retorno do player.

Preparar-Temporadas-1.06.ps1 faz backup dos três fontes e compila com a assinatura local. Requer o patch anterior Preparar-Vinculos-1.06. Mantém 1.06 / código 8. Não instala nem publica APK. -Verificar somente confere; -NaoCompilar aplica sem compilar. Fonte diferente do esperado interrompe antes de sobrescrever.

Validação: testes do cache real em JVM com respostas simuladas (abertura fria, resposta vazia seguida de resposta completa, duas listas com mesmo ID, nova geração, chamadas simultâneas, cancelamento e limpeza), compilação e lint. Sem teste de interface em aparelho neste ambiente. Confirmar com o provedor real: fechar e abrir o aplicativo, entrar diretamente em Favoritos e Continuar assistindo e abrir uma série sem buscá-la antes. Instale por cima, sem desinstalar nem limpar dados.
