# Troca de listas e carregamento dos canais

Correção sobre o último `java.zip` enviado, mantendo a versão 1.11 (13).

- As sessões de Filmes, Séries e Canais passam a pertencer à lista ativa. Ao trocar, as consultas e páginas em memória da sessão anterior são encerradas. O cache persistente do catálogo permanece no Room e é reutilizado quando a lista já está importada.
- A consulta da geração ativa também reinicia ao mudar de lista, sem usar provisoriamente a geração da anterior.
- Home e Minhas listas deixam de se sobrepor por efeito de transição. A seleção persistida é confirmada e o bloqueio da operação é liberado antes da navegação final.
- A gravação inicial da origem do histórico sai da thread de interface.
- Canais mostra **“Estamos conectando seus canais. Aguarde…”** durante a vinculação inicial, a preparação dos metadados e o carregamento da primeira página. O tratamento de erros da consulta continua separado.

## Aplicar pelo PowerShell

Na pasta do projeto, execute:

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/troca-listas-canais-20261022'
$script = Join-Path $env:TEMP 'Preparar-Troca-Listas-Canais.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Troca-Listas-Canais.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O instalador confere os arquivos e o pacote antes de copiar, preserva backup em `.playlist-switch-backups` e recusa sobrescrever alterações locais diferentes do projeto enviado. Não apaga catálogos, listas ou imagens. Não instala o APK automaticamente.

Se baixar o ZIP manualmente, acrescente `-PacoteLocal 'C:\caminho\troca-listas-canais.zip'` ao comando do script.

## Conferir na TV

Gere e instale o novo APK. Alterne entre duas listas com títulos e canais diferentes. A Home deve aparecer sem Minhas listas por cima; ao abrir cada catálogo, os itens devem corresponder à lista selecionada. Em Canais, confira a mensagem durante a preparação. Volte à primeira lista já importada e confirme o reaproveitamento do catálogo salvo. Uma atualização explícita continua solicitando nova importação.

Compilação e verificações locais estão em `verificacao.txt`. A fluidez, o foco do controle e os tempos reais precisam de validação na TV; não foram medidos em aparelho nesta entrega.
