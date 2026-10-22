# Importação do IPTV Plus: resultado da análise da interface

Análise estática do JavaScript fornecido pelo usuário, complementando a análise do APK 3.0.4. Nenhuma lista foi enviada a terceiros, e o JavaScript não foi executado.

## O que o código mostra

| Etapa | Xtream | M3U |
|---|---|---|
| Selecionar lista | Valida conta pelo player_api.php | Baixa texto completo e envia arquivo para /parse_playlist |
| Obter catálogo | Consulta diretamente endpoints Xtream de streams e categorias | Pede /get_playlist_data por tipo: tv, movie, serie; recebe data e groups |
| Iniciar seções | Dispara carregadores de Canais, Filmes e Séries sem aguardar entre eles | O mesmo disparador chama os três carregadores |
| Organizar | Monta índices de IDs, itens e categorias em memória | Também organiza a resposta recebida |
| Manter interface responsiva | Tenta usar Web Worker; possui alternativa em blocos de 500 registros com setTimeout(0) | Mesma infraestrutura de organização |

Dentro de cada carregador Xtream, primeiro aguarda os streams da seção, depois solicita suas categorias. Os endpoints examinados não recebem category_id: são consultas da seção inteira, não grupos de sete categorias. A sobreposição de rede das três seções é distinta da organização: existe uma cadeia de Promises que serializa os trabalhos de organização.

O caminho M3U encontrado na seleção é download via XMLHttpRequest, FormData/Blob e POST para /parse_playlist. Isso confirma que o cliente solicita processamento à API. O código interno do servidor não está disponível: não sabemos seu parser, hardware, cache ou tempo. Depois, os carregadores obtêm os dados separados por seção. Não afirmar que toda lista ou toda versão utiliza o mesmo caminho.

## Cache e armazenamento

O catálogo organizado é publicado no estado em memória (setChannelsContent, setMoviesContent e setSeriesContent). Há verificações de status success/none para evitar certas recargas na navegação. localStorage guarda, entre outros dados, favoritos, histórico e configurações por lista. Essas evidências não comprovam um catálogo completo persistido em disco para reabertura do aplicativo. Não atribuir a velocidade a um cache persistente ainda não demonstrado.

## Worker e lotes

O cliente cria um Worker externo e troca mensagens ParseCategories, ParseMoviesContent e ParseChannelsContent. Seu arquivo externo não foi fornecido. O código revela a tentativa de delegar a organização, mas não permite medir o desempenho do Worker nem afirmar que ele funciona em todo dispositivo. Para filmes, há timeout de cinco segundos antes da alternativa local.

A alternativa local organiza 500 registros por turno e usa setTimeout(0) entre turnos. Esse número é uma unidade de processamento JavaScript, não tamanho de download, paginação do provedor ou lote de gravação Room. Não equivale aos nossos 250 registros Xtream ou 2000 M3U.

A aplicação também usa startTransition ao publicar resultados. Isso prioriza a atualização de interface; não acelera a conexão com o provedor.

## O que aplicar ao nosso diagnóstico

A diferença observável é arquitetural: três seções podem baixar sobrepostas, organização compacta em memória e processamento M3U solicitado ao servidor. Isso não demonstra qual etapa explica a diferença de tempo percebida na TV.

Nosso aplicativo mantém catálogo persistente em Room, staging e índices. Antes de alterar o lote, medir separadamente: espera pelo primeiro byte, download, parse, classificação, gravação e publicação da seção. Assim podemos otimizar a etapa cara sem remover a persistência nem perder o formato Xtream.

A regra aprovada permanece: prioridade à seção acessada, reutilização do importado e preparação ociosa Filmes → Séries → Canais, uma seção por vez. Não foi modificada. Copiar o disparo simultâneo das três seções mudaria essa regra e pode aumentar competição por rede e memória.

## Evidências para reprodução local

Arquivo original: iptvplus-interface.js, 4.449.591 bytes.
SHA256: e80c9bc8c80be2a4255c7869a28e4621c26e76301e9455d10b50558a4043100c.
Origem declarada e referenciada pelo APK: https://tv.iptvpluseplayer.live/bundle.js.
Data declarada: 2026-10-22T14:19:49.9648597Z.
O hash e o tamanho conferem com o arquivo enviado; isso verifica integridade em relação ao manifesto, não autenticidade independente da origem.

Inspeção sem eval: tabela de 15.128 strings; deslocamento 0x1ee; rotação encontrada por reprodução aritmética limitada de 306 posições; substituição estática de chamadas à tabela e formatação. Não foram executados o bundle, seus módulos ou o Worker. A versão do JavaScript pode mudar independentemente do APK.

Referências locais na cópia formatada para análise (nomes ofuscados preservados):

- _0x213855: getPlaylistData, parsePlaylist, downloadPlaylist e downloadPlaylistXHR.
- _0x5dfcce: login, getChannels, getMovies, getSeries e métodos de categorias.
- _0x3e8ddd: dispara _0x108c2a(), _0x3646ce(), _0x327336() sem aguardar entre chamadas.
- _0x557e04: seleção M3U, download → FormData → parsePlaylist; seleção Xtream → login.
- _0x694443 e _0x260156: tentativa de Worker; _0x352d26 e _0x46c835: alternativa local em blocos de 0x1f4 = 500.
- _0x222f77: cadeia de organização; setters de conteúdo: publicação no estado em memória.

O APK, o JavaScript e o código de terceiros não estão publicados neste diretório. Não há medição em execução nem mudanças no IPTV Premium nesta entrega.
