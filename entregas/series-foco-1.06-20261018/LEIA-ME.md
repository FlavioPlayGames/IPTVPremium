# Integração do patch de séries por foco — 1.06 / interno 8

Base: series-fix.zip e series-episodes.patch fornecidos pelo usuário, comparados com java.zip. Os 129 fontes Kotlin encontrados no java.zip correspondem à versão anterior (conteúdo/linhas). Os quatro arquivos do patch foram integrados sobre essa base; não substitui outras correções por uma versão antiga.

Proposta preservada: a série em foco por 450 ms prepara os episódios em segundo plano; até duas consultas de aquecimento ao mesmo tempo. HTTP 408/429/5xx e falhas de IO rápidas recebem até três tentativas (400/1200 ms); HTML/JSON inválido rápido também, JSON válido vazio só duas. HTTP 401/403/404 não repete. Falha ou resposta inválida com duração >=10 s não recebe outra espera. Cancelamento permanece cancelamento. Cache de até 40 séries, 3.000 episódios por série e 8.000 episódios no total.

Ajustes da revisão: controle testável SeriesFocusPreloader cancela foco antigo, perda de foco, troca de filtro/categoria e saída da grade. O clique preserva a consulta já iniciada para o mesmo título. Inclui a grade de Continuar assistindo. Remove somente o prazo independente de 12 segundos do aquecimento de séries, que podia cancelar a consulta esperada pelo clique e forçar outra; permanecem o cancelamento pelo dono, duas tarefas de fundo, os timeouts HTTP e o limite de tentativas. O clique e o aquecimento compartilham o cache/lock já existente. Clique em outra série não espera a fila de aquecimento de outros títulos (exceto eventual colisão nos locks fixos já existentes).

RAM: mantém os limites em episódios da proposta e acrescenta estimativa máxima de 16 MiB de texto/objetos para o cache de séries. É uma estimativa conservadora, não medição do heap. Entrada individual maior não expulsa todas as séries pequenas. Não guarda vídeos ou bitmaps neste cache e não aumenta o cache de imagens, banco ou armazenamento Cloudflare.

Mantém temporadas com episódios reais, imagens existentes, extração de miniaturas somente em falta, favoritos/progresso entre listas, nomes alternativos, animações/temas e Android 9. Sem promessa de primeira abertura instantânea: o resultado no aparelho e na API real ainda precisa de teste. No celular por toque, sem foco prévio, a consulta iniciada no clique permanece.

Preparar-Series-Foco-1.06.ps1 verifica os cinco fontes, aceita a base enviada em java.zip e os quatro arquivos da proposta original, faz backup e compila com a assinatura local. Mantém 1.06 / código 8. -Verificar só confere; -NaoCompilar aplica sem compilar. Arquivos desconhecidos interrompem antes de sobrescrever. Não instala nem publica APK.

Validação: testes JVM do repositório real com respostas simuladas, foco com relógio virtual, cache real (incluindo clique compartilhando consulta de mais de 12 s, séries de 3.000 episódios e estimativa de RAM), compilação, lint e instalador. APKs conferidos para Android 9. Sem execução de UI em TV neste ambiente.

Teste: instalar por cima sem limpar dados; deixar foco numa série por pelo menos 450 ms, abrir, voltar e reabrir; navegar rapidamente por outros cards; conferir Favoritos e Continuar assistindo. Se persistir lentidão, registrar SeriesDetails (duracaoMs/quantidade) e PlaylistConnection (HTTP), sem URLs/credenciais.
