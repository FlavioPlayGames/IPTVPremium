# Site D1 r31 — sem R2

Atualizacao para a pasta oficial C:\Users\flavi\AndroidStudioProjects\Site IPTV. Execute Atualizar-Publicar.ps1: aplica arquivos com hashes/backup, testa, cria copia dos fontes, prepara SOMENTE duas tabelas temporarias no D1 existente e publica no Worker existente. Nao ativa R2, nao cria outro banco nem troca secrets. Esta execucao faz migracao D1 remota aditiva e deploy.

Para aplicar sem publicar: Aplicar-D1.ps1. Nenhum fonte Android muda. ZIP manual ate 2 MiB e SRT ate 1 MiB. Depois da confirmacao de salvamento na TV o arquivo e removido do armazenamento ativo. Expira em 15 minutos; limpeza ocorre na proxima requisicao manual ou cron horario. Pequeno status retido por mais uma hora. D1/Workers continuam sujeitos as cotas existentes.

Publicacoes futuras: Publicar-Site.ps1 dentro da pasta oficial, sem dependencia do pacote Downloads. Ele inclui schema-manual.sql na copia dos fontes e reaplica a migracao idempotente. Nunca editar .worker-preview. Nao alterar APP_KEY.

Recuperacao local: Desfazer-D1.ps1 -Backup "pasta indicada". Nao faz deploy nem apaga tabelas e recusa sobrescrever edicoes posteriores. A versao anterior dependia de R2; nao publicar a reversao sem revisar essa dependencia.

Testes locais: servidor com SQLite real, armazenamento transacional e navegador com APIs simuladas. Cloudflare real e TV fisica dependem da execucao pelo usuario. Consulte arquivos/validacao/VALIDACAO-R31.md.
