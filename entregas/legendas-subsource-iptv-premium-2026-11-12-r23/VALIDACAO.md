# Validação r23

Fonte da correção: SubSource-idiomas.txt enviado pelo usuário. O mapa público do site e as legendas da temporada 5 contêm brazilian_portuguese; a consulta anterior usava brazillian-portuguese. Não se presume que o endpoint interno do site seja o mesmo da API pública. O novo código precisa ser confirmado em uma consulta autenticada na TV.

compileDesempenhoKotlin passou nos fontes finais em 13 segundos. 29 testes de sincronização/idiomas/persistência e 39 verificações SubSource passaram com contrato simulado, incluindo consulta pelo código novo, resposta com brazilian_portuguese, manutenção de inglês/espanhol, rejeição de português genérico, paginação, packs e isolamento do cache de episódios.

Aplicação/reaplicação/restauração e guardas de hashes passaram no PowerShell 7 e em simulação do formato JSON do Windows PowerShell 5.1. Scripts passaram no parser e o coletor rejeitou campos extras privados. Sem Windows real e sem acesso à API real neste ambiente. Não foi repetido o lint completo para a alteração de constante/namespace; o lint da base r20 passou sem erros.

Dois arquivos Kotlin alterados; nenhuma mudança no site, player, ajuste manual, assinatura, regras de importação ou orçamento de cache. O auxiliar de sincronia não está incluído.
