# Versão 1.13 — histórico Kids separado e botão Assistir

IMPORTANTE: conforme autorizado, ao abrir esta versão o aplicativo limpa UMA ÚNICA VEZ o Continuar antigo já misturado e as posições antigas de retomada. Favoritos, listas e dados de acesso são preservados. Marcas válidas de episódios já concluídos permanecem; a limpeza é do Continuar antigo, não de toda a biblioteca.

Daí em diante Kids e normal possuem progresso, retomada, próximo episódio, remoções e vínculos de histórico separados. O mesmo filme/episódio pode ter posições diferentes nos dois modos. Cada modo continua acumulando seu próprio histórico entre listas, mostrando apenas conteúdo disponível e sem duplicar edições. Favoritos do player e catálogo continuam iguais dentro de cada modo; nunca mistura Kids com normal.

O botão Assistir nas fichas de filmes e séries Kids agora usa a cor azul/rosa do estilo Kids, tanto no preenchimento quanto na borda, sem mudar o botão normal ou o clique/reprodução.

O instalador atualiza versionName para 1.13 e eleva versionCode acima da versão 1.12, respeitando APP_VERSION_CODE em local.properties. Faz somente uma edição das declarações de versão em app/build.gradle.kts, preservando as demais configurações e guardando o original no mesmo backup dos fontes. Nenhuma cópia da configuração particular do projeto acompanha o pacote.

Incremental sobre player-opaco c0d2528. Oito fontes Kotlin (seis alterados, dois novos). Mantém player opaco, gaveta, estrela amarela, dados Ano/Nota e rolagem otimizada.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Há backup/Desfazer dos fontes e da versão; desfazer fontes NÃO recupera dados antigos que o aplicativo já limpou na primeira abertura. A limpeza dos dados foi autorizada nesta conversa. Reaplicar ou abrir de novo não limpa progresso novo.

Conferir na TV: filme/episódio no Kids não aparece no normal e vice-versa; posições diferentes do mesmo título; trocar lista em cada modo; favoritos e remoções separados; Assistir azul/rosa em filmes e séries; tela Sobre com 1.13.
