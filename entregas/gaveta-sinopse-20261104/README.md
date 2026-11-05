# Gaveta de sinopse e revisão da navegação

Aplica o visual aprovado: puxador contornado com SINOPSE na vertical, 22 x 84dp fixos. Alinhado à capa selecionada, o puxador só desliza horizontalmente junto com a borda da gaveta. O bloco de leitura é maior que o puxador; a alça não cresce nem muda de altura na abertura. A gaveta abre/recolhe com animação de 240ms.

Capas de filmes e séries mostram ano e nota (formato brasileiro, uma casa decimal). Sem nota real aparece ★ —; não inventa avaliação. Na gaveta ficam título, ano, gênero e duração quando disponíveis, mais a sinopse inteira com rolagem. Retiradas as instruções de rodapé da leitura.

→ na capa abre a gaveta. ↑/↓ rolam a leitura. ← ou Voltar recolhe e devolve o foco à mesma capa. O puxador também aceita clique. A navegação por níveis permanece: episódio -> temporada -> capa -> categorias -> vídeo. A aba continua ausente em temporadas e episódios. OK seleciona/reproduz e segurar por 2s favorita.

Revisão de desempenho: as coordenadas da aba e a animação foram isoladas da composição da lista paginada; deixam de atualizar todo o catálogo a cada posição de rolagem. Novas cargas de imagem aguardam a lista estabilizar, preservando bitmaps já exibidos e o cache existente. A transferência inicial de foco encerra as tentativas assim que aceita, para evitar disputa durante navegação rápida. Não há medição de FPS ou teste em TV neste ambiente; conferir a fluidez no aparelho.

Incremental sobre aba-sinopse 46ae509. Dois fontes alterados e um novo. PlaybackScreen, regras de Voltar, pausa/retomada, favoritos/histórico e manifesto preservados.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Há backup e Desfazer.ps1 com proteção de edições posteriores. O instalador recusa fontes desconhecidos. Sem APK ou configurações privadas.

Conferir na TV: lista com pressão rápida/contínua de setas, imagens após parar a rolagem, puxador fixo abrindo/fechando, sinopse longa, ano/nota e metadados ausentes, Voltar por níveis, OK e favoritar por 2s.
