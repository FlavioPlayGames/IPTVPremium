# Extrair respostas SubDL e OpenSubtitles

Comparar-Provedores.ps1 consulta The Handmaid's Tale, Fear the Walking Dead, The Good Doctor, House e Teen Wolf por TMDB, padrão temporada 1 episódio 3 (parâmetros -Temporada e -Episodio disponíveis).

SubDL: mesmo endpoint v2/autenticação Bearer do aplicativo, até 30 pacotes, unpack=1. Sem filtro explícito de idioma para observar os valores devolvidos; se a API aplicar um padrão, a amostra não representa todo o catálogo. OpenSubtitles: chave técnica e login pessoal, até cinco páginas de pesquisa por série, sem downloads de legenda. Não é uma varredura completa de todos os episódios/temporadas nem garantia de catálogo completo.

Prompts ocultos para chaves e senha. Enter pula um provedor. Requer a chave pessoal SubDL; OpenSubtitles requer a chave técnica do consumer do aplicativo, User-Agent e login pessoal. Não enviar esses valores no chat. Não lê segredos do Cloudflare nem altera APK, site ou configurações da TV. O login do OpenSubtitles é uma chamada real e a pesquisa utiliza a cota de requisições; nenhum endpoint de download é chamado.

O script exporta apenas campos selecionados da resposta. Não salva token, chave, senha, cookies, corpo bruto nem URLs de download. Redirecionamentos automáticos bloqueados; se houver HTTP 301, analisar antes de mudar a rota. O relatório fica em Downloads/IPTV-respostas-provedores.txt. Pode conter códigos/nomes de idioma e IDs públicos das obras.

Validação local: parser PowerShell e execução com respostas simuladas de ambos os provedores passaram, incluindo contagem de requisições e ausência dos valores privados simulados no relatório. Não executado contra contas reais. Disponibilidade e contrato do endpoint precisam ser confirmados com a coleta local.
