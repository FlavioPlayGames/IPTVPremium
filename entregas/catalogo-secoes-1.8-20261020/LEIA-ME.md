# IPTVPremium 1.8 — teste reversível de catálogo por seção

A Home deixa de esperar o catálogo inteiro na primeira importação. Ao entrar em Canais, Filmes ou Séries, a seção escolhida ganha prioridade. As outras seguem em segundo plano, com um único escritor e pequenas pausas entre lotes. Isso muda a espera para começar a usar o app; não é uma promessa de reduzir o tempo total de download/importação nem uma medição de desempenho na TV.

Mantém versão **1.8 / código 10**, Xtream **250**, M3U **2000**, identidade dos temas, grade de 8/10 capas, controle remoto e preparação de filmes por pelo menos três segundos. Não modifica o site nem pede acesso à TV. Nenhum APK/segredo foi publicado; os APKs são compilados e instalados localmente por cima.

## Caminhos de importação

- **Xtream:** autentica uma vez e importa canais, filmes e séries separadamente. A escolha de outra seção cancela apenas a etapa ainda não publicada, que pode ser retomada depois. Recuperações JSON por endpoint/categoria permanecem. Não há três downloads grandes simultâneos.
- **M3U direta:** faz um download em streaming para arquivos privados separados por tipo. Depois, importa primeiro o tipo escolhido. A lista não fica inteira na RAM e não é convertida no aplicativo. Como o servidor entrega um arquivo único, o primeiro download/separação ainda precisa terminar; esta etapa não pode buscar só um tipo no servidor. Os arquivos temporários são apagados ao terminar/cancelar e os de processo encerrado são removidos ao retomar. Reserva máxima de texto temporário: 512 Mi caracteres; não é teto de memória.
- **Alternativa Xtream → M3U:** preserva a importação completa alternativa já existente, em segundo plano, ativando-a apenas no sucesso. Cancelamento e erros de armazenamento não disparam download alternativo. Se o Xtream funcionar, não baixa M3U.
- **Catálogo já completo:** continua usando o cache existente. Não exige reenviar uma lista apenas para abrir o aplicativo. Uma solicitação explícita de atualização de uma lista já completa mantém a rotina integral existente; o novo caminho prioriza a primeira carga e a retomada de seções pendentes.

Cada seção é gravada numa geração temporária. A publicação e a marcação de conclusão ocorrem na mesma transação. Uma falha não publica os lotes parciais nem remove outra seção pronta. O banco passa de versão 4 para 5 com uma nova tabela de estado, sem apagar as tabelas existentes. Fechamento/forçar parada conserva as seções concluídas; a próxima abertura retoma as faltantes. A troca de lista interrompe o trabalho anterior; a exclusão interrompe o importador antes de remover seu cache.

A Home não fica bloqueada pela importação. Ao abrir uma seção ainda pendente, a tela temática existente aguarda essa seção; Voltar leva à Home. Erro oferece Fechar. Comandos de navegação/OK não acionam as capas ocultas durante a espera. O Paging só é iniciado para uma seção pronta; categorias, contagens e regras parentais são calculadas novamente quando ela conclui, sem expor o estado vazio inicial. O histórico aguarda um catálogo completo para não identificar favoritos/continuação com dados faltantes; sua execução permanece em segundo plano.

## Instalar e testar no emulador

Execute `Preparar-Catalogo-Secoes-1.8.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador emulator-5554 -TestarBanco` depois de baixar e verificar o SHA256 publicado junto do arquivo.

A opção `-MedirSecoes` (junto de `-TestarBanco` e `-Emulador`) inicia a captura e abre o aplicativo depois dos testes/configuração. Quando pedir, envie uma lista nova.

O instalador verifica hashes antes de escrever, cria backup local, mantém propriedades privadas e versão 1.8/código 10, compila, instala por cima e executa **38 testes Android**. Se falhar, envie `diagnostico-catalogo-secoes-testes.txt`; não publique APKs. Depois do sucesso, confirma PADRAO e ativa o modo Secoes. Não limpa dados, desinstala ou importa sua lista durante os testes.

Já importar uma lista completa significa que o próximo acesso usa o cache. Para avaliar esta mudança na primeira carga, envie **uma lista nova**, sem limpar os dados, e entre logo em Filmes ou Séries. Observe separadamente o tempo até a Home, até a seção desejada e até o fim de tudo. Não envie a lista durante os testes automáticos.

Para medir, na raiz do projeto:

```powershell
& '.\entregas-locais\catalogo-secoes-1.8\Medir-Catalogo-Secoes-1.8.ps1' -Dispositivo emulator-5554 -PastaSaida (Get-Location).Path -AbrirAplicativo
```

Quando a captura iniciar, envie a lista nova e escolha uma seção. Envie `diagnostico-catalogo-secoes.txt`. O medidor remove URLs/credenciais e não limpa logcat/dados. O resumo mede o catálogo inteiro; `abertura_concluida` mede a liberação da Home e os eventos `secao=... resultado=PRONTA` medem as seções. Não confundir tempo de gravação com disco puro nem heap Java com memória total. Ainda não foi demonstrado o teto de 350 MB de RAM na TV.

## Voltar ao comportamento anterior

Não é necessário restaurar APK antigo nem rebaixar o esquema do banco. O retorno é feito por uma configuração persistente:

```powershell
& '.\entregas-locais\catalogo-secoes-1.8\Configurar-Catalogo-1.8.ps1' -Modo Completa -Dispositivo emulator-5554 -PastaSaida (Get-Location).Path -AbrirAplicativo
```

Mantém listas, favoritos e histórico. Se existir uma carga incompleta, o modo Completa faz uma importação integral antes de liberar o catálogo, como antes. Para reativar, use `-Modo Secoes`. O controlador exige o APK de testes instalado e confirma a configuração; não declara sucesso só pela saída do ADB.

## Validação e limites

- Build debug, APK de testes e compilação Kotlin de release verificados nesta nuvem; nenhum APK de produção assinado foi publicado.
- 14 testes JUnit JVM do coordenador/endpoints: prioridade, cancelamento, retomada, duplicação, cache, uma única leitura M3U, episódios, isolamento de falha e alternativa. Usam as classes de produção com armazenamento simulado; não substituem os testes Room Android.
- 9 testes JVM de lotes/política e 7 cenários de framing/JSON Android.
- 38 testes Android compilados, incluindo 11 de seções: publicação, falha, prioridade, retomada, M3U, episódios, migração, conflito/transação e configuração. A execução real cabe ao emulador do usuário; não foi afirmada execução Android nesta nuvem.
- Aplicação/verificação do instalador e backup testados em cópia com propriedades fictícias. Controladores Secoes/Completa e captura testados com ADB simulado, incluindo sanitização e exclusão do último evento antigo.

Esta é uma experiência reversível autorizada pelo usuário. O objetivo de desempenho real será avaliado com o diagnóstico e a navegação no emulador; não se presume equivalência de velocidade entre emulador e TV.
