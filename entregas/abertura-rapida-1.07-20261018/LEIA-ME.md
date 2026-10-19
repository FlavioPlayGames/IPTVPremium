# Abertura e troca de listas — 1.07 / interno 9

O vídeo enviado mostra aproximadamente 88 segundos ainda na abertura. O código mantinha essa tela durante a central de acesso e também aguardava resolução de nomes alternativos e episódios do histórico antes de liberar a Home. Essa segunda etapa não tem limite global: vários lotes de buscas poderiam acumular minutos.

Acesso: restaura o estado local da central em IO antes de esperar rede ou mutex de envio de comandos. Usa a mesma verificação existente de prazo, relógio e bloqueio. Central continua consultada, sem alterar a política de licença; aparelho ainda não registrado continua aguardando validação inicial. Não cria prazo extra nem libera acesso expirado/bloqueado.

Catálogo: Usar lista aproveita geração ACTIVE/READY salva, mesmo ao selecionar a lista atual. Só importa se não há catálogo; comandos de atualização recebidos da central mantêm a recarga. Atualizar na Home continua limpando cache e reiniciando, exigindo nova importação. A primeira importação ainda depende do tamanho da lista, rede, servidor e armazenamento; não prometemos menos de dez segundos sem medição real.

Histórico: tentativa local inicial tem orçamento de 1,5 segundo. Identificação remota e carregamento de episódios continuam em segundo plano na Home e são pausados ao abrir outra seção, dando prioridade à navegação. O histórico local continua reconciliado sem bloquear a interface. Adicionar/trocar lista pode mostrar aviso discreto Importando histórico, sem prender a abertura; abertura normal não mostra o aviso. Comparação conserva IDs, nomes originais/inglês/espanhol/português e episódio por temporada/número.

Evita consultar repetidamente a identidade da mesma série para cada episódio do histórico; só deduplica consultas, preserva todos os eventos de progresso. Reutiliza os vínculos quando geração e identidades não mudaram, atualizando os fatos de favoritos/progresso. Uma nova geração invalida essa reutilização.

Diagnóstico StartupPerf separa acesso local, rede da central, cache de catálogo e histórico local. -CapturarLog preserva os nove testes nativos anteriores e coleta esses tempos. Nuvem sem TV/emulador: testes de regras, instalador, compilação e lint; velocidade real de abertura deve ser medida no aparelho. Mesmos limites de memória, quatro/cinco capas, foco e correção para APK x86 único. Listas, chaves e histórico não são apagados.

Instalador cumulativo com hashes, backup, rollback e modo -Verificar. Instalar por cima.
