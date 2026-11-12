# Validação r22

Os dois fontes finais foram comparados byte a byte com a r20 validada: iguais. Restabelece consultas e opções pt-BR, en e es, mantendo a rejeição de português genérico no SubSource. Sem nova compilação de código idêntico.

Testes dos scripts passaram: aplicação sobre r21, reaplicação, restauração, recusa de base divergente e recusa de desfazer sobre edição posterior. Executados em PowerShell 7 e com simulação do formato de JSON do Windows PowerShell 5.1. Todos os scripts passaram no parser; diagnóstico aceita somente os campos permitidos.

A r20 passou em compilação Kotlin e lint, com 29 verificações de sincronização e 39 do SubSource. Contratos simulados; a API real não foi acessada e o resultado precisa ser confirmado na TV.
