# Validação r9

## Fontes e testes

- Base dos 306 arquivos Kotlin conferida byte a byte com o último java.zip enviado.
- 39 verificações JVM do fluxo de transferência: A/B/C/D/E, última posição, ausência e retorno de conteúdos/favoritos, exclusão de todos os catálogos, reinício do processo, migração v1, preservação do progresso de instalação antiga, remoções, perfis separados, troca de geração e worker de lista anterior.
- Séries testadas com IDs de série/episódio diferentes, temporada e episódio iguais e preservação da posição real.
- Suíte anterior: pesquisa global e limpeza da consulta, histórico/compaction (inclui 2.200 fatos), avanço de episódios, identificação das capas, reserva ordenada e limites/rotação de armazenamento.
- Fixture de 900 filmes: cinco páginas na primeira passagem, cinco após materializar os novos IDs; depois zero páginas na mesma geração estável e zero após simular reinício com correspondências persistidas. Isso mede chamadas ao DAO simulado, não tempo na TV nem desempenho de Room em hardware real.
- Aplicador: aplicação, repetição, restauração, arquivos novos/excluídos, recusa de base diferente e recusa de restaurar sobre edição posterior. PowerShell 7 real e simulação do retorno JSON do Windows PowerShell 5.1. Não houve execução em Windows nativo.
- Os arquivos de orçamento, rotação e reserva/prefetch ativos das capas permanecem byte a byte iguais à r8.

Os testes usam os fontes reais de histórico e políticas, com adaptadores de Context, SharedPreferences, DAO, dados de provedor/rede e bitmap onde necessário. Não são testes em uma TV física. O SDK/Gradle local é usado para a compilação Android separada.

O pacote contém fontes e scripts, sem APK, credenciais, listas de reprodução, chaves ou configurações privadas. O instalador usa a assinatura local do usuário.

Os ganhos de tempo e o tamanho instalado devem ser medidos na TV. Remover código já descartado por R8 pode ter ganho pequeno ou nulo no APK.

## Compilação Android final

`:app:assembleDesempenho :app:lintDesempenho --offline` concluídos com sucesso. Lint: zero erros, 153 avisos e seis sugestões. O host usou JDK 25/Gradle 9.6 e heap de 6 GiB por argumento de execução; a configuração de memória do projeto do usuário não foi alterada.

APKs gerados localmente para arm64-v8a, armeabi-v7a, x86, x86_64 e universal. Comparação com r8 no mesmo ambiente: redução de 49.152 bytes por APK (arm64: 97.064.646 → 97.015.494 bytes). Essa redução é pequena; a otimização principal é eliminar trabalho repetido do histórico. Nenhum APK do host será distribuído, para preservar a assinatura local do usuário.
