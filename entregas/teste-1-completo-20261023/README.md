# Teste 1 completo — 1.12-T1.1

Arquitetura de catálogo nativo e progressivo, inspirada nos princípios do OwnTV. O cadastro pelo site permanece; QR + PIN não foi implementado.

## O que mudou

- Xtream: leitura direta com JsonReader, sem montar o JSON completo; até duas fases de rede, um escritor e lotes limitados. A Home libera após autenticação/categorias; as abas aproveitam o primeiro lote já gravado e recebem os demais pelo Paging.
- M3U: uma leitura linha a linha alimenta diretamente canais, filmes, séries e episódios. Sem guardar a lista inteira ou criar arquivos intermediários por seção. Lotes de até 400 entradas, com limite adicional de caracteres estimados.
- Room + Paging 3 continuam na leitura, agora com FTS4 unicode61 para pesquisar nomes de canais, filmes e séries. Busca por começo de palavra, sem diferenciar acentos. As consultas mantêm o isolamento por lista e filtros de acesso.
- Atualização incremental: reaproveita a identidade do catálogo, compara lotes e grava apenas conteúdo alterado ou mudança de posição. Exclusões só ocorrem após terminar uma resposta válida. Uma resposta interrompida preserva registros anteriores, embora novos lotes já confirmados possam ficar visíveis.
- Trocar de lista cancela o importador anterior. Reabrir uma lista completa reutiliza o banco; um processo interrompido pode continuar a importação. Atualização explícita consulta novamente o provedor.
- OkHttp 5.1: conexões compartilhadas, gzip transparente, cancelamento da chamada HTTP e leitura em fluxo. Integração com catálogo, TMDB/TVmaze, cadastro pelo site, Coil e Media3.
- Koin 4.1: instância única de HTTP, banco, importador e imagens por processo. As fábricas existentes de algumas telas continuam.
- Coil 3.3: carregamento real de capas e imagens de detalhes, cache compartilhado em RAM (10% do limite do processo) e disco (96 MiB), tamanho de decodificação limitado. Cache em disco é limitado e pode ser removido pelo Android; não é armazenamento permanente garantido. As capas genéricas locais permanecem.
- Configurações → Rede e DNS: DNS do aparelho, Google, Cloudflare, Quad9, IP próprio e DoH HTTPS com IP de inicialização. O VLC nativo continua usando o DNS do aparelho; Media3 usa a configuração nova.
- EPG continua fora da importação; pode ser desativado nas configurações de reprodução.
- Compatibilidade mínima Android 8/API 26, incluindo leitura de assinaturas e validação de atualização em aparelhos anteriores ao Android 9.

Não há garantia de redução específica de tempo: servidor, rede e armazenamento da TV ainda determinam a velocidade. O teste visual deve comparar primeira abertura, navegação durante a carga, troca de lista, reabertura após fechar e atualização da mesma lista.

## Instalar e gerar APK no Windows

Na raiz do projeto:

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/teste-1-completo-20261023'
$script=Join-Path $env:TEMP 'Preparar-Teste-1-Completo.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Teste-1-Completo.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Instale `app\build\outputs\apk\debug\app-debug.apk`. A versão deve aparecer como **1.12-T1.1**. Preserve a mesma chave de assinatura. O script não substitui `local.properties`, `google-services.json`, recursos ou chaves. Se algum arquivo tiver mudanças locais desconhecidas, interrompe antes de alterar qualquer arquivo.

O script aceita as bases enviadas nesta conversa, inclusive o Teste 1 anterior, e cria backup fora de `app/src`, em `.native-test1-complete-backups`.

## Primeira abertura e comparação

O catálogo deste teste usa `playlist_cache_native_test1_complete.db`. O banco de produção e o do T1 anterior permanecem preservados. Por isso a primeira abertura precisa importar o catálogo neste banco novo. Não compare essa importação inicial com uma abertura já em cache do outro APK.

O Coil usa um cache de imagens novo: a primeira visita pode baixar novamente uma capa guardada apenas no cache antigo. As próximas visitas aproveitam RAM/disco. Favoritos, histórico, cadastro e fontes cadastradas permanecem nos armazenamentos existentes.

Para testar DNS, use primeiro o modo automático; só compare outro DNS com a mesma lista/rede. DNS não corrige um servidor indisponível.

## Desfazer

Baixe `Desfazer-Teste-1-Completo.ps1` da mesma pasta e execute com `-Projeto` e `-Testar`. Depois instale o APK novamente. O script restaura exatamente os arquivos salvos e remove apenas os novos arquivos adicionados pelo pacote. Se houver alteração local posterior, interrompe para não sobrescrevê-la. O catálogo anterior volta a ser utilizado pelo código anterior.

## Referências

- https://github.com/ahXN00/OwnTV/wiki
- https://github.com/ahXN00/OwnTV_Core
- https://developer.android.com/training/data-storage/room/defining-data#fts
- https://coil-kt.github.io/coil/

Não é uma cópia completa do OwnTV: mantém cadastro, telas e funções do IPTV Premium, com o núcleo de importação e serviços descritos acima. DNS, Coil, DI e busca estão conectados ao uso real do app.
