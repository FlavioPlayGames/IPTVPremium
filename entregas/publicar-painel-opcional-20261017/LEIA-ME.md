# Implantar o painel opcional no Cloudflare

Publicar-painel-opcional.ps1 realiza a implantação, autorizada pelo usuário, usando a sessão Cloudflare do computador.
Requer Windows PowerShell, Node.js 22 ou superior e acesso à mesma conta já usada para o Worker central-iptv-premium.

Cria uma pasta nova na Área de Trabalho, baixa o pacote fixado e verifica SHA-256 e os arquivos do manifesto.
Instala Wrangler 4.151.1 conforme package.json, executa o dry-run e só então wrangler deploy.
Não substitui a pasta anterior, não cria banco, não executa migrations, não publica APKs nem grava metadados da versão 1.03 automaticamente.
Mantém as configurações confirmadas: DB, ASSETS, domínio centraliptvpremium.com.br, workers.dev e cron horário.
keep_vars preserva variáveis; os segredos remotos são mantidos. Não há valores de segredos neste pacote.

Ao terminar, atualizar o navegador com Ctrl+F5 e abrir Atualização opcional.
Cadastrar os links da v1.03, código interno 5, e publicar normalmente; não publicar e suspender depois.
Cada APK usa seu próprio link, tamanho e SHA-256 sem o prefixo sha256:.
A atualização opcional funciona no app 1.03 ou posterior; os antigos precisam instalar essa versão manualmente uma vez.
Os APKs continuam hospedados no GitHub; D1 guarda somente um registro de metadados de até 16 KiB.

Se ocorrer erro, enviar a saída completa. Não desinstalar o aplicativo, limpar dados nem criar outro banco.

Validação cloud: dry-run real reconheceu DB e ASSETS. Quatro cenários do comando passaram com npm/deploy simulados.
Não há sessão Cloudflare autenticada neste ambiente; a implantação real será executada no computador do usuário.
