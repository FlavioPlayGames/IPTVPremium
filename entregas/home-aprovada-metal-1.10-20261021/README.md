# Home aprovada — ícones novos — versão 1.10

Dez PNGs novos: cinco para Claro e cinco para Escuro, com interiores pérola e cinza prontos na própria imagem. A antena da TV é plana, em 2D. O acabamento e as sombras estão desenhados nas imagens. A cor do metal acompanha Dourado, Azul royal, Rosa, Roxo e Vermelho; a transformação do metal preserva os tons neutros e o alfa. É calculada no IO, uma vez por recurso/tema em cache limitado, sem recalcular em cada foco.

Mantém tamanho, posição, legendas nativas, seleção e reflexos no mármore. Não altera a área Kids interna. A prévia de recursos é uma composição com os arquivos reais e a geometria do app, não uma captura do Android.

Remove os dez PNGs anteriores, identificados por hash, e resíduos conhecidos das entregas clássica/máscara caso ainda existam. Faz backup antes de remover. Imagens personalizadas desconhecidas são preservadas e referências externas a imagens que seriam removidas bloqueiam a aplicação antes de gravar. Não restaura imagens excluídas manualmente. Não publica APKs nem configurações privadas.

Execute com o emulador aberto: `Preparar-Home-Aprovada-1.10.ps1 -Projeto C:\Users\flavi\AndroidStudioProjects\IPTVPremium -Emulador emulator-5554 -Testar`. O instalador baixa e verifica o pacote, aplica a correção, compila e instala com `adb install -r`, preservando listas, histórico, avaliações e cache. A suíte selecionada tem 113 testes. `-Verificar` apenas verifica, `-SomenteAplicar` aplica sem compilar.

Inclui cumulativamente a 1.10 e suas correções de capas em cache e compatibilidade de temas. A versão permanece 1.10 e preserva versionCode local superior.

Validação: debug e AndroidTest montados, release Kotlin compilado; cinco testes JVM da política aprovados e auditoria dos dez PNGs aprovada, com tons neutros e alfa preservados. Instalador verificado em seis bases/cenários. Instrumentação Android foi compilada; será executada no emulador pelo comando acima e não foi executada na nuvem.
