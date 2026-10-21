# Recurso ausente da prévia

Restaura `app/src/main/res/drawable-nodpi/preview_poster_04.jpg`, referenciado por `CatalogLayoutPreview.kt`. JPEG original da cópia anterior do projeto (420 × 600); nenhuma alteração em Kotlin, temas ou funções.

Copie a imagem para a pasta indicada e compile novamente. O recurso é uma capa local da prévia de personalização, não uma capa baixada do catálogo.

Se as imagens foram excluídas de propósito, execute `Corrigir-Previa.ps1 -Projeto CAMINHO_DO_PROJETO`. O script identifica referências `preview_poster_*` sem arquivo drawable e as substitui por uma capa referenciada ainda existente. Faz backup do Kotlin antes de escrever; se nenhuma capa existir, interrompe sem alterar. Não baixa/restaura imagens excluídas. Verificado com referências parcialmente ausentes, todas presentes, nenhuma capa disponível e reaplicação, preservando BOM/CRLF.
