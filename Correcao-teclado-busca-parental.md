# Busca parental: teclado inferior, Enter e contornos do tema

Base: atualização Ajustar-parental-referencia.ps1 já aplicada.

O painel de busca usa um contêiner de tela inteira com Alignment.BottomCenter, mantendo o teclado na parte inferior e respeitando a margem de 8 dp. O próprio teclado continua o mesmo AppKeyboard do aplicativo.

A causa identificada para o fechamento por Enter era a interceptação de teclas no contêiner do AppKeyboard: quando o dispositivo é classificado como teclado alfabético, Enter e Enter do numpad chamavam onAction antes que a tecla virtual em foco pudesse receber o evento. Adicionada opção enterSubmits com padrão true para preservar os outros consumidores. A busca parental passa false, deixando o Enter chegar à tecla virtual selecionada. Somente a ação Concluir ou o retorno do diálogo fecha a busca.

Incluídos contornos metálicos na cor do tema em todos os cartões (também sem foco), busca, botões e moldura externa. No teclado desta busca, themeBorders=true aplica os contornos metálicos ao painel e às teclas. Nos outros usos do AppKeyboard, o padrão permanece inalterado. O foco continua com borda mais espessa e fundo/texto destacado.

Atualização de dois arquivos, com validação prévia e backup em .iptv-backups/teclado-busca-*. Não altera PIN, bloqueios, listas, licença, assinatura, Firebase, versão ou Android mínimo 12.

Na raiz do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Corrigir-teclado-busca-parental.ps1
```

```powershell
.\gradlew.bat :app:assembleDebug
```

Instalar sobre o aplicativo atual com a mesma assinatura, sem desinstalar.

Validações: compilação das cinco variantes debug concluída; teste do manipulador real de teclas com adaptadores JVM confirma que Enter/numpad Enter não disparam Concluir na busca parental, que o comportamento padrão dos outros consumidores foi preservado e que letras/apagar continuam funcionando; instalador PowerShell validado em simulação, aplicação, backup, repetição, LF/CRLF/BOM, preservação de arquivos privados e rejeição de fonte desconhecida antes de escrever.

Posição visual e navegação com controle devem ser conferidas no aparelho; não houve teste físico ou emulador neste ambiente.
