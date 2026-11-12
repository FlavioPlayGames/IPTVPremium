# Correção do fundo — imagem estática da Home, sem logo — r28

Aplicar sobre a r27. Esta correção modifica somente `MetallicThemeBackdrop.kt`: remove o desenho procedural e exibe os fundos já preparados no projeto, `home_wallpaper_graphite` e `home_wallpaper_pearl`. Essas imagens correspondem ao cenário da Home sem logo nem ícones. A imagem é carregada em sua resolução original, no cache existente, fora da thread principal. O filtro de cor já configurado acompanha os cinco temas; o modo selecionado escolhe claro/escuro. Não há blur.

O logo do cabeçalho de cada tela é preservado. Nenhum logo é acrescentado ao fundo. Site, envio manual, cartões, botões, versão, assinatura e os demais arquivos permanecem como estão na r27.

Execute `Atualizar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"` na pasta extraída. O script faz backup do único arquivo, compila Desempenho e instala por ADB. Aceita `-Serial`, `-Adb` e `-SomenteAplicar`. Recusa arquivos que não correspondam à r27 ou à própria r28 antes de escrever. O instalador mantém os dados e a assinatura local do aplicativo.

Não é necessário reenviar imagens: os dois recursos já pertencem ao projeto atualizado enviado pelo usuário. Esta correção não publica nem altera o portal.
