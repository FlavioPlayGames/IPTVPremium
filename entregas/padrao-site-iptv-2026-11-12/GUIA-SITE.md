# Projeto principal do site IPTV Premium

Pasta oficial no PC: `C:\Users\flavi\Downloads\site-identidade-aprovada`.

Esta pasta deve conter os fontes ATUAIS. Não substituí-los por pacotes antigos de outro chat. ZIPs e .worker-preview são entregas/artefatos, não a fonte principal. O guia registra o procedimento, não comprova que o conteúdo seja a versão publicada.

## Organização

- `wrangler.jsonc` (ou `.toml`/`.json`): configuração ativa. `wrangler.antes-publicacao.jsonc` é referência, não configuração ativa.
- `package.json` e `package-lock.json`: comandos e dependências versionadas.
- `worker-atualizacao-abi.js`, `opensubtitles.js`, `playlist-validation.js`: entradas atuais conhecidas; a configuração pode mudar em versões futuras, conferir o `main` ativo.
- `public/`: páginas, scripts, estilos, fontes, imagens e outros assets públicos.
- `src/`: módulos adicionais, se houver.
- `validacao/`: testes mantidos de acordo com o comportamento atual.
- `manutencao-iptv/Manter-Site.ps1`: inventário, verificação e pacote de revisão.
- `manutencao-iptv/relatorios/`: inventário atual, resultados locais e referência do último pacote. `ultima-verificacao.json` guarda o resultado anterior; o inventário informa se ele ainda corresponde aos hashes dos fontes atuais.
- `manutencao-iptv/pacotes/`: ZIPs dos fontes para revisão. Não são enviados automaticamente a serviço algum.

Arquivos privados permanecem apenas no local seguro já adotado: `.env`, `.dev.vars`, chaves e senhas não devem ser enviados para chats ou GitHub. O pacote exclui nomes privados conhecidos; isso não identifica toda credencial eventualmente escrita dentro de um fonte/configuração. Revise antes de compartilhar. Logs de testes não entram automaticamente no pacote.

## Rotina de trabalho

1. Fazer toda edição sobre esta pasta atual, com controle de versão/backup antes das mudanças. As rotinas fornecidas não substituem os fontes nem configuram Git automaticamente.
2. Manter os testes acompanhando o comportamento aprovado; não alterar a interface apenas para satisfazer teste antigo.
3. Após mudanças, executar Verificar. Requer Node.js 22 recente ou superior e dependências locais instaladas. Se necessário, usar `npm ci` na raiz para reproduzir package-lock.json. A rotina não instala dependências automaticamente.
4. Executar os testes de navegador separadamente quando houver alterações visuais/de fluxo. O comando padrão conhecido é `node validacao/browser.test.mjs` e requer Playwright/Chromium preparados. Não registrar como passado um teste que não foi executado.
5. Atualizar README, VALIDACAO e checksums da entrega final. O inventário da manutenção contém os hashes dos fontes atuais; não é o mesmo que o SHA256SUMS de uma entrega antiga.
6. Para outro chat, enviar o pacote gerado dos fontes ATUAIS e indicar este guia. Não reenviar node_modules ou a pasta inteira de vários GB.

## Comandos

```powershell
$site = "$env:USERPROFILE\Downloads\site-identidade-aprovada"
powershell -ExecutionPolicy Bypass -File "$site\manutencao-iptv\Manter-Site.ps1" -Acao Inventario
powershell -ExecutionPolicy Bypass -File "$site\manutencao-iptv\Manter-Site.ps1" -Acao Verificar
powershell -ExecutionPolicy Bypass -File "$site\manutencao-iptv\Manter-Site.ps1" -Acao Pacote
```

Verificar executa sintaxe JavaScript e os scripts npm test/check existentes, recusando comandos diretos de deploy real ou D1 remoto identificados nesses scripts. O check atual faz `wrangler deploy --dry-run`. O código dos próprios testes continua sendo responsabilidade do projeto. Confira o log e os códigos de saída. Inventario e Pacote não executam testes. Um pacote criado não significa que o site passou na validação.

Pacote reúne fontes da raiz, public, src, testes e estas rotinas. Exclui dependências, gerados, relatórios, pacotes anteriores e nomes privados conhecidos. Limite conservador de 30 MiB de fontes antes da compressão. O relatório de inventário permite identificar arquivos grandes. Não altera o cadastro de listas/TVs nem faz deploy.

## Cloudflare

Última configuração conferida: Worker `central-iptv-premium`, assets `public` via `ASSETS`, banco existente via `DB`, `keep_vars: true`. Conferir a configuração ATUAL antes de publicar. Não recriar banco, não trocar APP_KEY e não substituir secrets existentes. A APP_KEY existente é necessária para dados criptografados.

Secrets OpenSubtitles já configurados no histórico deste projeto: `OPENSUBTITLES_API_KEY` e `OPENSUBTITLES_USER_AGENT`. Não são recriados em cada deploy.

Depois de validar e conferir a conta/destino:

```powershell
Push-Location "$env:USERPROFILE\Downloads\site-identidade-aprovada"
try {
    npx wrangler whoami
    npx wrangler deploy
}
finally { Pop-Location }
```

GitHub e Cloudflare são etapas distintas: push de pacote/fonte não faz deploy automaticamente neste fluxo. Restaurar fontes não restaura D1. Depois da publicação, validar no site/TV real; mocks não comprovam integração completa.

## Estado da última conferência recebida

Na base enviada em 12/11/2026: 24 testes de servidor e sintaxe passaram; o teste de navegador parou em `#validate-list` ausente, pois o formulário atual envia por `#send-list`. README e checksums estavam desatualizados. Essa observação é histórica: o outro chat já pode ter corrigido a base. Executar as verificações atuais, sem presumir que esta anotação descreva uma publicação posterior.
