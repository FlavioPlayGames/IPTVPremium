# Validação r19

- Fontes finais Android: `compileDesempenhoKotlin` e `lintDesempenho` concluídos em 5 min 46 s. Lint: 0 erros, 158 avisos e 6 sugestões. Há sugestões de modernização de APIs, incluindo nas partes novas; não foram tratadas como limpeza geral nesta etapa. O pacote distribui fontes e scripts, não um APK pré-assinado.

- 29 verificações de sincronismo/idiomas com o relógio, parser SRT/VTT e política de preferências reais: milissegundos preservados, sinal +8 s correto, limites, zerar, restauração da configuração, compartilhamento por temporada, individualidade dos filmes, isolamento entre obras/temporadas/perfis, preservação dos ajustes após mais de 128 novas configurações, identificação por hash e isolamento de IDs locais entre listas, aliases de idioma e prioridade SubDL → SubSource → OpenSubtitles. Preferências Android simuladas; sem teste visual no aparelho.
- 17 verificações do cliente SubSource com respostas simuladas: cabeçalho individual, ausência de download durante pesquisa, idiomas, episódio exato, rejeição de E060/E07, homônimos/ano/tipo/IMDb conflitantes, download explícito único, falha/escopo/sem conta e ausência de chave/URL/título no log. Não houve uso de conta ou serviço real.
- 44 verificações de servidor com SQLite e provedor simulados: sessão, CSRF, TV vinculada, criptografia, impedimento de recebimento em outra TV, ACK, remoção do payload, capacidades/status e formato da chave SubSource. Sem escrita no site real.
- 105 verificações de regressão do cliente OpenSubtitles, incluindo redirecionamento seguro da r18, conta/escopo, identificação, idioma e download; teste do ramo real de recebimento da sessão do portal.
- 40 verificações Chromium com API simulada, em 390 × 844 e 1440 × 1000: três serviços, envio e limpeza de campos, confirmação após ACK simulado, estilos, responsividade, mensagens de indisponibilidade e ausência de credenciais no armazenamento do navegador.
- Aplicador Android: aplicação/repetição/restauração/arquivos novos/guardas de base e hash; PowerShell 7 e simulação da forma de JSON do Windows PowerShell 5.1. Scripts analisados sintaticamente e coletor testado para aceitar somente os campos previstos.
- Aplicador do site: patch exato, repetição, restauração, bloqueio de fonte divergente e preservação de .env/wrangler.jsonc; PowerShell 7 e simulação JSON do Windows PowerShell 5.1. Não é uma execução integral no Windows 5.1.

## Limites da validação

Não houve instalação/acesso à TV nem publicação do Worker. O parser preserva tempos do arquivo; os testes não comprovam alinhamento com a fala do episódio real. ExoPlayer/VLC usam a mesma camada externa, mas ambos exigem teste visual da barra compacta, ausência de sobreposição com a legenda, DPAD, pausa/seek, troca de arquivo, zerar e reabrir no aparelho. Não há sincronização automática pelo áudio.

A rede deste ambiente bloqueou os domínios SubSource. A implementação API v1 foi conferida por referência técnica nos projetos públicos nRafinia/Emby.Subtitle.SubSource (8cf4c2f, 23/11/2025) e saeedrss/subsourceCLI (52ba907, 08/09/2026). Nenhum código com credencial desses projetos foi utilizado ou distribuído. Isso não substitui documentação atual/validação oficial com chave pessoal. Formato atual, acesso, pesquisa/download e cotas precisam ser confirmados na TV. Não se promete 20 downloads gratuitos para SubSource.

Os limites de imagens/cache/rotatividade, lote de importação, histórico, versão e assinatura foram preservados. O armazenamento total de 400 MB não foi novamente medido nesta etapa.
