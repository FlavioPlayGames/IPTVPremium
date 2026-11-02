# Ajuste salvo por filme e temporada

Atualização cumulativa de fontes para IPTVPremium, incluindo a entrega f07a8be e as revisões anteriores conhecidas.

## Alterações desta entrega

- Cada filme salva seu próprio enquadramento. Em séries, o ajuste é salvo por série e temporada: trocar para outro episódio da mesma temporada reaplica a escolha; outra temporada tem configuração própria.
- Salva Normal, Automático e os valores de Zoom/Manual (zoom, largura, altura e deslocamento), recuperando ao reabrir o player ou aplicativo. Preferências separadas por playlist e perfil Kids/normal. Temporada não identificada usa escopo do episódio para não misturar séries/temporadas.
- Controles internos de Zoom e Manual ampliados: áreas/setas de 28 para 56dp, painel transparente central de 156 para 280dp, título 24sp, Zoom 24sp, OK 20sp e largura/altura 21sp. Texto e setas não selecionadas em branco puro. Sem fundo quadrado; lista de opções conserva seu tamanho anterior.
- Tela cheia retirada das opções. A constante antiga fica apenas para compatibilidade; um ajuste antigo desse tipo é normalizado para Automático na leitura do novo armazenamento.
- Ajuste automático mantido. Continua esticando os eixos para preencher sem corte. Não há detecção ou recorte automático de barras pretas gravadas no arquivo. O relato de Homem-Aranha: Um Novo Dia é compatível com esse caso, mas a fonte não foi testada aqui. Manual/Zoom permitem ocultar regiões da imagem; o resultado fica salvo apenas para aquele filme.
- Preservados: seleção do catálogo em blocos, menus translúcidos, coração pelo mesmo componente da ficha, favoritos em 2 segundos, CH, imagens e retomada sem sombra.

## Aplicar

Extraia o ZIP e execute na pasta interna `player-ajuste-salvo`:

```powershell
$projeto = 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
& '.\Aplicar.ps1' -Projeto $projeto -Testar
```

Após BUILD SUCCESSFUL, com a TV conectada por ADB:

```powershell
& '.\Instalar-TV.ps1' -Projeto $projeto
```

Backup: `.player-ajuste-salvo-backups` no projeto. O instalador valida os fontes e protege alterações desconhecidas. A instalação usa `adb install -r`, sem apagar dados do aparelho.

Restaurar fontes: `& '.\Desfazer.ps1' -Projeto $projeto`. Preferências de enquadramento continuam no aparelho. Esta entrega contém fontes e scripts; o APK é gerado com as configurações do seu projeto.

## Conferir na TV

1. Ajustar um filme, sair/reabrir e reiniciar o app: recuperar o ajuste. Abrir outro filme: não herdar o enquadramento do primeiro.
2. Ajustar T1 de uma série, abrir outro episódio de T1: recuperar; abrir T2: configuração independente. Repetir após reiniciar o app e ao trocar playlist/Kids.
3. Conferir controles centrais grandes e brancos em Zoom/Manual; setas, OK, Voltar e fechamento por inatividade; verificar ausência de Tela cheia.
4. Conferir enquadramento em Exo/VLC, inclusive vídeo pausado, CH, favoritos e retomada. Automático deve manter o comportamento anterior.

Compilação, Lint, testes da persistência/escopos e instalador verificados no ambiente de desenvolvimento. Aparência, foco do controle e fontes reais precisam de validação no aparelho; não houve acesso à TV ou ao arquivo do Homem-Aranha.
