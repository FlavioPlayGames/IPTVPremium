# Catálogo restaurado e ajuste automático sem zoom

Atualização cumulativa para IPTVPremium, incluindo a entrega 6a69577 e as anteriores conhecidas.

- O catálogo lateral de filmes/séries voltou aos blocos anteriores: fundo discreto em cada linha e contorno/fundo na cor do tema ao selecionar. O painel mantém a cor e transparência aprovadas. A alteração também vale às temporadas/episódios dentro do catálogo.
- Opções e seus submenus continuam em lista simples, sem caixas individuais. Controles inferiores permanecem com foco apenas pela cor.
- Ajuste manual e Zoom ficam centralizados, sem fundo quadrado: somente título, setas e valores sobre o vídeo. As setas usam o próprio desenho de reprodução do player, apenas girado, sem uma segunda versão do triângulo.
- Nova opção Tela > Ajuste automático. Estica cada eixo para preencher a tela, sem zoom uniforme ou corte. Pode alterar a proporção dos objetos. O modo remove barras geradas pelo enquadramento; faixas pretas gravadas nos pixels do vídeo não desaparecem com simples esticamento. Normal, Zoom, Tela cheia (com corte) e Manual continuam disponíveis separadamente.
- O coração usa agora o mesmo componente ativo da ficha do filme (`ReferenceFavoriteGlyph`), com o mesmo desenho e a mesma paleta, sem cor aproximada ou o vetor antigo de outra tela. Aparece fora da capa, apenas nos favoritos marcados dentro do player. OK longo continua em 2 segundos.
- Preservados: capas das temporadas com alternativa da capa da série, miniaturas com alternativa do fundo, Continuar/Favoritos, CH, troca direta, menu Kids, e Continuar sem sombra/camada interna.

## Aplicar no Windows

Extraia o ZIP e execute na pasta interna `player-catalogo-ajuste`:

```powershell
$projeto = 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\Aplicar.ps1' -Projeto $projeto -Testar
```

Depois de BUILD SUCCESSFUL, com a TV conectada pelo ADB:

```powershell
& '.\Instalar-TV.ps1' -Projeto $projeto
```

O instalador verifica os fontes antes de escrever e guarda backup em `.player-catalogo-ajuste-backups`. Fontes desconhecidos são recusados para proteger alterações locais. Instalação com `adb install -r`, preservando dados. O ZIP contém fontes; o APK é gerado com as configurações do projeto do usuário.

Restaurar fontes: `& '.\Desfazer.ps1' -Projeto $projeto`. Não apaga favoritos e preferências do aparelho.

## Conferir na TV

1. Blocos e foco no catálogo; Opções e submenus sem caixas; cor/transparência preservadas.
2. Manual e Zoom sem fundo; setas iguais ao ícone de reprodução, giradas para cada direção.
3. Vídeo 4:3: Automático preenche a largura sem cortar os lados; vídeo panorâmico: estica a altura. Comparar com Normal e com Zoom/Tela cheia. Repetir em Exo e VLC; também conferir canais/Kids, que compartilham o ajuste.
4. Comparar o coração favoritado do player com o da ficha de informações. Segurar OK por 2s não pode iniciar o vídeo nem alternar duas vezes.
5. Conferir retomada, episódios anterior/próximo, CH e imagens.

Testes de lógica, compilação, Lint e instalador validados no ambiente de desenvolvimento. Foco, escala, vídeo real e aparência precisam de conferência na TV; não houve acesso ao aparelho nem medição de FPS.
