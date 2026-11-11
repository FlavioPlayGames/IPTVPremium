# IPTV Premium r12 — conteúdo selecionado primeiro

Pacote cumulativo: inclui as capas da r11 e as correções de abertura da r12. Aceita o projeto completo na base r10 ou r11; valida todos os arquivos antes de alterar. Não é necessário instalar os dois pacotes. Arquivos editados fora dessas bases são recusados para preservar suas alterações.

## Instalação na TV

Extraia o ZIP em `Downloads\carregamento-iptv-premium-r12`. No PowerShell do Android Studio, onde sua TV já está conectada:

```powershell
$pasta = "$env:USERPROFILE\Downloads\carregamento-iptv-premium-r12"
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O script aplica os fontes, compila com sua configuração de assinatura e instala sobre o aplicativo existente (`adb install -r`). Não desinstala, não limpa os dados da TV e não altera o pareamento. Versão, credenciais e configurações de assinatura permanecem no projeto. Nenhum APK acompanha a entrega.

Após instalar, atualize uma vez cada lista Xtream e M3U em **Minhas Listas → selecionar → Atualizar lista**, para importar os novos campos de ID externo. Uma atualização realizada na r10 não importou esses campos novos.

## Correções de abertura

- Filme: uma resposta válida que ultrapasse cinco segundos não é mais cancelada e reiniciada a cada cinco segundos. Mantido um prazo total de 15 segundos, até três tentativas apenas quando há falha rápida, e cancelamento ao sair. A consulta de dados usa até 12 segundos desse prazo.
- As imagens de fundo e logo recebem uma antecipação opcional de até um segundo. Imagem lenta, ausente ou inválida não descarta os dados já recebidos. Fotos do elenco carregam na tela de detalhes, sem bloquear sua abertura. A animação mínima existente de três segundos permanece.
- Consultas de capas visíveis e de antecipação são canceladas ao abrir conteúdo e retomadas quando a prioridade é liberada. Isso inclui a consulta de metadados do provedor/TMDB, além dos arquivos de imagem. A consulta de episódios para completar o histórico também pausa durante a abertura.
- Série selecionada não aguarda o lock nem herda o resultado de uma consulta antiga em segundo plano. Resultado válido já concluído em RAM continua sendo reutilizado. Acesso negado pelo provedor não gera nova tentativa automática nas séries.
- O carrossel recebe uma lista limitada e estável desde o clique, sem passar da lista curta da RAM para outra órbita maior depois. Reutiliza também URLs de capas TMDB já confirmadas em memória. A decoração lê RAM/disco e não inicia downloads; a quantidade de capas reais disponíveis depende do cache local.
- Sob pressão de RAM, o cache descartável de fichas/episódios passa a ser liberado junto com os caches de imagens. Os objetos da tela atual, histórico e favoritos não são apagados.

Isso corrige problemas identificados no código; tempo do servidor e resultado visual ainda precisam ser medidos na sua TV. Uma indisponibilidade real do provedor continua podendo exigir tentar novamente.

## Capas, incluídas da r11

Ordem: capa utilizável da lista/provedor → TMDB pelo ID externo real → nome e ano em português (`pt-BR`), inglês (`en-US`) e espanhol (`es-ES`). Se a lista não fornece ano, permanece a validação de identidade para evitar homônimos. IDs de stream e `tvg-id` não são interpretados como IDs TMDB.

Preserva IDs externos explícitos das duas importações e recupera os detalhes nativos de um filme visível sem capa/ID, com cache e concorrência limitados. Não consulta os detalhes de todo o catálogo. Capas genéricas só entram após o término das tentativas, sem antecipação na inicialização.

Capas permanecem limitadas a 300 × 450, mantendo proporção, sem ampliar imagens menores. Mantidas a rotação de arquivos, as filas e os limites existentes de 100 MB de cache gerenciado, 75 MB de imagens e 125 arquivos por tipo.

## Medir os quase 700 MB

A meta permanece **400.000.000 bytes para o aplicativo instalado e seus dados**. Os limites parciais não garantem essa meta quando APK, bibliotecas, catálogos, banco e outros dados já ocupam o restante. A r12 não afirma que o total de 700 MB foi corrigido nem que houve vazamento de RAM: falta a medição da sua TV.

Armazenamento exibido nas configurações da TV e memória RAM são medidas diferentes. O coletor exporta os tamanhos por área, arquivo principal do banco, WAL, páginas livres, gerações antigas e RAM do processo. Páginas livres no SQLite podem continuar ocupando espaço no arquivo; isso precisa ser confirmado antes de escolher uma compactação segura.

1. Abra filmes e séries, incluindo um que exigia tentar novamente. Observe se os dados chegam na primeira abertura.
2. Volte à Home e deixe o aplicativo por 90 segundos sem navegar, permitindo a manutenção existente.
3. Execute:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\carregamento-iptv-premium-r12\Coletar-Desempenho.ps1"
```

Envie `Downloads\IPTV-desempenho-r12.txt`. Para acompanhar os tempos desde antes dos cliques, execute o mesmo comando com `-AoVivo`, reproduza e encerre com Ctrl+C. Nesse modo, a amostra de RAM é a do início; execute novamente sem `-AoVivo` para medir ao terminar.

A coleta é somente leitura. Exporta campos numéricos e estados previstos; não exporta listas, URLs, nomes de conteúdos, credenciais nem corpos de resposta. Não execute limpeza de dados/desinstalação para medir.

## Recuperação

A aplicação guarda os fontes substituídos fora do projeto em `Downloads\IPTV-restauracao-r12-...` e imprime o comando de restauração. Para restaurar a lógica anterior e reinstalar, use `Desfazer.ps1 -Backup "PASTA_DA_RESTAURACAO" -Instalar`.

Na atualização desde r10, a migração Room 6→7 acrescenta duas colunas opcionais, sem apagar linhas. Ao desfazer, três arquivos de modelo/esquema compatível permanecem para permitir abrir o banco já migrado. Não há downgrade destrutivo do banco. Desde r11, os arquivos que já estavam corretos permanecem intocados.
