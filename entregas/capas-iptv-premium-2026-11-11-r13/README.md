# IPTV Premium r13 — conservar as capas ao voltar ao catálogo

Atualização sobre a **r12**, com aplicação por hashes e backup externo. Corrige a reutilização das capas TMDB quando a ficha do provedor revela ID/ano diferentes dos campos originalmente importados. Filmes Xtream e M3U usam o mesmo fluxo; os dois catálogos, normal e Kids, conservam uma capa já exibida quando uma tentativa posterior falha.

## Instalar

Extraia em `Downloads\capas-iptv-premium-r13` e execute no PowerShell do Android Studio, onde a TV já está conectada:

```powershell
$pasta = "$env:USERPROFILE\Downloads\capas-iptv-premium-r13"
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O script verifica a base r12 antes de alterar fontes, guarda recuperação em `Downloads\IPTV-restauracao-r13-...`, compila usando o projeto local e instala com `adb install -r`. Versão e assinatura continuam sendo as do seu projeto. O pacote não inclui APK.

## O que foi corrigido

Na r12, uma busca podia começar com título sem ano/ID e terminar com o ano/ID recebido dos detalhes do provedor. A capa TMDB ficava associada à identidade enriquecida. Ao reconstruir o catálogo, o aplicativo procurava com os dados originais e podia aguardar novamente a URL ruim do provedor, embora já tivesse uma capa alternativa correta.

A r13 grava um vínculo de sucesso entre o item original e a imagem TMDB confirmada. A chave inclui lista, ID do item, URL original e campos de identidade; alteração desses dados gera outra chave. O cache guarda somente o ID TMDB e o caminho público da imagem. Identificadores da lista, títulos e URLs do provedor entram apenas no hash, sem persistência em texto nesse vínculo.

- Ao retornar, a capa original disponível no cache mantém prioridade; a alternativa já confirmada também pode ser recuperada diretamente, sem refazer as tentativas anteriores no provedor.
- Se o arquivo da alternativa foi removido pela rotação, sua URL confirmada pode ser reutilizada para baixar a imagem, sem nova busca por título/ano.
- A fila de antecipação e a proteção dos arquivos visíveis usam a mesma URL resolvida do card.
- Uma falha posterior não substitui por vazio/genérico uma imagem válida do mesmo card. Mudanças de identidade, URL, lista ou temporada continuam recriando o estado para evitar capas antigas em outro conteúdo.

A primeira resolução de um item ainda depende das respostas do provedor e do TMDB. O coletor não registra nomes dos filmes; não é possível ligar um hash do relatório diretamente a Insaciável.

## Limites preservados

Capas de até 300 × 450, proporção preservada, rotação de arquivos e todos os limites anteriores mantidos: 100 MB de cache gerenciado, 75 MB de imagens e 125 imagens por tipo. Os vínculos usam o cache de metadados já existente, dentro dos mesmos 128 registros/8 MiB de RAM e da cota existente de respostas em disco, com validade de seis horas. Não há banco ou orçamento adicional.

A prioridade dos dados do conteúdo selecionado da r12 permanece. A r13 não altera importação, histórico, favoritos, políticas Kids/parentais ou configuração Gradle. A meta total de 400 MB continua precisando da medição de armazenamento: o relatório de desempenho anterior continha somente RAM, cerca de 219 MiB naquela amostra, sem comprovar ou excluir vazamento.

## Testar na TV

1. Localize Insaciável, deixe a capa aparecer e abra os detalhes.
2. Volte ao catálogo e repita duas ou três vezes, observando se a capa permanece/reaparece sem a demora anterior.
3. Faça o mesmo com outro filme afetado e nas duas formas de importação que usa.

Na primeira resolução após a instalação, o vínculo pode precisar ser criado. A partir daí, o retorno é o principal cenário a comparar.

Se persistir, inicie antes de navegar:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\capas-iptv-premium-r13\Coletar-Diagnostico.ps1" -AoVivo
```

Reproduza e encerre com Ctrl+C. Envie `Downloads\IPTV-capas-r13.txt`. Foram acrescentados registros `stage=binding event=HIT/SAVED`, com hash anônimo, para verificar a recuperação do vínculo na TV. O coletor mantém os filtros que excluem URLs, credenciais e campos extras.

Para desfazer, use o comando impresso na aplicação ou `Desfazer.ps1 -Backup "PASTA_DA_RESTAURACAO" -Instalar`. A r13 não altera o esquema do banco.
