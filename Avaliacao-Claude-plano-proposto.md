# Avaliação do relatório do Claude — proposta para aprovação

Relatório recebido: “IPTVPremium – Comparação entre versões e pontos de atenção.pdf”. Conferido com o código do ZIP28 e as correções autorizadas posteriores. Nenhuma otimização de desempenho foi aplicada nesta etapa.

## Constatações

1. Xtream: o caminho normal autentica e busca seis endpoints de catálogo em sequência. Falhas de JSON podem gerar requisições adicionais por categoria/repetição. Há Strings e JSONArray completos, com pico de memória. A sequência é intencional para reduzir a coexistência de grandes respostas; paralelizar tudo pode piorar travamentos.
2. M3U: já existe caminho de streaming para Room em M3uCacheImporter/M3uSourceSynchronizer. Não é correto aplicar ao M3U toda a afirmação de que a gravação só começa após baixar todos os catálogos. O importador usa lotes de 250 e chamadas separadas por DAO no flush.
3. Room: não há setJournalMode explícito. Isso não prova que todo aparelho usa TRUNCATE: AUTO pode selecionar WAL. Confirmar o modo real com PRAGMA journal_mode no aparelho. WAL pode aumentar arquivos auxiliares; precisa de orçamento de disco e política de checkpoint.
4. Parental: há COALESCE/NULLIF e IN/NOT IN nas consultas. O filtro normalizado pode impedir uso direto de índice nessa expressão; outros índices ainda podem ser usados pelo SQLite. A lentidão não está demonstrada sem medir e examinar EXPLAIN QUERY PLAN. categoryKey é proposta com migração, não correção para aplicar às cegas.
5. Atualizações: política atual fornece uma URL/hash/tamanho por versão, sem seleção por ABI. É válido incluir variantes no portal/backend e no app. Usar as ABIs aceitas pelo Android instalado (SUPPORTED_ABIS), não inferir pela CPU. Preservar universal como alternativa e verificações de versão, pacote, hash e assinatura. Manter arm32.
6. Player: os dois pontos conferidos usam ExoPlayer.Builder sem LoadControl/renderers/HTTP customizados. Isso não demonstra defeito: customização só deve responder a uma falha medida. Buffers maiores consomem RAM, e software upscale pode aumentar uso de CPU/GPU. Upscale não é prioridade para carga de listas.
7. Portal/push/identidade: o próprio relatório diz que não analisou essas rotinas. Elas são candidatas à medição, sem diagnóstico de gargalo já confirmado. Não aumentar consultas periódicas para corrigir carga lenta.
8. Armazenamento: MoviePosterLoader permite 250 MiB de capas em disco, além de cache RAM separado. O limite do cache não é o consumo atual, mas conflita com a meta de instalação total de 300 MB quando somado ao app e ao catálogo. Dividir APK não limita os dados do usuário.

## Ordem proposta, ainda não aplicada

1. Medir antes/depois em Android 12, celular e TV/box representativa: tempo de inicialização, primeira carga e sincronização M3U/Xtream, etapas de rede/parse/gravação, pico de memória e crescimento em disco. Registrar só métricas e contagens; nunca URLs com credenciais, PIN ou tokens.
2. Definir orçamento de disco por componente (app, banco, capas e temporários), ajustar retenção de gerações/cache e limpeza segura. Não apagar listas, favoritos, histórico ou credenciais. O orçamento depende do APK instalado e do tamanho dos catálogos; 300 MB não é garantia sem limitar também dados variáveis.
3. Melhorar gravação: transação por lote, testar tamanhos e verificar WAL/checkpoint no aparelho. Preservar a troca de geração somente após sucesso; uma carga interrompida não pode destruir o catálogo anterior. Não colocar toda a importação de rede em uma única transação longa.
4. Xtream: parsing streaming com recuperação compatível e concorrência limitada/adaptativa, somente após autenticação e respeitando categorias/dependências, timeout, limites do provedor e RAM. Comparar com o fluxo sequencial.
5. Atualizador por ABI: alterar contrato do portal/backend e app em conjunto; manter universal, arm32, versões e assinatura compatíveis.
6. Parental/player/startup: medir; otimizar consultas ou buffer/rede apenas onde o resultado justificar. Qualquer migração preserva regras e dados existentes. Deixar upscale e novos renderers para avaliação separada.

Critério de aprovação: redução medida de tempo/armazenamento, sem perda de conteúdo, aumento de travamentos, quebra do parental, identidade, renovação, push ou atualizações. Não há percentuais de ganho prometidos sem testes. Alterações visuais continuam exigindo aprovação prévia do usuário.
