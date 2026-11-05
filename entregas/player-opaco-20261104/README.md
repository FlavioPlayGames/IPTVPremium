# Player: fundo translúcido mais opaco e dados nas capas

Unifica os fundos dos painéis, menus/submenus, gaveta/puxador, barra de controles e janelas do player em grafite neutro com aproximadamente 85% de opacidade. Vale para filmes/séries normais e Kids. Só os destaques recebem a cor do tema: a sessão Kids usa diretamente o azul/rosa de seu estilo, sem emprestar a cor do perfil normal. Não aplica filtro, mudança de opacidade ou desfoque ao vídeo.

Nas capas, dados em duas linhas: Ano: 2026 e Nota: ★ 8,2. A estrela é sempre amarela, inclusive no Kids azul/rosa. Dados ausentes mostram —, sem inventar avaliação. Ano/nota ficam ao lado da imagem, sem símbolos sobre a capa.

Gaveta e puxador vertical fixo, animação, rolagem otimizada, sinopse completa e Voltar por níveis permanecem. Os mini controles centrais de Zoom/Manual continuam sem fundo, conforme o pedido anterior; a opacidade nova aplica-se aos fundos que já existem. Não altera tema do menu principal/Kids, legendas de vídeo, pausa/retomada, histórico/favoritos ou geometria da tela.

Incremental sobre gaveta-sinopse 3f774db. Seis fontes Kotlin alterados. Sem dependências ou manifesto novos.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Há backup e Desfazer.ps1, com proteção de edições posteriores. Fontes desconhecidos são recusados. Sem APK ou configurações privadas.

Conferir na TV normal/Kids azul/Kids rosa: catálogo, gaveta, opções/submenus, barra de controles, janela de retomada e estrela amarela. Conferir também fonte maior, dados ausentes e navegação rápida. As propostas visuais não são capturas desta compilação.
