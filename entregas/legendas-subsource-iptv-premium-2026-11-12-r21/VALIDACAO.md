# Validação r21

compileDesempenhoKotlin passou nos fontes finais (12 segundos). 29 testes de sincronização/idiomas/persistência e 6 verificações específicas do SubSource passaram: consulta somente brasileiro, nenhuma consulta inglês/espanhol, rejeição de idiomas não brasileiros em resposta mista, aliases brasileiros reconhecidos e diagnóstico correspondente. API e preferências simuladas, sem acesso real ao provedor.

Aplicação/reaplicação/restauração e guardas de hash passaram em PowerShell 7 e com simulação do formato JSON do Windows PowerShell 5.1. Todos os scripts passaram no parser; o coletor preservou campos permitidos e rejeitou dados privados.

Não foi repetido o lint completo: alteração pequena no filtro de idioma, compilação e testes direcionados passaram. A base r20 foi validada com lint sem erros. A instalação na TV ainda precisa ser conferida. O ajuste manual e os outros provedores permanecem com o código anterior.
