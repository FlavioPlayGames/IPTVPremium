# Validação r18

- Android: `assembleDesempenho` e `lintDesempenho` concluídos em 6 min 54 s. Lint: 0 erros, 154 avisos e 6 sugestões. Avisos existentes não foram objeto desta etapa.

- 37 verificações do servidor recebido com SQLite/provedor simulados; sem escrita no site real.
- 105 verificações do cliente/protocolo OpenSubtitles, incluindo 301/302/307/308, destino relativo, preservação de parâmetros, codificação/ordem equivalentes, separação correta de parâmetros codificados, host VIP, rejeição de terceiros/HTTP/userinfo/porta/fragmento/outro endpoint/outra consulta, ciclo, limite de três GET e ausência de repetição do POST de download.
- Regressões de autenticação, isolamento de conta, identificação por ID/nome/episódio, idiomas, cache/cota, credenciais ausentes nos logs e sessão recebida pelo portal.
- Orquestração real de consultas confirma início do OpenSubtitles sem aguardar SubDL.
- Aplicador: aplicação repetida/restauração/guardas de base e hash em PowerShell 7 e simulação do formato JSON do Windows PowerShell 5.1. Scripts analisados sintaticamente. Coletor aceita somente os campos previstos e rejeita dados extras/segredos.
- Apenas dois fontes Kotlin alterados sobre a r17. Código das capas, memória, cache, histórico, importação e armazenamento preservado byte a byte.

Os testes usam respostas simuladas, sem chave/conta real, sem acesso à TV e sem confirmação do destino do 301 observado. A compatibilidade real do redirecionamento e a existência/download das legendas devem ser confirmados no aparelho.
