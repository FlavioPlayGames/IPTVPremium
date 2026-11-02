# Player — visual aprovado

Atualização cumulativa de fontes para IPTVPremium, incluindo a entrega aa237e8 e as revisões anteriores conhecidas.

## O que mudou

- Ícones vetoriais de traço fino: anterior/próximo episódio, voltar/avançar 10s, reproduzir/pausar, catálogo, legendas, tela e opções. Seleção com fundo suave e contorno na cor do tema; sem sublinhado.
- Faixa inferior escura translúcida, compacta, com divisão horizontal reta e barra de progresso larga. Horário mantido.
- Catálogo à esquerda e configurações à direita: painéis de altura inteira, sem margem externa nem cantos arredondados, com divisão vertical reta. Os controles internos mantêm os destaques suaves da prévia.
- Capas sem símbolos sobrepostos. O conteúdo atual é indicado pelo texto “Reproduzindo agora”. O coração fica ao lado do nome e reutiliza o vetor da ficha de informações.
- Favoritos de filmes/séries do player agora ficam separados dos favoritos da ficha. Somente os itens marcados dentro do player mostram coração no seu catálogo; favoritos de episódios continuam separados por playlist e Kids/normal. Favoritos anteriores da ficha permanecem intactos, sem migração automática para o player.
- Segurar seleção/OK por pelo menos 3s favorita/desfavorita uma vez. Comandos DOWN repetidos não reiniciam a contagem. Soltar depois não reproduz. Perda de foco, janela ou cancelamento interrompe o gesto. Toque curto reproduz normalmente.
- Ao entrar em uma série, busca informações e fotos pela mesma identidade confirmada da ficha. Ficha e player usam o mesmo carregador/cache de temporadas. Fotos oficiais têm prioridade, depois as do fornecedor e alternativas já usadas pela ficha. Na ausência/falha da miniatura horizontal, usa o fundo da série; não usa um pôster vertical como miniatura de episódio.
- Catálogo preserva Continuar/Favoritos, CH para subir/descer, reprodução durante a navegação e troca direta sem abrir informações. Kids conserva sua cor.

## Instalar

Extraia o ZIP e abra o PowerShell na pasta interna `player-visual-aprovado`:

```powershell
$projeto = 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\Aplicar.ps1' -Projeto $projeto -Testar
```

Após BUILD SUCCESSFUL, com a TV conectada pelo ADB:

```powershell
& '.\Instalar-TV.ps1' -Projeto $projeto
```

O instalador verifica os fontes, guarda backup local em `.player-visual-backups` e instala com `adb install -r` preservando dados. Recusa arquivos desconhecidos em vez de sobrescrever alterações locais. O pacote entrega fontes, sem APK com configurações locais.

Para restaurar os fontes: `& '.\Desfazer.ps1' -Projeto $projeto`. Isso não apaga preferências do aparelho, inclusive os novos favoritos do player.

## Conferir no aparelho

- Verificar faixa inferior, ícones e foco sem sublinhado; painéis de altura inteira à esquerda e à direita, incluindo Tela/Zoom/Ajuste manual.
- Segurar OK 3s, continuar segurando, soltar: favoritar uma única vez sem iniciar vídeo. Repetir para desfavoritar; repetir com filme, série e episódio. OK curto deve abrir diretamente.
- Conferir coração fora da capa e somente para os favoritos do player, com isolamento entre playlists e Kids.
- Abrir outra série e temporadas; conferir fotos oficiais, fallback do fornecedor e fundo quando a foto faltar/falhar. Comparar com o caminho tradicional.
- Conferir CH, vídeo reproduzindo/pausado durante navegação e botões de episódio anterior/próximo.

Compilação, Lint, testes de lógica e instalador foram verificados no ambiente de desenvolvimento. A escala e o comportamento real do controle ainda precisam de validação na TV. A prévia aprovada é referência de desenho; não é captura do aplicativo em execução.
