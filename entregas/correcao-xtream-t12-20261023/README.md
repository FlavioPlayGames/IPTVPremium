# Correção Xtream — 1.12-T1.2

Corrige problemas encontrados na revisão após relato de lentidão e falha na tela inicial em lista Xtream. A causa exata ocorrida na TV ainda não foi confirmada, porque a mensagem anterior era genérica.

## Mudanças

- A Home deixa de depender do sucesso das três consultas de categorias. Após validar a conta e preparar o catálogo local, a Home abre; as seções continuam sendo importadas e exibem progresso/erro.
- Categorias são consultadas dentro da fase de cada seção, com prazo de 5 segundos. Se falharem, o importador reutiliza categorias locais ou cria rótulos `Categoria <id>` a partir dos itens recebidos. Isso não substitui validação da conta nem transforma resposta inválida em catálogo válido.
- Uma requisição de catálogo de cada vez, mantendo leitura em fluxo, lotes limitados, fila curta e gravação sobreposta ao parsing. Evita a rajada anterior de três requisições de categorias.
- Uma resposta completa válida encerra o download daquela seção; não consulta novamente cada categoria anunciada mas vazia. Recuperação por categoria fica restrita à falha real do endpoint completo.
- Estados das seções são preparados antes de liberar a Home, impedindo que um erro antigo apareça enquanto a nova tentativa ainda aguarda sua vez.
- A retomada automática aguarda a preparação inicial, evitando uma tentativa concorrente à que controla a tela de abertura.
- Validação inicial da conta limitada a 15 segundos. O esgotamento desse prazo vira erro visível, em vez de deixar a tela aguardando uma operação cancelada.
- Diagnóstico controlado na tela: DNS, TEMPO, SSL, ACESSO, HTTP, RESPOSTA, BANCO ou FORMATO. Sem endereço do servidor, usuário, senha ou corpo da resposta.

Cadastro pelo site, DNS configurável, M3U, Coil e FTS permanecem. Nenhum DNS é imposto. O banco continua `playlist_cache_native_test1_complete.db`, reaproveitando os dados do T1.1; este patch não força uma nova importação completa nem altera a assinatura.

## Aplicar e gerar APK

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/correcao-xtream-t12-20261023'
$script=Join-Path $env:TEMP 'Corrigir-Xtream-T1.2.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Corrigir-Xtream-T1.2.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Instale o APK de `app\build\outputs\apk\debug\app-universal-debug.apk`, confirmando **1.12-T1.2**. Não limpe os dados do aplicativo para aplicar o patch.

O instalador valida hashes antes de modificar arquivos, preserva configurações privadas e cria backup em `.native-xtream-fix-backups`. Aceita T1.1 e as bases conhecidas anteriores. Alterações desconhecidas fazem o script interromper antes da aplicação.

Para desfazer, baixe `Desfazer-Correcao-Xtream.ps1` desta pasta e execute com `-Projeto` e `-Testar`; depois instale novamente o APK gerado.

## Teste visual na TV

Verifique a entrada, a abertura de Filmes e a reabertura após fechar. A Home abrir antes não significa que todo o catálogo terminou: acompanhe a contagem das seções e a navegação. Se continuar falhando, a mensagem agora deve identificar a etapa e o tipo do erro, permitindo uma correção dirigida. Não é necessário conectar a TV ao computador.
