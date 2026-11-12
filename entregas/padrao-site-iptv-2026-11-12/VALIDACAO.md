# Validação das rotinas

Testes passaram em PowerShell 7 e com simulação do formato de arrays JSON do Windows PowerShell 5.1. Não foi executado em Windows real.

Verificado: instalação, repetição, preservação por hash dos fontes e configuração originais, recusa de sobrescrever guia editado, caminhos com espaços, inventário, ZIP com fontes/configuração/public/_headers, exclusão de .env/.dev.vars/nomes privados conhecidos/dependências/gerados, criação de pacotes sem incluir pacotes anteriores, manutenção da referência da última verificação e detecção de fonte alterado após ela.

Sintaxe JavaScript executada com Node real; comandos npm simulados para testar o fluxo da rotina. A rotina bloqueou um npm check configurado diretamente como wrangler deploy sem dry-run. Sem deploy, conta Cloudflare, D1 remoto ou login de provedor.

Os testes do site atual continuam sendo responsabilidade dos fontes atuais. O teste de navegador opcional não é executado automaticamente por esta rotina. Nenhuma garantia de detecção de todo segredo escrito dentro de um código/configuração; nomes privados conhecidos são excluídos e o pacote fica local até o usuário compartilhar.
