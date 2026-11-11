# Capas e imagens temporárias r10

Base: fontes da r9. Mantém o histórico novo, perfis normal/Kids, pesquisa, assinatura, versão e configurações locais.

## Instalar

Pode usar o PowerShell do Android Studio, onde a TV já está conectada e autorizada:

```powershell
$pasta = "$env:USERPROFILE\Downloads\capas-iptv-premium-r10"
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O script confere os hashes, salva os fontes anteriores em Downloads\IPTV-restauracao-r10-HORARIO, aplica os arquivos, compila desempenho com a assinatura local e instala a ABI correta com adb install -r. Não apaga dados, listas, histórico, favoritos ou o cache de imagens. Não executa a limpeza física novamente. Se o ADB não encontrar a TV, conecte/autorize e execute de novo; os fontes já aplicados serão reconhecidos.

**Depois de instalar, atualize a lista Xtream uma vez em Minhas Listas → selecionar a lista → Atualizar lista.** Os campos alternativos descartados na importação antiga só podem ser recuperados ao reler a resposta do provedor. O fallback TMDB já funciona sem essa atualização, quando houver identificação segura da obra.

## Alterações

- Importação normal e rápida do Xtream aproveitam movie_image → cover_big → cover → stream_icon, escolhendo o primeiro endereço utilizável. Os detalhes usam a mesma ordem. Endereços vazios, inválidos ou de esquemas não suportados não ocultam uma alternativa válida.
- Filmes passam a buscar capa no TMDB quando a do provedor está ausente ou não pode ser carregada. Vale para M3U e Xtream. Só são aceitas correspondências verificadas por nome oficial, ano e IDs disponíveis; não se escolhe o primeiro resultado por popularidade. Obras ambíguas permanecem com a imagem temporária.
- Capas de séries e temporadas continuam usando a identidade verificada. Não se usa foto de episódio como substituto arbitrário de pôster.
- A imagem temporária aparece desde o primeiro quadro, inclusive durante download, busca e espera por prioridade. As quatro imagens locais existentes são compartilhadas; nenhum novo recurso pesado foi adicionado.
- Cartões voltam a tentar uma falha temporária de forma limitada e reagem à volta para a área visível e à conclusão de uma antecipação do mesmo endereço. Não ficam definitivamente presos na capa genérica após a primeira tentativa.
- Falhas temporárias, falta de orçamento, seleção de mídia e cancelamentos não são tratados como uma imagem inexistente por cinco minutos. O registro antigo de falhas é descartado; os arquivos de imagem existentes são preservados. O bloqueio longo fica para 404/410 e arquivos acima do limite.
- Imagens locais válidas e capas TMDB já preparadas são reaproveitadas antes de nova rede. A busca de pôster de filme não solicita créditos/elenco desnecessários.
- Até duas buscas de identidade são executadas por vez, com no máximo uma de antecipação, deixando espaço para capas visíveis. Downloads e decodificação têm seus próprios limites.

## Resolução, fila e armazenamento

As capas armazenadas continuam em **até 300 × 450 pixels**, mantendo a proporção e sem ampliar imagens menores. O TMDB usa a miniatura oficial w342, próxima da resolução final, em vez de original/w500/w600 e do preset w600_and_h900_bestv2 quando estiverem maiores. Isso muda somente o transporte da imagem; preserva a chave do cache e os endereços de reprodução. Outros provedores mantêm seus endereços; a imagem recebida é preparada localmente no limite existente.

A fila continua começando em 15 e crescendo até 25 capas à frente, em grupos de cinco, com dois trabalhadores de antecipação. As cinco mais próximas são preparadas em memória. Navegação rápida mantém os tempos anteriores. Filmes e séries usam a mesma fila tipada, incluindo os casos sem URL.

O orçamento permanece em 100.000.000 bytes no cache gerenciado, 75.000.000 bytes para imagens e 125 arquivos por tipo, com a rotação existente. A meta de 400 MB do aplicativo instalado deve continuar sendo medida na TV; esses limites não foram aumentados.

## Validar e medir na TV

Após atualizar a lista Xtream, busque “Insaciável 4K” e “Outra Mamãe”; teste também M3U, filmes, séries e Kids. Confira a imagem temporária durante a espera, a substituição pela capa real e a navegação de ida e volta.

Para coletar tempos e decisões sem exportar URLs, títulos ou credenciais:

```powershell
$pasta = "$env:USERPROFILE\Downloads\capas-iptv-premium-r10"
powershell -ExecutionPolicy Bypass -File "$pasta\Coletar-Diagnostico.ps1" -AoVivo
```

Com a coleta aberta, reproduza o problema na TV e encerre com Ctrl+C. O arquivo fica em Downloads\IPTV-capas-r10.txt. O campo source identifica PROVIDER, TMDB ou NONE; workHash é um identificador resumido, não o título do filme.

Para desfazer os fontes: execute Desfazer.ps1 -Backup PASTA_DA_RESTAURACAO e recompile. Isso não desfaz ações realizadas nos dados do aplicativo.
