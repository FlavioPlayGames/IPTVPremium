# Listas: recuperação e mensagens — 1.07

A mensagem do aplicativo identifica falha ao interpretar a autenticação Xtream. A imagem não contém a resposta HTTP: não permite afirmar se foi HTML, JSON truncado ou outra resposta temporária. O cadastro da lista é separado da geração de catálogo; na central a configuração pode estar salva/selecionada mesmo que a importação falhe. Selecionar novamente pode disparar uma nova importação, explicando o funcionamento posterior sem provar que a primeira terminou.

Site: envia Xtream apenas se a API confirmar a conta. Se a API não confirmar e a M3U validar, envia M3U. Se consultas do site forem inconclusivas por HTTP/rede/timeout, oferece M3U para conferência no aparelho com mensagem neutra. Entradas realmente inválidas continuam rejeitadas. player_api.php é convertido para o endpoint get.php no caminho M3U. A consulta inconclusiva nunca é marcada como verificada. Não envia listas automaticamente sem o clique em Enviar lista.

Detalhes técnicos da consulta inconclusiva ficam em diagnostics, removidos no servidor para sessões de cliente; só administrador recebe e visualiza. Erros persistentes do cliente mostram orientação simples. Mantidos autenticação, CSRF, escopo do aparelho, limite de consultas, proteção de destinos e limpeza de credenciais após recebimento.

App: autenticação Xtream inválida/incompleta tem uma única tentativa adicional após 300 ms, antes de escrever conteúdo do catálogo. JSON com espaços/BOM é aceito. Negativa explícita, conta expirada, falha de rede ou cancelamento não causam essa repetição. Corrige aceitação contraditória auth=0/status=Active e auth=1/status=Expired. Se persistir falha, mostra mensagem simples com Fechar; não finge sucesso nem apaga o cadastro. Mantém as gerações transacionais existentes. Não há conversão nova automática de M3U para Xtream no app.

Verificação: 30 testes do servidor, 10 verificações do navegador, nove testes JVM da autenticação e Wrangler dry-run passaram. App e APK de testes compilados. Não há emulador nesta nuvem e não foi reproduzida a resposta real do provedor: executar os nove testes Android com -TestarImportacao e conferir uma importação real após publicar o site.

Preparar-Listas-Recuperacao-1.07.ps1 aplica cinco arquivos, com hashes, backup e rollback de gravação; instala por cima sem limpar dados. Resultado diagnostico-listas-recuperacao.txt. Preserva Gzip e a velocidade aprovada da rolagem.

Publicar-Listas-Recuperacao-1.07.ps1 prepara a central e publica o Worker existente usando seu login Wrangler local; requer Node.js 22+. Sem migração de banco. -NaoPublicar apenas extrai para revisão. A publicação ao vivo não foi executada nesta nuvem.
