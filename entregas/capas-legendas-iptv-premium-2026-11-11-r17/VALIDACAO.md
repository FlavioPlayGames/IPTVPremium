# Validação r17

- Compilação Android final: `assembleDesempenho` e `lintDesempenho` concluídos em 6 min 21 s. Lint: 0 erros, 154 avisos e 6 sugestões. Os avisos existentes não foram objeto desta etapa.

- 37 verificações do servidor recebido, com SQLite e API simulados; sem alteração do site ou segredos em produção.
- 66 verificações do cliente/protocolo OpenSubtitles: identidade, ano contraditório, idiomas, episódio, autenticação, isolamento da sessão, download somente por escolha, cotas/cooldown, erros e logs sem credenciais.
- Teste do ramo real de recebimento da sessão do portal e teste da orquestração real das consultas, comprovando que OpenSubtitles inicia sem aguardar SubDL.
- Testes JVM de capas com fontes reais e adaptadores de rede/bitmap: preferência do provedor, identificação verificada, metadados de temporada, reaproveitamento de cache e vínculos ao item/lista; filas e reserva existentes preservadas.
- Método real `loadProvider`, extraído do fonte: cache sem rede, fonte saudável, cancelamento de I/O simulado em aproximadamente 1,6 s, pausa aplicada e cancelamento externo propagado sem falsa falha.
- Política de falha transitória: expiração, limite de registros, isolamento por URL, remoção no sucesso e exclusão de cancelamentos por prioridade. Prazo de 1,5 s cancelou uma operação simulada e seu observador de conexão; não equivale a uma medição de rede na TV.
- Aplicação/repetição/restauração/bloqueio de base divergente em PowerShell 7 e simulação da forma JSON do Windows PowerShell 5.1. Scripts analisados sintaticamente. Coletor aceita os campos novos e rejeita linhas com URLs/segredos/campos extras.

Nenhuma conta real foi usada neste ambiente. Não houve acesso à TV, teste real da busca OpenSubtitles ou medição de melhora das capas após a atualização. A confirmação depende do diagnóstico r17 no aparelho. Não há mudança de cota comercial/gratuita do serviço.
