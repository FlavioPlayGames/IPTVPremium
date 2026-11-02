# Player — painel translúcido e ajuste central

Atualização cumulativa de fontes para IPTVPremium. Inclui as revisões anteriores conhecidas, até 135485c, com o refinamento visual aprovado.

- Seleção somente pela cor do texto/ícone, sem fundo individual, caixa, contorno, ponto ou sublinhado. Mesmo tamanho e peso durante o foco.
- Cada menu usa um único painel translúcido inteiro. Catálogo à esquerda; Opções e submenus à direita, com altura inteira e bordas retas. Sem linhas extras de divisão ou blocos em cada opção.
- Continuar/Do início sem camada interna, sombra, brilho, contorno ou efeito de foco retangular; foco apenas na cor do texto e ícone.
- O mesmo visual vale ao entrar em Reprodutor, Áudio, Legendas, Estilo das legendas, Velocidade, Tela e Informações técnicas.
- Ajuste manual e zoom exibem somente o mini menu no centro do vídeo, sem painel lateral nem controles principais. Setas ajustam, OK retorna ao menu Tela; Voltar ou inatividade fecham. O vídeo continua no estado anterior.
- Temporadas tentam primeiro sua própria capa (oficial/fornecedor), depois a capa da série. Se uma URL existir mas falhar ao baixar, a próxima alternativa é tentada. Depende de ao menos uma capa válida disponível; não cria arte inexistente.
- Preservados: fotos de episódios com fundo da série como alternativa, favoritos locais do player com OK por 2s, coração rosa forte fora da capa, Continuar/Favoritos, CH, troca direta e tema Kids.

## Aplicar

Extraia o ZIP e execute na pasta interna `player-translucido-final`:

```powershell
$projeto = 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\Aplicar.ps1' -Projeto $projeto -Testar
```

Depois de BUILD SUCCESSFUL, com a TV conectada por ADB:

```powershell
& '.\Instalar-TV.ps1' -Projeto $projeto
```

Backup local: `.player-translucido-backups` no projeto. O instalador verifica os fontes antes de escrever e recusa versões desconhecidas. Instalação com `adb install -r`, preservando dados.

Restaurar fontes: `& '.\Desfazer.ps1' -Projeto $projeto`. Não apaga favoritos/preferências do aparelho. O ZIP contém fontes e scripts; o APK é compilado com as configurações do seu projeto.

## Conferir na TV

1. Foco muda apenas a cor no player, catálogo e em todas as opções internas. Nenhuma caixa em torno dos itens, nenhuma divisão duplicada.
2. Tela > Ajuste manual: somente o mini menu no centro. Testar setas, OK, Voltar e fechamento por inatividade, inclusive com vídeo pausado.
3. Temporada com capa própria usa sua imagem; sem capa ou com link inválido usa capa da série, se disponível.
4. Conferir favoritos (OK por 2s, uma vez por pressão), CH e reprodução enquanto navega.

Compilação, Lint, testes de lógica e instalador validados no ambiente de desenvolvimento. Escala/transparência, foco real do controle e desempenho devem ser conferidos no aparelho; não houve acesso à TV.
