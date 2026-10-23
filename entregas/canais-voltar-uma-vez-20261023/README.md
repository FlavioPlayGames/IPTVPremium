# Canais: sair das categorias com um toque

Com o cursor em uma categoria de Canais, um único ESC/Voltar retorna à Home. Fora das categorias, o primeiro toque devolve o foco às categorias; somente o próximo sai. O retorno é explícito, antes que uma View focada consuma o comando. A navegação desempilha até a Home, preservando as etapas anteriores: teclado e menus fecham primeiro, sair da tela cheia retorna à prévia, a lista de canais retorna às categorias. A busca de servidor não bloqueia a saída; o ciclo de vida da tela libera a conexão ao sair.

Aplicar após importacao-dinamica-prioridade-20261023. Mantém os demais ajustes, incluindo carregamento-sem-subtitulo.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/canais-voltar-uma-vez-20261023'
$script = Join-Path $env:TEMP 'Preparar-Voltar-Canais.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Voltar-Canais.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Instalador valida os arquivos antes de escrever e cria backup fora de app/src. Também limita o recorte e o desenho de foco dos ícones Atualizar e Configurações à área ampliada de cada ícone, evitando ampliar uma faixa do mármore. Não altera arquivos de imagens ou dados. Não acrescenta testes ao aplicativo. Ainda é necessário confirmar os comandos no controle da TV.
