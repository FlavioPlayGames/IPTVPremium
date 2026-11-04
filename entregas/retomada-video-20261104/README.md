# Retomada automática de filmes e séries

Correção incremental sobre retomada-sessao. Filmes e séries pausam ao sair para outro aplicativo e voltam a reproduzir quando a tela do player fica ativa novamente. O comando passa de ON_START para ON_RESUME, quando a tela está interativa. A pausa de segundo plano não vira intenção de pausa manual. O pedido de retomada é salvo até a reprodução começar; uma tecla de pausa continua funcionando normalmente enquanto a pessoa está no player.

Os canais conservam o fluxo anterior de pausa/reconexão. Nenhuma alteração de manifesto, navegação, histórico, favoritos ou ajuste de tela neste pacote. Uma fonte Kotlin alterada. Requer a atualização retomada-sessao já aplicada.

Extraia o ZIP e execute dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Há backup e Desfazer.ps1; o instalador recusa fontes desconhecidos. Sem APK ou configurações privadas.

Conferir na TV: filme e série, sair para outro app e voltar, repetir, retornar após recriação do processo, pausar manualmente dentro do player, e canal ao vivo sem alteração de comportamento.
