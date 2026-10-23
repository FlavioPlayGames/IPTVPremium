# Validação — correção Xtream T1.2

Compilação de verificação em cópia isolada do projeto Android. Os recursos gráficos dessa cópia incluem substitutos; não distribuir os APKs desta cópia. O instalador gera os APKs com os recursos reais do projeto do usuário.

19 cenários do importador passaram. SQLite/FTS e instalador passaram; compilação Kotlin, assembleDebug e lintDebug concluídos com sucesso (sem erros de lint; avisos não bloqueantes permanecem). Os APKs de verificação incluem variantes por ABI e universal.

## Importação

Harness JVM externo, executando as fontes de produção do importador/HTTP/diagnóstico compiladas em conjunto e os DAOs de produção do build como dependência. Ponte Android JsonReader→Gson apenas no harness, sem inclusão no aplicativo. DAOs de armazenamento falsos executam as funções transacionais padrão de produção; o SQL real é verificado separadamente em SQLite.

Cobre streaming de 100 mil itens, primeiro lote antes do término da resposta, gravações incrementais, reordenação, exclusões apenas depois de resposta válida, falhas de autenticação/gravação/parsing, cancelamento na troca de lista, reaproveitamento persistente e os cenários M3U já existentes.

Novos cenários reproduzem categoria parada na abertura, APIs de categorias inválidas com itens válidos, categorias vazias anunciadas sem consultas extras, erro de abertura sem revelar credenciais, prazo de categorias com fallback e prazo de autenticação convertido em erro visível.

## SQLite / FTS

Revalidados o esquema gerado pelo Room, consultas reais de poda e rollback transacional, cascata, isolamento por lista e triggers FTS de inserção, substituição, renomeação e exclusão.

## Instalador

PowerShell real sobre cinco bases conhecidas, inclusive T1.1: aplicação, reaplicação idempotente, BOM/CRLF, recusa de conflitos antes de alterar arquivos, reversão byte a byte, exclusão apenas de novos fontes e preservação de configurações privadas.

## Limite

Não houve teste físico na TV nem acesso ao provedor do usuário. A correção remove problemas comprovados de controle de fluxo, mas a causa exata do erro relatado continua dependente da mensagem na TV. A abertura da Home antes não equivale a concluir o download do catálogo; verificar também contagens, entrada das seções e navegação. O banco existente do teste T1.1 é preservado.
