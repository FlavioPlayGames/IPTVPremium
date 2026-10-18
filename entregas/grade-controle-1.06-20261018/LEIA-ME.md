# Grade, controle e atualização — 1.06

Filmes e séries: foco estável na grade enquanto a capa de origem é reciclada. Cima/baixo mantêm a coluna e esquerda/direita mudam uma coluna sem dar a volta. Somente ESQUERDA na primeira coluna pode sair para categorias. A rolagem aguarda a linha e enquadra o destino inteiro, sem entregar o foco a outro painel.

Aparência: Capas por fileira abaixo de Tamanho dos textos. Opções persistentes 4 ou 5 (padrão 5). Quatro colunas dimensionam duas fileiras completas, reservando espaço para títulos, notas, texto ampliado, margens e brilho. Arte inteira com ContentScale.Fit, sem deformação. A proporção depende do espaço disponível. Mantém o neon fino +25%.

Capas: o lote prioritário de até 15 agora também decodifica para o cache de RAM existente, não apenas baixa o arquivo. Três downloads e duas decodificações simultâneas; cache continua 20 MiB. Catálogo e demais caches mantêm os limites anteriores. A velocidade de imagens ainda depende do servidor; o alvo total de 350 MB requer medição no aparelho.

Atualizar na Home: inicia uma Activity interna não exportada em processo separado, encerra o processo antigo para evitar arquivos recriados durante a limpeza, remove cacheDir/externalCacheDir e tabelas temporárias do catálogo Room, e abre MainActivity em um processo novo. O catálogo será importado novamente e os recursos do tema recompostos. Preserva SharedPreferences e filesDir: listas cadastradas, chaves, configurações, favoritos, histórico e identidades compartilhadas. Não desinstala nem limpa dados do aplicativo.

Compilação app/testes Android e lint em nuvem. Oito testes Compose preparados para foco e teclado, incluindo tecla mantida e saída para categoria; compilados, não executados neste ambiente sem emulador/KVM. -CapturarLog instala por cima, executa uma única vez os oito testes e captura memória/navegação. O botão Atualizar real precisa ser conferido na TV/emulador; reinício real não foi executado em nuvem.

Instalador cumulativo com hashes, backup e rollback. -Verificar apenas confere compatibilidade. 1.06/interno 8.
