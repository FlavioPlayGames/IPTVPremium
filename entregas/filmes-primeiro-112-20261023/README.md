# Versão 1.12 — Filmes primeiro, Séries e Canais por acesso

Feito sobre o java.zip enviado em 23/10/2026. Mantém versionName 1.12 e versionCode mínimo 14, respeitando APP_VERSION_CODE se for maior. Preserva assinatura e applicationId.

## O fluxo

| Ação | Resultado |
|---|---|
| Importar uma lista nova | Prepara Filmes com prioridade sobre histórico, capas de catálogo e manutenção; depois libera a Home. |
| Atualizar lista, inclusive pelo fluxo automático existente | Constrói uma geração nova com Filmes e deixa Séries/Canais pendentes, seguindo a mesma regra. |
| Abrir Séries | Importa somente Séries, mantendo uma tela de carregamento até o catálogo e seus metadados estarem prontos. |
| Abrir Canais | Completa somente Canais; usa o carregamento já existente da aba. |
| Reentrar em uma seção concluída | Usa o catálogo salvo; não importa de novo. |
| Sair durante a importação de uma seção | Cancela a operação; uma tentativa posterior recomeça a seção incompleta. Filmes e a seção já pronta continuam salvos. |
| Trocar para outra lista já importada | Usa o catálogo dessa lista; não reutiliza os dados da fonte anterior. |

Não completa Séries/Canais automaticamente na Home ou na ociosidade. Catálogos antigos que já estejam completos continuam sendo reaproveitados: **use Atualizar lista para testar a nova divisão numa lista já importada**.

## Xtream e M3U

Xtream inicial usa autenticação, categorias de Filmes e Filmes; não consulta endpoints de Séries/Canais. Cada aba completa somente seus próprios endpoints. Mantém streaming, lotes 500 e recuperação por categoria quando a resposta completa é inválida. Recusa definitiva de acesso encerra sem repetir quatro vezes.

M3U é um arquivo único e precisa ser lido inteiro. Durante essa leitura, grava Filmes no Room e separa as entradas de Séries e Canais em dois fragmentos locais comprimidos, com compressão rápida. Não mantém esses catálogos inteiros na RAM. Ao abrir as abas, importa os fragmentos sem novo download. Eles sobrevivem ao fechamento do processo, ficam no armazenamento privado sem backup e são removidos depois da publicação ou pela limpeza de gerações antigas. Se um fragmento for removido externamente, recupera somente a seção pedida relendo a fonte. Limite de segurança: 128 MiB comprimidos por geração pendente; falhas não publicam um catálogo parcial.

Cada seção é construída numa geração temporária. Sua publicação é transacional e confere se a geração ativa continua a mesma. Uma atualização ou troca concorrente não publica os dados antigos sobre a nova geração. Não muda o esquema do banco nem exige apagar dados.

## Prioridade, memória e capas

- Histórico e conciliação opcional aguardam a importação. Séries pendente não é reconciliada como se estivesse vazia; o histórico dessa seção é reprogramado após sua conclusão.
- O agendador automático não inicia outra importação sobre a seção que está carregando. Canais também sinaliza prioridade de mídia para evitar trabalho opcional durante seu uso/reprodução.
- A Home não decodifica sua arte atrás da tela de importação. O preparo das quatro capas genéricas começa após a liberação; continua compartilhado, sem uma imagem por título.
- Remove o preparo e a reserva inicial de capas para a Home.
- Mantém **RAM como mesa de trabalho e disco como armazenamento entre sessões**, cache de 125 por seção, grupos de cinco e prioridade para as capas visíveis. Não apaga os downloads já existentes.
- Metadados de categorias ficam disponíveis em memória; o catálogo permanece no banco e as páginas de itens são limitadas a uma janela de 180, inclusive em Canais. Não carrega todo o catálogo na RAM.
- M3U ganhou um observador de cancelamento que solicita desconexão da rede fora da thread da interface. Controle e Voltar continuam utilizáveis; prioridade não significa bloquear a interface ou garantir toda a CPU/banda do aparelho.

## Aplicar pelo PowerShell

Feche o aplicativo e aguarde outra compilação terminar antes de aplicar.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/filmes-primeiro-112-20261023'
$script = Join-Path $env:TEMP 'Preparar-Filmes-Primeiro-1.12.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Filmes-Primeiro-1.12.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O script baixa o pacote e verifica seu SHA-256 e todos os arquivos antes de escrever. Fontes diferentes dos enviados são recusados sem sobrescrever. Guarda backup em .movies-first-112-backups, fora de app/src. A opção -Testar executa assembleDebug; depois instale o APK gerado sobre o app existente, com a mesma assinatura.

Não inclui local.properties, google-services.json, credenciais, imagens restauradas ou testes no aplicativo. Validações externas estão descritas em VERIFICACAO.txt. Não houve teste no controle da TV; não se promete um tempo de importação sem medição no aparelho.
