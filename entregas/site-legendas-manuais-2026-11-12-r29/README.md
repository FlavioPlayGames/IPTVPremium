# Site — envio manual de legendas r29

Entrega somente do SITE. O Java atualizado recebido foi preservado isoladamente; o fundo estático r28 e as funções Android não são substituídos. O patch adiciona o envio manual ao Worker modular recebido anteriormente, mantendo SubSource e envio direto de listas.

## Aplicar e conferir

No PowerShell, após extrair este pacote em Downloads\site-legendas-manuais-r29:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\site-legendas-manuais-r29\Aplicar-Site.ps1" -Site "$env:USERPROFILE\Downloads\site-identidade-aprovada"
```

O aplicador valida a base e dependências antes de escrever, recusa Worker ausente/arquivos divergentes e cria backup externo em Downloads\IPTV-site-restauracao-r29-.... Não restaura preview/backups antigos automaticamente. A base é o último conjunto recebido nesta conversa; não presumimos que seja a versão publicada ou a pasta atual do PC. Se divergir, enviar os fontes atuais para adaptar, preservando os avanços.

A aplicação NÃO faz deploy, NÃO cria R2 e NÃO executa comandos no D1 remoto. O patch mantém APP_KEY/secrets, DB, ASSETS, domínio e cron; acrescenta somente o binding privado MANUAL_SUBTITLES à configuração existente.

## Preparar e publicar — executar só após aplicar sem erro

O passo Preparar-R2 realiza operações reais na conta Cloudflare usada pelo Wrangler: cria o bucket dedicado se faltar e configura expiração do prefixo manual/ em um dia. Reutiliza bucket/regra corretos e recusa uma regra com mesmo nome e configuração diferente. Não habilita acesso público nem altera D1. Se o bucket já existir, ele deve permanecer privado; a rotina não muda configurações de publicidade preexistentes.

```powershell
$site = "$env:USERPROFILE\Downloads\site-identidade-aprovada"
Push-Location $site
try {
    powershell -ExecutionPolicy Bypass -File .\Testar-Site.ps1
    if ($LASTEXITCODE -ne 0) { throw "A verificacao falhou. Nao publique." }

    powershell -ExecutionPolicy Bypass -File .\Preparar-R2.ps1
    if ($LASTEXITCODE -ne 0) { throw "O R2 nao foi confirmado. Nao publique." }

    npx wrangler deploy
    if ($LASTEXITCODE -ne 0) { throw "A publicacao falhou. Confira a saida." }
}
finally { Pop-Location }
```

Não cadastrar novamente secrets OpenSubtitles já existentes. Não trocar APP_KEY. Criar o bucket pelo painel também é possível: nome iptv-premium-manual-subtitles, privado, binding MANUAL_SUBTITLES e lifecycle manual/ após 1 dia. Preparar-R2.ps1 consulta whoami antes das ações; usar a conta correta do Worker/D1 existente.

## Testar o caminho completo

Na TV: Configurações → Legendas → Envio manual. Escolher filme ou série, temporada/episódio, gerar o QR e abrir pelo celular. Anexar SRT/ZIP sem renomear. Na TV, conferir as associações e salvar. O site só deve mostrar recebido depois de salvar; no player selecionar Legendas manuais. Conferir aparência e sincronização existentes. Em pacote de temporada, testar ao menos dois episódios.

Pedidos expiram em 15 minutos. SRT até 1 MiB e ZIP até 8 MiB; a seleção/extracão é validada pelo APK atualizado. O R2 guarda o upload temporário e o exclui após confirmação/rejeição/cancelamento; lifecycle cobre abandonados. Não há migração D1.

## Validação e limites

24 verificações do servidor existente e os fluxos manuais com DB/R2 simulados passaram. 224 verificações Chromium passaram em 360/390/768/1440 px. npm run check e Testar-Site.ps1 passaram com Wrangler 4.151.1 em dry-run. Scripts de aplicação/reaplicação/restauração e recusa de Worker ausente/divergente passaram em PowerShell 7 e com simulação do formato JSON do Windows PowerShell 5.1. Não houve Windows real, R2 real, deploy, login real de provedores nem TV física neste ambiente.

O teste do navegador foi atualizado para o fluxo real #send-list, com resposta preparada type=m3u também para entrada Xtream; não foi restaurada a confirmação antiga. O administrador ganhou o cartão SubSource. A troca de TV durante validação agora impede envio para a TV errada. Arquivo enviado e salvo na TV são estados diferentes; um pedido existente não é apresentado como prova de TV online.

## Desfazer

O aplicador mostra o caminho exato do backup e o comando Desfazer-Site.ps1. A restauração confere hashes e recusa sobrescrever edições posteriores. Depois de restaurar, publicar separadamente com npx wrangler deploy para reverter o serviço. Não remove o bucket, não restaura D1 e não apaga legendas salvas na TV. Não incluir credenciais, .env ou .dev.vars em pacotes de revisão.
