# Home: novos ícones e foco dos utilitários

Base: `java.zip` enviado nesta solicitação. Substitui as referências dos ícones anteriores pelos dez conjuntos PNG aprovados: branco no modo claro e cinza no modo escuro, para Dourado, Azul, Rosa, Roxo e Vermelho. Usa as cores prontas dos PNGs, sem aplicar novamente a recoloração do metal. Os arquivos de imagem são cópias sem edição das versões geradas.

Cada tema/modo usa uma única faixa de imagem compartilhada pelos cinco ícones, com recortes definidos no desenho. A proporção original é preservada dentro dos espaços existentes, com a base e o centro alinhados. O cache de arte existente de 18 MB é reutilizado; não há novo cache nem processamento de cores por pixel. As imagens antigas deixam de ser usadas pela Home, mas o instalador não remove recursos antigos do projeto.

Atualizar e Configurações usam o aumento de 10% e a animação de 180 ms já existentes na Home, em vez do clareamento ao receber foco. As cores são as mesmas antes e durante o foco. Também recebem aumento no layout vertical. As áreas de clique, ações e navegação permanecem iguais.

## Instalação

Na raiz do projeto, execute `Atualizar-Icones.ps1 -Projeto CAMINHO_DO_PROJETO` no PowerShell e depois `.\gradlew.bat assembleDebug`.

O instalador inclui os três arquivos Kotlin e os dez recursos necessários. Confere a versão de todos os arquivos antes de escrever, faz backup em `.navigation-backups/home-branco-cinza-*`, recusa sobrescrever fontes diferentes da base enviada e restaura as alterações se a gravação falhar. A reaplicação é segura. Não altera Gradle nem assinatura.

## Validação

Testes verificam os dez recursos distintos, limites dos recortes, decodificação dos PNGs reais e compartilhamento do mesmo bitmap pelos cinco ícones, navegação dos utilitários e renderização do aumento com conservação da cor. O instalador foi verificado com LF, CRLF/BOM, repetição, recusa de fonte modificada e falha simulada após criar um PNG.

A cópia local não contém todos os fundos do projeto atual: os testes de compilação usam aliases temporários para doze recursos ausentes. Esses aliases não estão no instalador. O teste de renderização usa um fundo sintético para medir aumento e cor. Os dez PNGs novos são reais. Não foi realizada validação em TV física com os fundos atuais.

Para repetir no projeto completo:

```powershell
.\gradlew.bat -I caminho\testes\validar-icones.gradle :app:testDebugUnitTest --tests '*HomeFinishedIconsTest' --tests '*HomeUtilityZoomTest'
```

Todas as outras fontes do ZIP atual permanecem intactas, incluindo categorias, capas do catálogo, importação, Kids, favoritos e reprodução.
