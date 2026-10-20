# Xtream até 1.000; alternativa até 500 — 1.07

Atende à decisão de testar primeiro **1.000 no Xtream** e só depois mudar para **500**, se necessário. Ambos os perfis mantêm **M3U até 2.000** independentemente do Xtream. Não há redução automática para 500: a troca é explícita, sem recompilar. A importação por M3U alternativa já existente continua funcionando se Xtream falhar; não aceita catálogo incompleto como sucesso.

| Perfil novo | Xtream: produtor e gravador | Limiar de texto Xtream | M3U | Banco |
|---|---:|---:|---:|---|
| XTREAM_1000 | Até 1.000 | 512 Ki caracteres | Até 2.000; limiar de 2 Mi caracteres | Automático/padrão do Android, como ATUAL |
| XTREAM_500 | Até 500 | 256 Ki caracteres | Até 2.000; limiar de 2 Mi caracteres | Automático/padrão do Android, como ATUAL |

O limite é por coleção/bloco e pode ocorrer antes pelo limiar de texto. Um item individual pode exceder o limiar; ele não constitui teto de RAM do aplicativo. Não foram aumentados caches de imagens ou do player nem a concorrência de rede. A meta de 350 MB totais não foi declarada medida por estes testes.

O perfil ATUAL e os perfis anteriores continuam disponíveis para comparação. Os perfis experimentais seguem restritos ao debug: release ignora a preferência e usa ATUAL. Nada foi adotado automaticamente como padrão de publicação.

A referência de Xtream direto é **20,51 segundos** com 43.569 filmes e 9.775 séries. O teste com 2.000 falhou no Xtream e terminou via M3U, por isso não foi tratado como ganho de Xtream. A M3U em 2.000 completou em aproximadamente 68–70 segundos, contra 95 segundos antes. Ainda não se atribui a causa exata das falhas apenas ao tamanho de lote; vamos testar 1.000 nas mesmas condições. Nenhum resultado de velocidade foi prometido nesta entrega.

## Instalar, testar e iniciar a captura de Xtream 1.000

Abra emulator-5554. Execute o bloco abaixo no PowerShell. Ele compila/instala por cima, preservando dados/listas/histórico, executa os testes Android e só depois de passarem seleciona XTREAM_1000 e inicia a captura. Os testes automáticos usam bancos temporários e dados fictícios: não envie lista enquanto eles estiverem rodando.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/xtream-1000-1.07-20261020'
$arquivo = Join-Path $env:TEMP 'Preparar-Xtream-1000-1.07.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Preparar-Xtream-1000-1.07.ps1" -OutFile $arquivo
if ((Get-FileHash $arquivo -Algorithm SHA256).Hash -ne 'a864e1401b83c8abfd6497ed40021e65daa6294ae69bfd9773b3f3c2831d1948') { throw 'Download diferente do esperado.' }
& $arquivo -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador 'emulator-5554' -TestarBanco -MedirXtream1000
```

Esperado nos testes: **OK (26 tests)**. Se falhar, o script interrompe antes da seleção/captura: envie `diagnostico-xtream-1000-testes.txt`. Não publique APKs que falharem.

Quando aparecer **“Captura iniciada”**, reenvie a mesma lista pelo site como Xtream ou M3U com conversão automática. Para este teste, não selecione M3U direta. Envie **diagnostico-importacao-xtream-xtream_1000.txt** ao terminar. Ele fica na raiz do projeto, com os tempos, DbPerf e o formato realmente utilizado. Se cair na alternativa M3U, aparecerá `formato=M3U`; o rótulo do arquivo não muda o protocolo nem prova sucesso de Xtream.

Não é necessário atualizar o site novamente. O instalador atualiza também os dois scripts em `entregas-locais\perfis-importacao-1.07`, aceita os arquivos da entrega anterior de perfis, valida todos os hashes antes de gravar e faz backup. Não sobrescreve arquivos de revisões desconhecidas. Pode ser executado com `-Verificar` ou `-SomenteAplicar` para verificar/aplicar sem compilar. Sem `-MedirXtream1000`, não escolhe um novo perfil nem inicia captura.

## Trocar para 500, se o resultado de 1.000 não for satisfatório

Não precisa reinstalar nem recompilar:

```powershell
Set-Location 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\entregas-locais\perfis-importacao-1.07\Testar-Perfil-Importacao-1.07.ps1' -Perfil XTREAM_500 -Formato XTREAM
```

Depois de iniciar a captura, envie a mesma lista no mesmo formato. Resultado: `diagnostico-importacao-xtream-xtream_500.txt`. Esse perfil mantém a M3U em 2.000. Para repetir 1.000, use `-Perfil XTREAM_1000` no mesmo comando.

Para voltar à configuração original, sem captura:

```powershell
& '.\entregas-locais\perfis-importacao-1.07\Testar-Perfil-Importacao-1.07.ps1' -Perfil ATUAL -SomenteConfigurar
```

O app fica fechado após SomenteConfigurar; abra-o normalmente. Nenhum desses comandos limpa dados ou cache nem desinstala o aplicativo. O perfil selecionado permanece entre aberturas até você trocá-lo.

## Implementação e validação

- O produtor Xtream passa a emitir até 1.000/500 conforme o perfil; o gravador permite o mesmo limite. Apenas mudar o gravador não alteraria os blocos de 250 produzidos anteriormente.
- Tamanhos M3U são propriedades independentes, evitando reduzir a M3U para 1.000/500 ao selecionar o perfil Xtream. DbPerf revisão db-r2 inclui `loteM3u` e `limiteCaracteresM3u`, além dos limites do Xtream e valores efetivos de journal/synchronous da conexão de escrita.
- Não altera índices, esquema, migrações, recuperação de JSON, retentativas, formato dos cadastros, licença ou navegação.
- App e APK de testes Android compilados. 8 testes JUnit JVM e 7 cenários de compatibilidade/framing JSON passaram. Incluem a independência dos tamanhos M3U/Xtream, ignorar perfis em release e preservação dos itens/cancelamento.
- Os 26 testes foram compilados para Android; não foram executados nesta nuvem. O teste de banco inclui agora os novos perfis nas duas rodadas de 5.000 itens por formato, com falha/cancelamento e reabertura do banco. A execução real será feita no emulador do usuário.
- Instalador aplicado/verificado em cópia com todos os hashes conferidos. Controlador/medidor exercitados com ADB simulado, incluindo tamanhos separados, modo real preservado mesmo quando diverge do rótulo e remoção de URLs/segredos. O fluxo de compilação/instalação Windows e SQLite real fica para o emulador; não foi descrito como executado aqui.

O objetivo é carregar todo o catálogo em Xtream sem recorrer à alternativa e comparar tempo com ATUAL. Testes sintéticos isolados não substituem esse resultado com a lista real.
