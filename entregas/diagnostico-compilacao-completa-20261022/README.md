# Diagnóstico completo de compilação

Use sobre o projeto atualmente aberto no Android Studio. Não modifica fontes, remove testes, instala APK ou limpa dados do aplicativo. A tarefa Gradle clean remove apenas saídas de compilação. Feche o Android Studio durante a coleta para reduzir bloqueios de arquivos.

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/diagnostico-compilacao-completa-20261022'
$script=Join-Path $env:TEMP 'Diagnosticar-Compilacao.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Diagnosticar-Compilacao.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
```

Aguarde até mostrar o caminho do TXT, mesmo se BUILD FAILED. O relatório fica na Área de Trabalho; se ela não estiver disponível, na raiz do projeto. Envie esse arquivo completo. Ele contém caminhos relativos dos candidatos e a saída completa do Gradle, não uma cópia do conteúdo dos fontes.

Executa clean, :app:assembleDebug e :app:compileReleaseKotlin, com --continue, sem cache de configuração/build e com dois workers. O Gradle pode resolver dependências normalmente. Não inclui testes automatizados no aplicativo.

O inventário procura declarações Kotlin no nível superior com o mesmo pacote/nome em arquivos diferentes, excluindo declarações private e classes indentadas. É uma heurística: variantes diferentes podem legitimamente declarar o mesmo nome e resultados precisam ser revisados. Não substitui o compilador, não encontra todo tipo de conflito e não altera declarações automaticamente.

Se a compilação passar e os erros aparecerem só no editor, o próximo passo será corrigir sincronização/índices do Android Studio. Se falhar, usar os erros do compilador para corrigir as causas e verificar novamente antes de entregar o conjunto. Erros posteriores podem aparecer após resolver a primeira causa, portanto não há garantia de que uma única rodada exponha todos.

Verificação do script: sintaxe PowerShell validada; inventário em fixture confirma candidato duplicado de KidsStyle e exclusão de private/nested. O comando Gradle precisa rodar no computador do usuário para diagnosticar o estado real do projeto. Não foi executado no projeto Windows nesta máquina.
