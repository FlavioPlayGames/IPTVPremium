# Site oficial r30

Projeto completo reconstruído para C:\Users\flavi\AndroidStudioProjects\Site IPTV.

Execute Instalar-Site.ps1. Instala somente fontes verificados, recusa sobrescrever arquivos divergentes e preserva a antiga pasta Downloads/site-identidade-aprovada. Nenhum deploy durante instalação.

Depois, na pasta oficial, execute Testar-Site.ps1. Publicar-Site.ps1 testa, guarda cópia dos fontes, prepara R2 privado e publica no Worker existente usando o login Cloudflare do PC. Mantém secrets e DB existentes. Não trocar APP_KEY.

SITE-LEIA-PRIMEIRO.md na pasta instalada contém a rotina permanente. Para enviar o projeto atualizado: manutencao-iptv/Manter-Site.ps1 -Acao Pacote. Não compartilhar credenciais.

Consulte site/validacao/VALIDACAO-R30.md. Testes locais passaram; produção/TV ainda dependem da execução no PC. A rotina de instalação foi testada em PowerShell 7 Linux; Windows PowerShell 5.1 deve ser confirmado no PC.
