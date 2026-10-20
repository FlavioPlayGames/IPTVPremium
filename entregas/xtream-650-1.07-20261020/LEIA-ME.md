# Teste Xtream até 650 — 1.07

Adiciona **XTREAM_650**, mantendo os perfis anteriores. Produtor e gravador Xtream passam a aceitar até 650 itens por lote; a M3U permanece até 2.000. Banco continua automático/padrão do Android, e o limiar de texto Xtream permanece 256 Ki caracteres, exatamente como em XTREAM_500. Isso evita mudar journal, synchronous e limite de texto junto com a quantidade de itens. Um bloco pode terminar antes de 650 por atingir o limiar de texto; não se trata de um limite de RAM.

Referência da lista real no emulador: 500 concluiu em **19,14 segundos**, com 43.569 filmes e 9.775 séries, sem recuperação por categorias ou alternativa M3U. 250 concluiu em 20,51 segundos. 1.000 terminou em 95,56 segundos com recuperação; 2.000 precisou da M3U alternativa. O objetivo é comparar 650 com 500 sem presumir ganho ou atribuir uma causa definitiva às falhas anteriores.

## Executar no emulador

Abra emulator-5554. Cole no PowerShell:

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/xtream-650-1.07-20261020'
$arquivo = Join-Path $env:TEMP 'Preparar-Xtream-650-1.07.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Preparar-Xtream-650-1.07.ps1" -OutFile $arquivo
if ((Get-FileHash $arquivo -Algorithm SHA256).Hash -ne 'ae52e884155eef7279bbc6d2263b52ab221ea31cd8995228633624a979dee484') { throw 'Download diferente do esperado.' }
& $arquivo -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador 'emulator-5554' -TestarBanco -MedirXtream650
```

O instalador confere hashes, faz backup, compila/instala por cima sem limpar dados e executa os **26 testes Android**. Depois de passarem, seleciona XTREAM_650, reinicia o processo e inicia a captura. Não envie listas durante os testes automáticos, que usam bancos temporários separados e dados fictícios.

Quando aparecer **“Captura iniciada”**, reenvie a mesma lista pelo site como Xtream ou M3U com conversão automática. Não use M3U direta para esta medição. Envie **diagnostico-importacao-xtream-xtream_650.txt**, salvo na raiz do projeto. Se os testes falharem, a captura não inicia: envie **diagnostico-xtream-650-testes.txt**.

Não precisa atualizar o site. O perfil persiste entre aberturas até ser trocado; não vira automaticamente padrão de produção. Release continua ignorando a preferência experimental e usando ATUAL. Os scripts atualizados também ficam em `entregas-locais\perfis-importacao-1.07` no projeto.

## Voltar a 500 ou repetir 650

Sem recompilar:

```powershell
Set-Location 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\entregas-locais\perfis-importacao-1.07\Testar-Perfil-Importacao-1.07.ps1' -Perfil XTREAM_500 -Formato XTREAM
```

Para repetir 650, troque o perfil acima por `XTREAM_650`. Para apenas configurar 500 e deixar o app fechado, acrescente `-SomenteConfigurar`. Nenhum comando apaga dados/cache ou desinstala o app. M3U segue em 2.000 nos dois perfis. O formato do nome do arquivo é rótulo: o resultado efetivo `formato=XTREAM`/`formato=M3U` e a presença de recuperações é que determinam como terminou a importação.

## Validação

App e APK de testes Android compilados. **8 testes JUnit JVM e 7 cenários de framing/compatibilidade JSON passaram**. O teste de independência de M3U inclui o perfil 650, e a leitura verifica contagem, limites e cancelamento nos perfis disponíveis. Os 26 testes Android foram compilados; execução real será no emulador do usuário, não nesta nuvem.

O comparador Android existente inclui automaticamente 650 nas duas rodadas de dados fictícios e nas verificações de falha, cancelamento e reabertura. Esses resultados sintéticos não substituem a medição da lista real. Não simula corte de energia.

Instalador verificado e aplicado em cópia da entrega anterior com hashes conferidos. Controlador e medidor exercitados com ADB simulado, preservando o perfil/tamanhos e o protocolo efetivo, excluindo evento antigo e removendo URLs/segredos. A compilação/instalação Windows e as métricas de SQLite real permanecem para o emulador.

Não modifica esquema, índices, reparo de JSON, retentativas, licença, histórico, site, rolagem, cache de imagens ou concorrência. Não declara o teto de 350 MB medido ou garantido por este teste. Nenhuma credencial real/provedor real foi usado nesta nuvem.
