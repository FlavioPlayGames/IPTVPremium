# Validação da entrega r29

- Base: Worker modular enviado em worker-atualizacao-abi.zip, SHA256 7c7d33be76f1c1bc3a2ccb84cf1be2b08d28d0f1c1a63d459f2b236e0cdb1628, módulos/configuração/public/testes enviados nesta conversa. O bundle de .worker-preview não foi usado como fonte.
- Java recebido: 326 arquivos, preservados isoladamente. Fundo r28 estático e classes de envio manual presentes. Sem edição ou nova compilação Android reivindicada nesta entrega.
- npm test: 24 testes do servidor original passaram e os cenários do envio manual passaram no Worker modular atual, com DB/R2 simulados: autenticação/CSRF, isolamento entre TVs, QR, hash, upload/download, confirmação/rejeição, exclusão, substituição, cancelamento, concorrência, expiração e limite de 8 MiB.
- Navegador: 224 verificações Chromium passaram em 360, 390, 768 e 1440 px, com API/TV simuladas. Fluxo atual de envio direto, criação/edição, erros preservando campos, cliques repetidos, formato preparado M3U, M3U direta administrativa, TV correta após validação atrasada, SubSource no administrador, upload manual e confirmação posterior. Nenhum login/serviço real.
- npm run check: PASS. Sete verificações de sintaxe e Wrangler 4.151.1 dry-run. Binding R2 reconhecido, sem criar bucket ou publicar.
- Testar-Site.ps1: executado em PowerShell 7 com ponte de npm.cmd para npm no Linux; servidor/dry-run reais passaram, incluindo avisos nativos sem falso erro. Não executado em Windows real.
- Aplicar/Desfazer: passaram sobre fixtures completos, incluindo reaplicação, ida/volta, novos arquivos, bloqueio de edição posterior, Worker ausente ou divergente sem escrita. Arquivo privado de teste, módulos de provedores, DB, ASSETS, domínio e cron preservados. PowerShell 7 e simulação de formato JSON WinPS5.1.
- Preparar-R2: CLI simulada passou para bucket novo, existente, regra diferente e falha de CLI; flags/formato conferidos no Wrangler fixado. Sem operação real na conta.
- Parser de todos os scripts PowerShell: PASS.

Não comprovado: qual versão está hoje publicada, existência/privacidade do bucket real, dados do D1 real e integração celular → site → TV → player. O usuário deve executar os passos locais e validar a TV. Aplicação de patch, dry-run, publicação GitHub e deploy são etapas separadas.
