# Protótipo independente de importação M3U

## Escopo e condição de aprovação

Foi implementado um construtor novo de catálogo, separado da rotina M3U atual e do banco compartilhado. A entrega é um experimento executável pelo ADB na variante desempenho. A integração dos menus normais fica para depois da medição no aparelho. Não há resultado de tempo na TV nesta entrega, nem confirmação da meta de 25 segundos.

Todos os arquivos do pacote são novos. Não há alteração de código, configuração ou dados do importador/sincronizador Xtream. Nenhuma modificação é aplicada ao banco playlist_cache.db. A entrada de teste lê somente a lista ativa cadastrada como M3U e baixa sua URL original; não chama a API Xtream ou o detector M3U→Xtream.

## Arquitetura construída

1. Seleção e prioridade: a atividade de teste lê uma emissão consistente de PlaylistStorage.selection. Somente uma construção do protótipo pode executar por vez. A espera de prioridade e a leitura da seleção entram no tempo total da atividade.

2. Download: o cliente HTTP existente é usado somente como transporte de linhas para um arquivo privado. O arquivo é normalizado em UTF-8 e sincronizado. SHA-256 da fonte normalizada é registrado sem revelar o link. Não há transação SQLite aberta nessa etapa.

3. Preparação: o parser independente lê o arquivo incrementalmente e escreve registros tipados em prepared.bin. Filmes, canais e episódios não são acumulados em listas do catálogo. Categorias e metadados finais das séries ficam em mapas: a memória desses mapas cresce com as categorias/séries, não com todos os episódios. Os últimos metadados da série prevalecem, e sua primeira ordem é mantida.

4. Banco privado: um Room novo é aberto exclusivamente em files/m3u_snapshot_prototype/<run>/snapshot.db para criar o esquema compatível. Nunca é usado getInstance(), que abre o banco normal. Os índices secundários são removidos somente desse banco novo e vazio; as chaves primárias e a política REPLACE permanecem.

5. Carga: instruções preparadas são reutilizadas durante toda a carga, com todos os parâmetros vinculados. Um único escritor faz uma transação maior no arquivo privado. Ela lê somente os registros binários já preparados; o download e o parsing M3U já terminaram. A leitura binária continua incremental e tem custo incluído no tempo. A transação não bloqueia o banco usado pela interface.

6. Índices: os índices secundários completos são criados ao final em uma transação própria. Não se anuncia conclusão antes desse trabalho. A combinação é deliberadamente diferente das tentativas anteriores de ajustar 250/500/2.000 registros no banco compartilhado.

7. Validação: são conferidos quick_check, contagens, vínculos de episódios/séries e categorias. Os contadores diferenciam registros processados e linhas únicas armazenadas, pois duplicidades são resolvidas pela mesma chave e último valor. A linha de identidade do Room não é simplesmente confiada: o protótipo remove a tabela de identidade SOMENTE do seu arquivo privado, reabre pelo Room e força a validação real do esquema, que recria a identidade. Nada disso é feito no banco normal.

8. Finalização: o WAL é explicitamente checkpointado após a validação do Room. O arquivo principal é sincronizado; fonte e spool temporários são removidos. Os custos de commit, índices, validação, checkpoint e limpeza fazem parte da medição.

9. Publicação do protótipo: um AtomicFile por playlist aponta para o arquivo validado. A referência anterior é conservada enquanto o novo arquivo é construído. Erro/cancelamento antes da publicação descarta só a sessão privada incompleta. Depois de iniciar uma publicação incerta, o candidato é preservado para não deixar uma referência potencialmente válida sem arquivo. O próximo teste limpa sessões abandonadas apenas se conseguir ler as referências existentes.

O diretório e a referência pertencem somente ao protótipo. A limpeza do arquivo anterior funciona aqui porque os leitores de teste já foram fechados. Antes de integrar aos menus normais, será necessário gerenciar o tempo de vida dos leitores e rotear somente os catálogos M3U correspondentes; não basta substituir o arquivo de um Room aberto.

## Compatibilidade dos dados

O pipeline de construção e persistência foi refeito. Foram mantidas as regras conhecidas de identificação/classificação do conteúdo para não reinventar os IDs: M3uEntryMetadata e M3uEntryClassifier são usados sem modificações, e as regras de título, ano, temporada/episódio e UUID são compatíveis com o importador nativo existente.

Os índices/tabelas correspondem ao esquema gerado pelo Room versão 5. Não se altera a versão de Kotlin, Gradle, Room ou o esquema normal. As relações, nulidade, parâmetros SQL, ordem do provedor e última ocorrência foram comparados com a referência. Integração de EPG, consumidores de histórico e navegação continua pelo caminho normal atual; este protótipo não muda seus dados.

## Medição

IPTV_SNAPSHOT registra início/fim/duração por etapa e progresso a cada 5.000 registros. O campo stage=commit é a duração do endTransaction da carga e está contido em load. Não some commit a load novamente. Bootstrap, remoção de índices privados, índices, validação e checkpoint também têm seus custos incluídos.

stage=summary apresenta o tempo interno do construtor, contagens e tamanho do arquivo pronto. stage=complete é o término do teste de sucesso e inclui a leitura da seleção e espera de prioridade. O coletor só encerra o sucesso em complete. Não se usa uma tela concluída enquanto o trabalho é adiado para o fundo.

O modo de journal e synchronous do escritor privado são consultados dentro de uma transação para conferir as condições efetivas. Não se usa synchronous=OFF, journal=OFF, allowMainThreadQueries, migração destrutiva ou GlobalScope. O cancelamento é estruturado, verificado durante a leitura/carga, com rollback do arquivo privado.

O SHA-256 permite reconhecer fontes normalizadas iguais entre rodadas. Os tempos reais ainda precisam ser obtidos na mesma TV. A preparação em disco acrescenta leitura/escrita deliberadamente; esse custo não foi escondido e pode tornar a abordagem insuficiente para 25 segundos. Se isso ocorrer, as etapas indicam o gargalo real desta arquitetura.

## Validação local

17 verificações passaram usando SQLite real via JDBC e o esquema gerado do Room: comparação de todos os campos/IDs/ordem com o importador de referência em quatro conjuntos, séries intercaladas e capas variáveis, duplicidades e última ocorrência, XUI/títulos especiais, índices compatíveis com o validador gerado, cancelamento com rollback, arquivo preparado truncado, registro inválido e fonte inválida.

Um teste funcional incremental com 329.732 entradas passou com JVM limitada a -Xmx128m. Foram conferidos 104 categorias, 2.355 canais, 44.099 filmes, 9.800 séries e 283.278 episódios persistidos. Houve uma gravação final por série e duas transações na parte carga/índices. Isso é teste de volume e memória de heap no ambiente de desenvolvimento, não uma medição de RAM total ou velocidade da TV. Não inclui HTTP real, driver Android, validação Room por abertura ou publicação Android.

Os scripts de aplicação e restauração são testados com arquivos novos, reaplicação, rejeição de conflito/corrupção e conflito ao desfazer. O coletor é testado com ADB simulado para lançamento, captura, privacidade, limpeza e espera pelo complete em vez de encerrar no summary intermediário. Os logs completos de validação estão em validacao/.

A execução Android de download/construção/publicação e a recuperação por interrupção física do aparelho ainda não foram testadas na TV neste ambiente. A geração do APK e o Lint são verificados na entrega; não substituem esse teste real.

## Arquivos acrescentados

Dentro de app/src/main/java/br/com/flaviotorres/iptvpremium/data/m3usnapshot/: SnapshotParser.kt (preparação incremental e consolidação de séries), SnapshotLoader.kt (carga SQL, índices e validação), SnapshotSchema.kt (contrato do esquema), SnapshotRunner.kt (etapas/arquivo/pointer/cancelamento) e SnapshotPrototypeActivity.kt (execução de teste e limpeza). A atividade é registrada somente em app/src/desempenho/AndroidManifest.xml, protegida por android.permission.DUMP, sem entrada no launcher.

A versão de teste usa o mesmo pacote/assinatura local da variante desempenho já existente. Nenhum arquivo normal de Xtream, importador M3U antigo, DAO ou banco compartilhado é sobrescrito pelo instalador deste protótipo. O pacote contém somente acréscimos, conferidos por hashes e com rollback.

## Próximo teste

Aplicar o pacote, compilar/instalar a variante desempenho e executar Testar-Prototipo-M3U.ps1 com a mesma lista M3U ativa usada nos diagnósticos. O teste fecha o aplicativo antes de abrir o experimento. Não é necessário copiar a URL ou credenciais. Fique na tela de teste e envie o TXT gerado. O modo -Limpar remove somente os artefatos do protótipo quando o experimento terminar.

## Verificações finais da entrega

A geração completa do APK desempenho e o Lint terminaram com BUILD SUCCESSFUL. Foram executados novamente os 17 testes de paridade/falhas e os 17 cenários legados de sincronização/armazenamento. O APK gerado contém as etapas finais complete/sourceHash e a limpeza. O manifesto mesclado confirma que a atividade é protegida por DUMP e não tem launcher. Os arquivos compartilhados conferidos contra a entrega anterior permanecem iguais. Nenhum tempo da TV foi inventado ou extrapolado do teste de volume.
