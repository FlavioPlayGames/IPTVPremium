# Capas por janela, prioridade na abertura e favoritos discretos

Atualização baseada no **IPTVPremium (8).zip restaurado**, enviado nesta solicitação. Não inclui os efeitos de upscale/nitidez, shaders, dependências ou mudanças no player das atualizações anteriores.

## Comportamento

- **Cinco colunas:** duas fileiras visíveis, com até 10 capas. Antecipa somente as próximas duas fileiras, com até outras 10 capas.
- **Quatro colunas:** duas fileiras visíveis, com até 8 capas. Antecipa somente as próximas 8.
- A janela acompanha a primeira fileira visível. Por exemplo, em cinco colunas: itens 0–9 visíveis e 10–19 antecipados; ao avançar uma fileira, 5–14 visíveis e 15–24 antecipados. Não baixa um terceiro grupo nem tenta carregar o catálogo inteiro.
- As capas visíveis mantêm o carregamento normal; a antecipação guarda os arquivos comprimidos em disco, sem decodificar bitmaps adicionais. A lista de dados do Room/Paging pode conter mais registros, mas esses registros não disparam downloads de capas fora das duas janelas.
- Ao abrir um título, as tarefas de capas visíveis e antecipadas daquele catálogo são canceladas **antes** da chamada de navegação. As conexões registradas recebem fechamento em uma tarefa de IO, sem bloquear a interface. Novos pedidos desse catálogo ficam suspensos até o retorno à tela.
- Não há mais antecipação de detalhes, backdrops, logos ou episódios de outros títulos a partir do catálogo. A abertura carrega o item selecionado. As outras capas da animação de abertura reutilizam somente imagens já disponíveis em RAM.
- Filmes e séries começam com foco na seleção de categorias. Ao retornar dos detalhes, preservam a categoria, a posição e o foco do card anterior.
- O coração sai da arte e fica pequeno, fino e vazado ao lado dos metadados/nota. Permanece vazado também nos favoritos, com a cor de destaque indicando esse estado. O toque no coração e o comando de manter OK pressionado continuam funcionando.

A regra usa o catálogo real paginado de filmes e séries, incluindo Favoritos e Continuar assistindo. Os caches existentes continuam disponíveis; não há limpeza de capas nem mudança na importação de playlists.

## Atualizar pelo PowerShell

No Android Studio, abra o terminal PowerShell **na raiz do projeto restaurado**, onde está `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Capas-Prioridade.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/capas-janela-prioridade/Atualizar-Capas-Prioridade.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Veja a mensagem acima.' }
.\gradlew.bat assembleDebug
```

O script verifica a versão de todos os arquivos antes de alterar o projeto. Aceita LF/CRLF e BOM, cria backup em `.navigation-backups/capas-janela-prioridade-*`, verifica a gravação e restaura os originais em caso de falha durante a aplicação. A reaplicação não duplica alterações. Mudanças desconhecidas fazem o script parar, preservando os arquivos locais.

São sete arquivos de produção e dois arquivos de testes. Não modifica `build.gradle.kts`, assinatura, configuração local ou dados da playlist. `alteracoes.patch` contém as mudanças para revisão.

## Validação

- Compilação do APK de depuração e do APK dos testes Android concluída.
- Onze testes executados em ambiente de testes Android no host: limites das janelas, avanço por fileira, cancelamento das tarefas e fechamento solicitado das conexões, independência do carregamento de detalhes, pausa/retomada, foco inicial nas duas telas, posição e formato vazado do coração, e regressões de ESC/retorno às categorias.
- Atualizador PowerShell validado sobre cópias do ZIP restaurado, com LF e CRLF: aplicação, backup exato, reaplicação, preservação do Gradle/configuração local, rejeição de alterações desconhecidas e restauração após falha de gravação.

Os testes não medem o tempo de resposta do provedor nem o desempenho na TV física. Após instalar, confirme a navegação e a abertura no seu aparelho, especialmente com o cache de capas ainda incompleto.
