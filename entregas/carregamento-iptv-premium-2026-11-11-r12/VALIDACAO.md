# Validação r12

## Compilação final

`assembleDesempenho` e `lintDesempenho` concluídos em 11/11/2026. Lint: 0 erros, 154 avisos e 6 sugestões. APK de referência arm64: 97.081.030 bytes; não distribuído. JDK 25, Gradle 9.6, Android SDK 37 e NDK 28.2.13676358 neste host. O heap de 6 GiB foi usado somente na linha de comando do host e não altera o projeto.

## Correções verificadas

- Testes JVM executam as políticas reais de abertura/prioridade, cache real de fichas e métodos reais de preparação extraídos do ViewModel. Android, relógio, banco e rede/imagens usam adaptadores explicitados nos fontes de teste.
- Resposta de metadados em 5,3 segundos termina na primeira chamada, sem corte em cinco segundos. Falha rápida permite recuperação; sair cancela a requisição.
- Prioridade de conteúdo cancela tarefas de capas visíveis e de segundo plano; elas retomam ao liberar a prioridade.
- Uma série selecionada recebe resultado novo enquanto o dono antigo do cache continua bloqueado; uma falha antiga não substitui o sucesso. Liberação por pressão de RAM remove só entradas descartáveis.
- Método real de preparação mantém a ficha do provedor quando o preparo visual lança erro ou excede um segundo; não repete a consulta de dados por causa das imagens.
- Código real de seleção do carrossel reutiliza a capa TMDB confirmada para o filme selecionado sem iniciar rede.
- Carrossel mantém vinte posições desde a seleção mesmo com quatro acertos de RAM. O teste verifica a seleção; apresentação/animação real na TV ainda não foi medida.
- Regressões da r11: 39 verificações de histórico/transferência, pesquisa global/limpeza, capas verificadas, temporadas, filas/retomada rápida, cache persistente, rotação/orçamento, parsers M3U e Xtream normal/streaming com 1.100 filmes.
- Scripts: aplicação desde r10 e r11, repetição idempotente, restauração/reaplicação, proteção de alterações posteriores e sintaxe. Executados em PowerShell 7 com simulação do JSON do Windows PowerShell 5.1; não em Windows nativo.
- Coletores testados com ADB simulado: exportam apenas os campos permitidos, distinguem RAM e armazenamento, rejeitam credenciais e campos extras. Não se conectaram à TV neste ambiente.

## Limites

Não foram acessados o provedor atual, as APIs reais de TMDB nem a TV. Não se afirma que Insaciável 4K, Outra Mamãe ou Pesadelo na Cozinha estejam visualmente corrigidos sem essa validação. IDs e respostas dos testes são fictícios.

A r12 preserva o orçamento e a rotação de armazenamento. Não inclui remoção de listas, histórico, favoritos, dados do aplicativo ou compactação pesada do SQLite durante a abertura. Sem os contadores da TV, não há evidência suficiente para atribuir os quase 700 MB a RAM, banco, gerações antigas ou cache, nem para afirmar o cumprimento do total de 400 MB.

Os fontes atualizados não mudam versão, assinatura ou configuração Gradle. O pacote não inclui APK, listas completas, configurações privadas, bibliotecas binárias ou chaves.
