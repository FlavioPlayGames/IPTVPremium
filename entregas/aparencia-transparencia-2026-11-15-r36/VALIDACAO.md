# Validação r36 — 15/11/2026

- :app:assembleDesempenho final aprovado: Kotlin, lint vital, R8 e geração dos APKs. Os seis fontes do manifesto correspondem aos fontes finais compilados.
- Aplicação, repetição, restauração byte a byte, proteção contra base divergente e contra sobrescrever edições posteriores aprovadas em PowerShell 7 e simulação da forma de arrays do Windows PowerShell 5.1. Parser dos scripts aprovado. A execução Windows real/ADB depende do PC.
- Todos os recursos/imagens de app/src/main/res comparados ao snapshot r35 e preservados byte a byte. Nenhuma imagem/recurso/dependência nova.
- Doze dependências de prévias/fundos/temas e player r35 são conferidas pelo aplicador e permaneceram idênticas. Home usa MarbleHomePreview/MarbleLayeredHomePicture; Kids usa KidsHomeArtwork/KidsCinemaHomeArtwork; Capas usa CatalogLayoutPreview, todos os renderizadores originais. Não há arte ilustrativa substituindo os fundos originais.
- Alteração central de transparência conserva RGB de panel/control/selection e as proporções da mistura do foco em SettingsLook, modificando alpha. Superfícies de legendas preservam seu RGB. Transparência aplicada ao fundo do painel, não ao conteúdo: imagens, QR, textos e ícones não recebem alpha do grupo.
- Revisão da geometria: menu de duas colunas e três linhas com opções compactas; painel de opções ocupa o mesmo espaço mínimo, sem longa lista vertical; prévias 16:9 lado a lado em largura >=660dp. Espaçamentos/paddings compactos para a referência 960×540dp. Telas estreitas/texto ampliado usam rolagem e prévias focáveis.
- Foco: abrir grupo leva à opção selecionada; Menu/Voltar retornam à entrada de origem. Alterar opção atualiza as prévias pelos estados existentes e mantém as rotinas de salvamento originais.

Limite: não houve emulador ou TV física. A compilação e a revisão de geometria/foco não comprovam visualmente cada resolução, fonte ampliada ou modo; conferir na TV menu, alternância de cores/modos/Kids, quatro/cinco capas e navegação. Nenhuma imagem de proposta é apresentada como captura do APK.

A r35 de sincronia/altura não foi alterada. Sem mudança de importação, legendas/provedores, site, dados, caches ou limites de memória/armazenamento. O problema de capas genéricas recarregando permanece separado.
