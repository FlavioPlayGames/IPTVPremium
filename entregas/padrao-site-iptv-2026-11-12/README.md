# Padrão permanente de manutenção do site IPTV Premium

Instala somente SITE-LEIA-PRIMEIRO.md, manutencao-iptv/Manter-Site.ps1 e manutencao-iptv/.gitignore na pasta atual. Não contém cópias antigas de Worker/public, não substitui fontes/configuração, não faz deploy, não executa migrações e não configura Git automaticamente.

Pasta padrão: C:\Users\flavi\Downloads\site-identidade-aprovada. Executar Padronizar-Site.ps1 -Projeto "$env:USERPROFILE\Downloads\site-identidade-aprovada". A instalação verifica hashes e recusa sobrescrever rotinas/guias existentes que tenham sido modificados. É repetível quando os arquivos são idênticos.

Depois, usar o Manter-Site.ps1 dentro do projeto com -Acao Inventario, Verificar ou Pacote. Os relatórios e ZIPs ficam em manutencao-iptv, separados dos assets públicos e ignorados pelo .gitignore dessa pasta. O comando Pacote é o meio padrão para enviar os fontes atuais para outro chat sem node_modules/gerados. Não confundir pacote criado com teste aprovado.

As cópias geradas são locais. Para histórico fora do PC, manter os fontes atuais em controle de versão/backup externo revisado; esta instalação não envia automaticamente os fontes para o GitHub. O GitHub desta entrega preserva o guia e as rotinas, não uma cópia do site avançado do outro chat. Credenciais escritas diretamente em código/configuração exigem revisão antes de compartilhar pacotes.

Para retirar apenas estas rotinas, guardar eventuais relatórios/pacotes desejados e remover SITE-LEIA-PRIMEIRO.md e manutencao-iptv. Nenhum fonte ou configuração original é modificado pelo instalador.
