# Aparência e transparência r36

Base obrigatória: r35 aplicada em C:\Users\flavi\AndroidStudioProjects\IPTVPremium.

## Atualizar

Execute Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium" com a TV já conectada/autorizada no ADB. O script valida os fontes antes de gravar, cria recuperação externa, compila e instala com a assinatura local. Não altera o pareamento nem apaga dados. Aplicar.ps1 apenas aplica; Desfazer.ps1 -Backup "pasta informada" -Instalar restaura protegendo alterações posteriores.

## Aparência aprovada

- Menu compacto em duas colunas: Tema/Modo de exibição; Visual Kids/Tamanho dos textos; Capas por fileira/legenda das cores.
- Opções do grupo escolhido aparecem no mesmo painel, organizadas em grade compacta. As prévias permanecem abaixo. Menu e Voltar retornam ao grupo de origem com foco.
- Home, Kids e Capas lado a lado em TV larga, com proporção 16:9. A organização reserva espaço para mostrar menu e três prévias juntos na referência 960×540 dp, sem expandir uma longa coluna de opções. Telas estreitas/textos maiores podem rolar, e as prévias são alcançáveis pelo controle.
- As prévias são os componentes originais já existentes: MarbleHomePreview, KidsHomeArtwork e CatalogLayoutPreview. Usam os fundos, artes, menus e capas locais originais, acompanhando as escolhas. Nenhuma arte ilustrativa da proposta foi adicionada ao aplicativo.
- As opções e o salvamento existentes foram mantidos: cinco temas, modos Claro/Escuro, estilos Kids, três tamanhos de texto e quatro/cinco capas por fileira.

## Transparência

Painéis e controles de Configurações recebem transparência via componentes compartilhados, incluindo as telas internas, superfícies de legendas, formulários/grade de proteção e os componentes que já reutilizam esse visual. A grade principal continua 3×3; a descrição de Controle parental foi encurtada para caber melhor. Fundos estáticos e suas imagens permanecem, sem novo desenho/blur/animação de cenário. A transparência está na cor de fundo do painel, não nos textos, ícones, QR ou imagens de prévia.

Paletas e modos continuam os existentes. A alteração central de SettingsLook mantém o RGB das cores e modifica sua opacidade; a mistura de foco usa a mesma cor e proporção anteriores. As cinco cores de tema e os renderizadores de fundos/prévias são verificados como dependências inalteradas.

Sem alterações de player, sincronia/altura r35, legenda/provedor, importação, site, cache ou limites de armazenamento. Nenhuma biblioteca/recurso/imagem nova. Conferir VALIDACAO.md e testar foco/legibilidade e as prévias na TV após instalar; não houve captura real de APK neste ambiente.
