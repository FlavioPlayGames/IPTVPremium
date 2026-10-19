# Comparação de WAL, synchronous=NORMAL e lotes — 1.07

Comparador preparado para **emulator-5554**, como solicitado. Não exige conectar a TV e não afirma ganhos ainda não medidos. As referências da lista real no emulador são M3U 95,04 s (77,51 s em chamadas de gravação) e Xtream 22,16 s (11,38 s em chamadas de gravação).

## Perfis

| Perfil | Journal solicitado | synchronous solicitado | Lotes |
|---|---|---|---|
| ATUAL | Automático do Room, configuração original | Padrão do Android | 250 no produtor Xtream e M3U; gravador Xtream mantém limite interno anterior de 500 |
| WAL | WAL explícito | Padrão do Android | Iguais ao ATUAL |
| NORMAL | WAL explícito | NORMAL, na conexão de escrita | Iguais ao ATUAL |
| LOTES | WAL explícito | NORMAL | Até 2.000 itens por coleção/lote e limiar adicional de texto de 2 Mi caracteres |

Na prática, o Android pode já usar WAL/NORMAL em ATUAL. Os valores efetivos são registrados em DbPerf; não assumimos que mudar o builder trará ganho. `synchronous=1` significa NORMAL, `2` FULL, `0` OFF e `3` EXTRA. OFF não foi oferecido. A consulta dos valores é feita em transação somente de leitura de metadados, fixando a conexão de escrita, para não confundir uma conexão de leitura do pool com o escritor.

A configuração experimental existe só em debug. Release ignora a preferência e mantém ATUAL. A primeira instalação desse comparador fica em ATUAL; um perfil escolhido depois permanece até trocar novamente. Não há adoção automática do perfil aparentemente mais rápido.

Os lotes maiores atuam no produtor Xtream e no gravador: aumentar só o limite de 500 do gravador não mudaria os blocos de 250 vindos do parser. Na M3U, mantém a transação por lote, com mais itens antes do flush. Não mantém a transação aberta durante download nem materializa o catálogo inteiro. O limiar de texto não limita o tamanho de um único item do provedor nem equivale a um teto de RAM; 350 MB totais ainda exigem medir memória do processo. Não aumenta o cache de imagens, o limite do player ou a concorrência de rede.

A leitura Xtream verifica cancelamento também antes e depois de emitir cada lote. Cancelamento/falha continuam descartando somente a geração incompleta. Não foram alterados índices, esquema/migrações, endpoints paralelos, fila de rede, licença, histórico ou velocidade aprovada da rolagem.

## Primeiro passo: instalar e executar o teste automático no emulador

Não envie lista durante o teste automático. Ele usa bancos temporários separados e URLs fictícias sem rede. O aplicativo e os APKs de testes foram compilados aqui, mas **os testes SQLite/Android não foram executados nesta nuvem**, que não tem emulador. Por isso o primeiro comando abaixo executa a validação no seu emulador antes de escolhermos qualquer perfil para sua lista.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/perfis-importacao-1.07-20261019'
$arquivo = Join-Path $env:TEMP 'Preparar-Perfis-Importacao-1.07.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Preparar-Perfis-Importacao-1.07.ps1" -OutFile $arquivo
if ((Get-FileHash $arquivo -Algorithm SHA256).Hash -ne '6b01e978d23810bab24ba391dce494f9d9fe68785df8886c6ac8c1a98ca43fc9') { throw 'Download diferente do esperado.' }
& $arquivo -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador 'emulator-5554' -TestarBanco
```

O script valida hashes de todos os arquivos antes de gravar, faz backup e compila/instala por cima sem apagar dados. Inclui a atualização anterior Xtream com alternativa M3U para manter os arquivos dependentes compatíveis; não altera o site. Aceita as revisões anteriores conhecidas e o estado já aplicado; arquivos divergentes interrompem a aplicação para preservar alterações locais. `-Verificar` verifica sem escrever; `-SomenteAplicar` aplica sem compilar/instalar.

Resultado esperado: **OK (25 tests)**. Envie **diagnostico-perfis-importacao.txt**, salvo na raiz do projeto, inclusive se houver falha. Não publique APKs nem avance com perfis se o teste falhar. O teste pode levar alguns minutos.

O comparador Android faz duas rodadas com ordem invertida, 5.000 filmes fictícios por formato/perfil. Produz 16 linhas `BENCH_IMPORT rodada=... perfil=... formato=... itens=5000 totalMs=... journal=... synchronous=... lote=...`. Mede parser, mapeamento e persistência, sem rede real. Na segunda rodada, simula IOException e cancelamento depois de gravações parciais: a geração ACTIVE deve continuar intacta e não deve sobrar uma geração READY da tentativa falha. Fecha e reabre o banco e confere os 5.000 itens. Cada banco é eliminado ao final do respectivo caso; os bancos e as listas reais do usuário não são usados nesse teste.

As duas rodadas reduzem alguns efeitos de ordem, mas continuam sendo uma comparação pequena de dados sintéticos: JIT, cache do sistema, checkpoint e variações do emulador podem alterar os números. Não comprovam ganho na lista real. O teste não simula corte de energia nem garante durabilidade contra falha física do armazenamento.

## Depois do teste automático: medir com a lista real

Os scripts ficam instalados também em `entregas-locais\perfis-importacao-1.07` dentro do seu projeto. Se os 25 testes passarem, começaremos por ATUAL para registrar o modo real do banco da lista existente. Execute quando nenhuma outra importação estiver em andamento:

```powershell
Set-Location 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\entregas-locais\perfis-importacao-1.07\Testar-Perfil-Importacao-1.07.ps1' -Perfil ATUAL -Formato M3U
```

O controlador fecha o processo, aplica a preferência pelo APK de testes, fecha esse processo de teste e inicia a captura **antes** de reabrir o app. Quando aparecer “Captura iniciada”, reenvie sua lista pelo site em **M3U direta — sem conversão**, já disponível no administrador. Não precisa apagar nem criar uma nova lista: edite/reenvie a existente.

Arquivo: `diagnostico-importacao-m3u-atual.txt`. Ele contém DbPerf com os valores efetivos e as métricas da importação. O parâmetro Formato só rotula a medição, não converte a lista. Para Xtream, selecione o envio automático/Xtream no site; confirme `tipo=XTREAM` no resultado.

Os outros perfis usam o mesmo comando, trocando apenas `-Perfil WAL`, `-Perfil NORMAL` ou `-Perfil LOTES`. Cada arquivo tem formato e perfil no nome e não substitui o de outro perfil. Começaremos com os resultados do teste automático e ATUAL, antes de pedir todas as oito importações reais. Será possível identificar quando WAL e NORMAL já equivalem ao estado atual e evitar repetições sem utilidade.

Para voltar à configuração original, sem iniciar uma captura:

```powershell
& '.\entregas-locais\perfis-importacao-1.07\Testar-Perfil-Importacao-1.07.ps1' -Perfil ATUAL -SomenteConfigurar
```

O aplicativo fica fechado após SomenteConfigurar; abra-o normalmente no emulador. Nenhum comando limpa dados/cache ou desinstala aplicativos. O controlador mantém o perfil selecionado entre aberturas até você trocá-lo. Se um perfil não permitir abrir o banco, ainda é possível configurar ATUAL: o método que grava a preferência não abre o banco real.

## NORMAL e segurança do catálogo

O ajuste é somente no banco `playlist_cache.db`, de catálogo reimportável; não altera preferências de histórico, favoritos ou licença. WAL com NORMAL mantém as garantias de consistência do SQLite, mas transações recentes podem ser perdidas após queda de energia/reinicialização do sistema. Falha comum/cancelamento durante uma importação e reabertura normal são testados; não confundimos isso com um teste de energia cortada. O catálogo anterior continua protegido pela troca de gerações já existente. Nenhum perfil foi declarado vencedor antes das medições no emulador.

## Validação disponível nesta nuvem

- App e APK de testes Android compilados.
- 7 testes JUnit em JVM passaram: perfis isolados, release ignorando preferências experimentais, validação do modo, limites/contagem dos lotes, limiar de texto e cancelamento entre lotes.
- 7 cenários existentes de framing/compatibilidade JSON passaram com o parser compilado do aplicativo e implementação Android-compatible de JSON, incluindo caracteres escapados, leituras pequenas, respostas incompletas e limites.
- 25 testes foram compilados para Android (os 16 da entrega anterior e os 9 novos). Sua execução real fica para o emulador do usuário; não foi afirmado que passaram aqui.
- Controlador e medidor exercitados com ADB simulado: perfil confirmado, reinício/captura, DbPerf preservado, evento antigo excluído e URLs/segredos removidos do relatório. Instalador verificado/aplicado em cópia e todos os hashes conferidos.
- Nenhuma mudança de ambiente/configuração reutilizável foi necessária; a configuração existente de JDK/Android/Gradle foi reutilizada.
