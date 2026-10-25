# Correção exclusiva da importação

Base: java.zip enviado nesta solicitação. Dois arquivos existentes e um novo auxiliar. Todos os demais fontes foram comparados byte a byte e preservados. Não inclui versão, Gradle, recursos, temas, navegação ou player.

## O que muda

1. A leitura/conversão Xtream continua progressiva, com o JsonReader novo já presente no projeto e fallback compatível preservado. Enquanto um lote é gravado, o leitor pode preparar o próximo. A fila permite apenas **um lote aguardando** e um único gravador, com contrapressão. Não baixa o JSON inteiro para a RAM nem abre consultas de catálogo adicionais.
2. Mantém tamanho de lote, ordem, primeira/última ocorrência, categorias, URLs e regras da geração temporária. O retorno de sucesso espera a última gravação. Falhas de escrita, parser e cancelamento continuam abortando a geração; não publica catálogo parcial.
3. M3U usa o mesmo leitor HTTP de importação que já possui tratamento de gzip, timeout de conexão/leitura, desconexão no cancelamento e diagnóstico de cabeçalhos. Adiciona progresso agregado por linhas, sem URL, títulos, usuário ou senha nos novos registros. O caminho get() dos detalhes não muda.
4. Mantém o formato escolhido: não converte Xtream em M3U, nem o inverso. Não mexe nas tentativas, transações, índices ou políticas de limpeza existentes.

A fila pode manter um lote em gravação, um aguardando e outro em construção; os limites por lote existentes continuam valendo. Não é promessa de tempo fixo ou de atingir 4 segundos na TV.

## O que a captura mostra

A captura inclui duas importações Xtream completas com 44.724 filmes e 20.170 séries: 22,9s e 95,2s. Na segunda, os filmes levaram 86,7s; o contador de leitura de rede acumulou 72,8s, contra 0,45s na primeira. Processamento e gravação ficaram próximos. O tempo máximo de uma leitura foi 223ms: não foi uma única leitura presa por mais de um minuto. A fila não garante eliminar variação do provedor/rede.

A terceira operação é M3U nativa e ainda estava ativa ao encerrar a captura; não há evidência para afirmar que falhou ou travou. O progresso novo distingue espera de cabeçalho e linhas efetivamente entregues, e a conexão passa a ser interrompida ao cancelar. Não publicamos a captura bruta, credenciais ou informações do aparelho.

## Aplicar

Baixe `Corrigir-Importacao.ps1` e execute na raiz do projeto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Corrigir-Importacao.ps1 -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw "Atualização interrompida." }
.\gradlew.bat assembleDebug
```

O instalador verifica os arquivos antes de escrever, aceita LF ou CRLF/BOM, cria backup, restaura em falha de escrita e pode ser executado novamente. Se a base estiver diferente do ZIP, recusa sem sobrescrever suas mudanças.

## Validar na TV

Repita uma importação com a mesma lista. Verifique contagens, sucesso, `filaLotes=1`, gravação e tempo total. Teste cancelar uma importação mantendo disponível o catálogo anterior. Para M3U, observe `READER_HEADERS`, `STREAM_PROGRESS` e `STREAM`. Não é necessário limpar os dados do aplicativo. Os tempos de testes em computador não são uma medição na sua TV.
