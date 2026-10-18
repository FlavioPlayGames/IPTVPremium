# Comparação temporária dos vínculos entre listas — 1.06 / interno 8

Não é uma correção confirmada do carregamento: é um teste de isolamento. Preserva as correções cumulativas de episódios e espera de artes, sem mudar consultas, IDs, metadados ou prioridade das imagens.

O modo Pausados interrompe o efeito que reconcilia favoritos/progresso e enriquece identidades de filmes e nomes alternativos em segundo plano. O processo é encerrado e reaberto após instalar por cima: os mapas de associações em memória começam vazios. Assim, o histórico e os favoritos exibidos ficam restritos à própria lista. Nenhuma identidade, favorito, progresso, lista, cache, chave ou configuração privada é apagada. Não é necessário marcar/desmarcar favoritos durante a comparação. O modo Ativos recompila o mesmo código com o comportamento normal e recalcula os vínculos existentes.

A opção de diagnóstico só existe em debug. Sem a propriedade de compilação, o comportamento é normal; release sempre mantém o compartilhamento. Não se grava preferência permanente de diagnóstico nem alteração em gradle.properties/local.properties para essa opção.

## Executar no PowerShell

Preparar-Series-Vinculos-1.06.ps1 -Projeto CAMINHO -CompararVinculos -ModoVinculos Pausados -Emulador emulator-5554

O script confere/aplica o pacote cumulativo, compila, instala por cima somente no emulador, encerra o processo e abre o aplicativo com logcat filtrado. Não desinstala nem limpa dados/cache. O modo pausado é recusado sem -CompararVinculos para evitar gerar por engano APKs de diagnóstico destinados a publicação. Não publique esses APKs.

1. Com Pausados, abra a mesma série pela categoria/busca, aguarde e anote resultado/tempo. Evite trocar lista ou modificar favoritos no meio da comparação.
2. Ctrl+C encerra somente a coleta; não reativa o compartilhamento.
3. Rode o mesmo comando com -ModoVinculos Ativos. Abra novamente a mesma série e compare também os atalhos de favoritos/continuar, quando disponíveis.
4. Envie os dois arquivos diagnostico-series-vinculos-pausados-logcat.txt e diagnostico-series-vinculos-ativos-logcat.txt, salvos na raiz do projeto.

Os caches de episódios e imagens permanecem: a segunda abertura pode ser mais rápida por cache, portanto diferença de tempo isolada não prova causa. Os logs distinguem origem=disco, origem=rede e o total da abertura; uma comparação de rede precisa verificar essas origens. Erro na primeira execução e sucesso em cache na segunda não demonstra interferência por si só. Não se limpa o cache como parte deste teste.

Durante Pausados, atalhos importados de outras listas podem desaparecer temporariamente. Reativar recalcula as associações. Os favoritos e o histórico originais continuam armazenados. Mudanças que você fizer localmente continuam sendo salvas e poderão ser compartilhadas quando reativar.

## Segurança do instalador

Cumulativo com 26 entradas (17 arquivos e nove exclusões antigas já autorizadas). Na versão r5, muda somente app/build.gradle.kts e AppNavigation.kt. Pré-verificação integral, backup, rollback e rejeição de fontes desconhecidos. -Verificar não compila, instala ou coleta logs. -NaoCompilar aplica fontes, mas não muda o modo do APK já instalado.

O teste de comportamento no seu provedor precisa ser executado no emulador local. O teste externo desta sessão retornou HTTP 403 para ambos os User-Agents; não confirmou hipótese de User-Agent.
