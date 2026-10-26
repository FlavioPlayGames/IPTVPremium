# Armazenamento, RAM e fluidez — avaliação inicial

Base: APK desempenho com piloto API-primeiro 158860b e captura de 26/10/2026 às 15:34. Esta etapa é uma inspeção e coleta; não aplica mudança ao APK.

## Evidência atual

- Nenhum registro de OutOfMemoryError, onTrimMemory ou Skipped frames na captura. Isso não comprova ausência de travamentos ou vazamentos em todas as telas.
- 21 eventos de coleta de lixo. O tempo total de uma coleta concorrente não equivale à pausa da interface; pausas registradas são distintas. Não atribuir as centenas de milissegundos da duração concorrente a um congelamento da UI.
- Capas de filmes: 60 cargas bem-sucedidas registradas, mediana 50 ms, percentil 95 de 1161 ms, máximo 1163 ms. Inclui cache e rede; a amostra é pequena e não representa todas as categorias.
- Capas de séries: 37 cargas bem-sucedidas, mediana 33 ms, percentil 95 de 81 ms, máximo 267 ms. Algumas operações canceladas acompanham navegação e mudança de prioridade; cancelamento não equivale automaticamente a falha de imagem.
- Primeiro cenário de séries: preparação 1198 ms; cenário em cache posterior 12 ms. Não é a duração de abertura de um episódio.
- Última fotografia de armazenamento: cerca de 788 MB totais; três catálogos ACTIVE e um STALE. A limpeza STALE iniciou, apagou séries e foi pausada ao começar nova importação. Não há fotografia final após sua conclusão nesse arquivo.
- Medição anterior por páginas: 558.610 episódios da M3U antiga ocupavam aproximadamente 447 MB incluindo índices. Isso é armazenamento persistente de catálogo, não RAM nem vídeos baixados. A conta de episódios pode variar conforme a importação e a correção de E1000.
- Não há PSS/Native/Graphics amostrados na última captura. O heap observado pelos logs de importação não mede a RAM total do app, do player e dos bitmaps.

## Revisão de código

- Imagens têm cotas adaptadas ao heap e aparelho: política compartilhada para capas, detalhes e carrossel, com limites de cada cache. Cota de bitmap não é um limite total de PSS.
- Há reação a trim/low-memory e verificação periódica a cada cinco segundos fora da thread de UI, com histerese e espaçamento dos cortes de cache.
- Catálogo usa Room/Paging; não carrega todo o catálogo para montar uma grade. As decodificações e rede dos carregadores examinados usam IO/Default e semáforos; imagens são reduzidas para o espaço de exibição.
- Antecipação ativa observada trabalha em grupos e mantém até três fileiras preparadas. Não aumentar antecipação, paralelismo, qualidade de decode ou tamanho de cache sem o teste de RAM.
- Cache em disco já possui rotação por cota, protegendo leitores e downloads. Mantém reserva mínima de 24 MB para evitar que o excesso do banco desligue as capas.
- A medição detalhada dbstat de aproximadamente 29 segundos foi retirada da manutenção normal no piloto API-primeiro.
- A limpeza atual de gerações antigas não implementa sozinha a exclusão completa de todos os dados de uma fonte. Cadastro, favoritos, bloqueios e caches estão distribuídos entre DataStore, preferências, banco e arquivos. Alguns arquivos de imagens são compartilhados por hash da URL, e o cache de episódios usa hash de chave; sua propriedade por lista precisa ser resolvida antes de apagar arquivos individualmente.

## Regras solicitadas para a próxima correção

| Situação | Remover | Preservar |
| --- | --- | --- |
| Pressão de espaço | Catálogo/cache descartável da lista menos usada, se necessário | Cadastro, favoritos, histórico e preferências pessoais; proteger a lista ativa e trabalhos em andamento |
| Exclusão confirmada pelo app/site | Dados exclusivos da lista, catálogos, favoritos comuns/Kids dessa fonte, avaliações/preferências/bloqueios da fonte, decisões de transporte e credenciais | Histórico de uso; dados de outras listas e configurações globais |
| Troca efetiva de link/conta | Dados associados à versão anterior da fonte, sem misturar com a nova | Histórico de uso; dados de outras listas e configurações globais |
| Apenas renomear | Não executar limpeza de conteúdo | Catálogo e dados existentes |

O usuário prefere manter todas as listas baixadas para troca imediata, autorizando liberar a menos usada somente quando faltar espaço. Ao escolher novamente uma fonte cujo catálogo foi descartado, será necessária nova importação. A identificação da menos usada precisa usar registros de acesso reproduzíveis, não a ordem dos cadastros nem a data de atualização do catálogo. A implementação atual não mantém um registro completo do último acesso ao catálogo de cada lista.

## Cuidados de implementação e validação

1. Registrar acessos por lista; projetar política de espaço sem remover o catálogo ativo.
2. Separar descarte automático de cache de exclusão explícita. Histórico não é cache.
3. Registrar trabalhos de limpeza duráveis por fonte/versão para sobreviver a fechamento do app. Uma limpeza antiga não pode apagar uma importação nova do mesmo ID.
4. Identificar eventos reais de exclusão do site: falha de rede, lista desativada e resposta parcial não significam exclusão.
5. Limpar somente as preferências pertencentes à fonte removida. Não apagar PIN/tema globais nem favoritos de outra lista. No compartilhamento entre listas, remover somente os vínculos/flags pertencentes à fonte, preservando os registros necessários ao histórico.
6. Imagens compartilhadas ficam enquanto outra fonte ou leitura ativa as utiliza; liberar as referências da fonte excluída e os arquivos exclusivos. Cache de episódios/metadados precisa de propriedade de origem verificável.
7. Troca explícita de link pode renegociar transporte após invalidar o estado anterior. Falha temporária de API não deve mudar transporte nem executar limpeza destrutiva.
8. Testar exclusão local e remota, alteração de link, renomeação, cancelamento, reinício durante limpeza, reaproveitamento do mesmo ID, preferências Kids, histórico e fontes que compartilham imagens.
9. Medir PSS, Native, Graphics e frames em navegação repetida e após sair do player antes de reduzir caches de RAM. Não afirmar melhoria perceptível apenas pela ausência de avisos no logcat.

400 MB permanece uma meta. Uma M3U cujo catálogo ativo ultrapassa esse valor não pode ser corrigida somente apagando capas ou outras listas; nesse caso é necessário reduzir a representação persistente ou usar API compatível. Não haverá descarte silencioso de episódios para caber no limite.

## Captura preparada

Medir-Fluidez.ps1 executa no PC via ADB com o APK atual, sem reinstalação, sem limpar cache ou logcat, sem importar e sem parar o app. Amostra meminfo/proc a cada 15 segundos (mínimo configurável 10), mantém logcat por UID e fotografa gfxinfo/cpuinfo no início/fim. Reseta apenas contadores diagnósticos de frames. Guarda brutos, CSV e resumo de picos com timestamps num ZIP.

Limitações: coleta pode acrescentar alguma carga; picos entre amostras podem passar despercebidos; getSystem/driver e meminfo podem variar conforme TV. Dados ausentes ficam sem valor, sem inventar zero. Gfxinfo mede o que o sistema disponibiliza, não uma certificação de fluidez. Observar também a experiência visual.

Teste sugerido (15 minutos): 90 segundos na Home; navegar lentamente e rapidamente por filmes e séries, abrir detalhes/atores e retornar; Kids; reproduzir e sair de um canal e um episódio; repetir o trajeto inicial para comparar cache/retorno. Não importar, atualizar ou limpar cache durante a captura. Manter a TV no app.

Estado: auditoria inicial e coletor concluídos. Políticas novas de exclusão/troca de link/liberação por uso ainda não implementadas no APK; serão parte da correção, com testes de integridade e de propriedade de dados.
