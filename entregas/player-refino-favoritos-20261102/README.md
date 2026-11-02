# Player: refinamento, catálogo e favoritos

Atualização cumulativa para o projeto IPTVPremium. Inclui as revisões anteriores conhecidas e os ajustes desta entrega, sem apagar dados do aplicativo.

## Alterações

- Menu inferior mais baixo, ícones menores e linha do tempo quase de ponta a ponta na cor do tema. Horário mantido.
- Botões **Episódio anterior** e **Próximo episódio** em séries: usam a sequência real do catálogo e abrem diretamente a reprodução, sem passar pelos detalhes. Quando não há episódio na direção escolhida, o fluxo existente informa o usuário.
- Continuar de filmes/séries, Opções, Tela e demais painéis com visual escuro e discreto, combinando com o novo player.
- Catálogo preservado em uma coluna, com indicador ▶ na capa do conteúdo em reprodução, separado do foco do controle.
- CH abre o catálogo; quando aberto, CH+ sobe e CH− desce. O botão de lista de conteúdo pode abrir/fechar.
- Catálogo com **Continuar** e **Favoritos**, separado entre filmes e séries. Continuar recupera a posição salva; confirmar o item que já está tocando apenas fecha o catálogo.
- Segurar OK por **3 segundos ou mais** favorita/desfavorita filmes, séries e episódios. O coração aparece somente nos favoritos. O gesto longo não inicia reprodução. Favoritos de episódios são separados por playlist e por perfil Kids/normal.
- Capas de temporadas usam a mesma identificação e cache de informações da série. Falha na busca de arte preserva os episódios do fornecedor; somente temporadas reais são exibidas.
- Kids ao vivo usa o mesmo componente de menu dos canais normais, com a cor do próprio Kids. Filmes e episódios Kids também respeitam sua paleta.
- Navegar no catálogo mantém a reprodução e preserva uma pausa anterior. Selecionar outro conteúdo troca diretamente o vídeo.

## Aplicar no Windows

Extraia o ZIP. Na pasta interna `player-refino-favoritos`, execute:

```powershell
$projeto = 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\Aplicar.ps1' -Projeto $projeto -Testar
& '.\Instalar-TV.ps1' -Projeto $projeto
```

O primeiro comando verifica os fontes, cria um backup local e compila. O segundo instala com `adb install -r`, preservando dados. A TV precisa estar conectada ao ADB. Não acompanha APK com configurações locais de validação.

O instalador aceita o java.zip original de 02/11/2026 e as revisões conhecidas, inclusive o player minimalista 3d0b7a6. Recusa fontes alterados fora dessas versões para evitar sobrescrever trabalho novo. Não remova essa proteção: envie o arquivo indicado se houver conflito.

Backup: `.player-refino-backups` no projeto. Para restaurar os fontes:

```powershell
& '.\Desfazer.ps1' -Projeto $projeto
```

Restaurar fontes não apaga favoritos/preferências salvos no aparelho. Backups antigos retirados de `java` ficam no backup local, fora da compilação.

## Conferir na TV

1. Filme e série: verificar novo Continuar, altura dos controles e painéis Tela/Opções/Legendas.
2. Série: testar anterior/próximo no meio e nos limites da sequência, preservação do histórico e ausência da tela de informações.
3. CH: abrir catálogo, subir/descer sem trocar o vídeo, confirmar novo item e repetir com vídeo pausado.
4. OK curto reproduz; OK por 3s favorita sem reproduzir; coração persiste após reabrir. Testar filme, série e episódio nas coleções Favoritos e Continuar.
5. Conferir indicador ▶, miniaturas de temporadas ao mudar de série e fallback quando não houver imagem.
6. Kids: conferir menu de canais, cor própria e isolamento de favoritos em relação ao perfil normal.

Compilação, análise estática, testes de lógica e do instalador foram feitos no ambiente de desenvolvimento. Foco real do controle, escala e fluidez precisam ser conferidos na TV; não houve acesso ao aparelho.
