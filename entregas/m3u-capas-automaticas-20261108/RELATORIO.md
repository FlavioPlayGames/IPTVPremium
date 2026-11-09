# Correção do caminho M3U já persistido

## Falha encontrada

O pacote anterior passava apiCovers = source.forceM3u ao importador nativo. forceM3u indica a opção administrativa de M3U direta, não se uma lista nativa precisa de capas. Uma lista automática já mantida como M3U podia permanecer nativa por decisão persistida, sem renegociar o transporte, e também pular a consulta de metadados de capas. O teste anterior exercitava o importador diretamente com o parâmetro padrão, sem reproduzir esse caminho completo do sincronizador.

O sincronizador agora deixa a consulta opcional habilitada também para esse caso. A resolução de endereço, prazo, correspondência e leitura de cover continuam no helper existente. Não há migração automática do transporte nem mudança de IDs, vídeos, prioridade ou regra de retentativa Xtream.

## Imagem compartilhada entre obras

A M3U fornecida anteriormente contém 132 episódios de O Conto da Aia em duas edições e 29 de O Conto da Raposa de Nove Caudas. Uma das URLs de imagem é compartilhada por essas obras. Uma imagem estável por série, sozinha, não comprova uma capa correta quando o mesmo endereço é declarado por outra obra.

O preparador agora rejeita candidatas nativas cujo endereço seja compartilhado por nomes normalizados diferentes ou anos conhecidos conflitantes. Edições compatíveis da mesma obra podem manter sua imagem própria. O helper que associa capas da API à M3U aplica a mesma proteção às candidatas retornadas para as obras solicitadas, evitando reintroduzir uma URL compartilhada por títulos/anos incompatíveis através dessa segunda etapa.

Nenhuma capa é emprestada de outra série nem pesquisada no TMDB. Sem uma candidata confiável, a URL do catálogo fica ausente, para não publicar uma imagem identificada como ambígua. A política não faz reconhecimento visual nem detecta toda imagem incorreta que apareça isoladamente nos dados do provedor. A validação final depende do retorno real da API e da tela da TV.

## Reprodução automatizada

CachedNativeCoverTest usa as 161 entradas da amostra real anterior, preservando títulos/categorias/URLs públicas das imagens e substituindo os links de vídeo por endereços fictícios. A base tem uma geração ACTIVE anterior e uma decisão persistida M3U para uma fonte automática forceM3u=false. O teste chama o sincronizador real, que chama o importador e writer reais; o serviço de metadados é simulado. Não é uma consulta ao provedor real nem uma reprodução visual em Android.

O caminho consulta apenas get_series_categories/get_series para capas, associa os dados às próprias séries, prepara READY sem alterar ACTIVE, conserva o transporte M3U e mantém todos os 161 episódios indexados fora do Room. Foram testadas API saudável, API indisponível e API devolvendo a mesma URL para obras diferentes. Guards adicionais testam anos incompatíveis e edições compatíveis.

Regressões cobrem metadados de capas, importador, preservação do catálogo, cancelamento, ordenação e sincronização/prioridade do Xtream. As ferramentas verificam aplicação/restauração idempotente, bloqueio de divergência/corrupção/conflito e privacidade da coleta. O coletor agora preserva native_covers/rejected, series_covers/matched e classes de erro de uma lista restrita; não salva URL, credencial ou mensagem livre de erro.

## Escopo e instalação

Três arquivos Kotlin alterados: sincronizador M3U, preparador M3U e associação de capas da API à M3U. Nenhuma migração do banco 6, dependência, alteração de qualidade de vídeo, mudança de layout ou limpeza de dados. Instalar e atualizar a mesma lista é necessário para substituir posterUrl da geração anteriormente gravada. Leia LEIA-ME.md e envie o diagnóstico desta revisão para confirmar o caminho executado na TV.

## Resultado final

APK desempenho e Lint: BUILD SUCCESSFUL (7m36s). Cenários aprovados: cached-native: 4, native-guard: 2, provider: 6, official: 20, sync: 21, installer: 4, scripts: 2, collector: 1. A disponibilidade e associação das capas do serviço real devem ser confirmadas na TV após atualizar a lista.
