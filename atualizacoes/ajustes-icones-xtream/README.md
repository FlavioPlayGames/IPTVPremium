Ícones: saturação em 75% e cinza interno escuro mais claro

Reduz a saturação em 25% no azul, vermelho, roxo, rosa e dourado, nos modos claro e escuro. O filtro é compartilhado pelos ícones da Home horizontal e vertical e seus reflexos. Os PNGs e desenhos permanecem intactos.

No modo escuro, os meios-tons cinzas internos recebem ganho de luminosidade de 15%, com bordas suavizadas para preservar contornos pretos e reflexos. A seleção de tons neutros preserva o metal colorido e a transparência. O ajuste ocorre uma vez ao decodificar a arte em IO, usando um buffer de uma linha e o cache existente. O modo claro não recebe esse clareamento.

Xtream recebe até três novas tentativas após a primeira falha (quatro no total), com pausas de 0,5 / 1 / 2 segundos. Nunca troca para M3U. Cancelamento e erro local de armazenamento interrompem a operação. Uma atualização malsucedida mantém a geração ativa anterior.

Somente MarbleLayeredHomeArtwork.kt, MarbleArtwork.kt e M3uSourceSynchronizer.kt são substituídos. Ícones em PNG, geometria, foco, navegação, Atualizar, Configurações, player, listas e limites de cache preservados. Instalador com backup, verificação de versão, repetição segura e restauração em caso de erro de escrita.
