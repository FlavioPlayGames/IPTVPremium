# IPTV Premium 1.09 — categorias e favoritos dos canais

Categorias de canais e séries passam a usar o mesmo critério dos filmes: lançamentos primeiro, 4K em destaque, demais categorias em ordem alfabética e adultos/XXX/+18, cursos e nomes iniciados por símbolos no final. O tratamento de legendados já existente é preservado, sem recriar a aba removida. Os atalhos Todos, Continuar e Favoritos mantêm sua posição.

Nos canais, toque curto seleciona; segurar OK por 1 segundo adiciona/remove o favorito uma única vez, sem selecionar ao soltar após a ação longa. Toque/mouse usam o mesmo segundo completo. Repetição de tecla não antecipa a ação. Cancelamento ou perda de foco interrompem a espera. O coração indica o estado sem criar um segundo alvo de foco.

As linhas de canais recortam o preenchimento e a seleção em seu contorno arredondado, sem a indicação retangular preta padrão. Os IDs favoritos ficam salvos no mesmo arquivo de preferências dos filmes, em chave própria por lista. Ao abrir Favoritos, somente esses IDs são consultados no Room, inclusive quando ainda não estão na página carregada. O acesso existente mantém as regras parentais e do Kids.

Aplicar após a última correção visual da versão 1.09. Execute Preparar-Categorias-Favoritos-1.09.ps1 na raiz do projeto. Com o emulador aberto, use -Emulador emulator-5554 -Testar. Verificação de compatibilidade acontece antes da escrita; backup automático; instalação sobre o app existente, sem limpar dados.

Validação: 14 testes JVM da política real de ordenação e limite temporal; assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin; verificação do instalador incluindo backup exato, reaplicação, rejeição atômica de edição desconhecida e preservação de configurações privadas/testes locais em UTF-8 e BOM/CRLF.

A nuvem não tem emulador. A suíte instrumentada foi compilada; -Testar executa 97 testes no emulador local. Ações reais do controle/toque, foco e aparência ainda precisam da revisão no aparelho.
