# Reações somente na caixa “Sua avaliação”

Base: o último java.zip enviado. Modifica apenas ReferenceDetailHeader.kt. Mantém as estrelas da página de informações sem rostos, partículas ou animação.

## Ao confirmar a nota

- 1 ou 2: as estrelas preenchidas balançam suavemente e voltam ao lugar, em 0,8 segundo.
- 3 ou 4: as estrelas preenchidas aumentam discretamente, sorriem e retornam, em 0,8 segundo.
- 5: a quinta cresce, se desfaz em 12 estrelinhas e se forma novamente, em 1,5 segundo. As quatro primeiras ficam estáveis.

A nota começa a ser salva imediatamente, pelo mecanismo existente. Ao terminar a animação, fecha somente a caixa: você permanece nas informações do filme/série. Voltar ou Fechar permite sair antes; a gravação já iniciada permanece protegida pelo código existente. Remover avaliação não tem animação e fecha diretamente.

A animação não acontece só por abrir a caixa, recuperar uma nota salva ou mover o foco. Durante uma reação, cliques adicionais nas estrelas não disparam novas gravações; é possível mover o foco e fechar a caixa. O tamanho da caixa permanece estável caso já existisse avaliação. O botão Remover fica indisponível durante a reação para não conflitar com a confirmação.

São estrelas vetoriais, mantendo cinco pontas. Não usa GIF, imagens adicionais, animação infinita, nem cria testes no aplicativo. O armazenamento, identidade das avaliações e nota pública do provedor permanecem iguais. Falhas de gravação continuam mostrando o aviso existente e revertendo a nota exibida.

## Aplicar

```powershell
$base='https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/reacoes-avaliacao-dialogo-20261022'
$script=Join-Path $env:TEMP 'Preparar-Reacoes-Avaliacao.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Reacoes-Avaliacao.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O instalador verifica o arquivo e guarda backup em `.rating-dialog-backups`. Se o código tiver mudado desde o ZIP enviado, interrompe antes de escrever. Não troca fundos, fontes, imagens, player ou importação. Depois de BUILD SUCCESSFUL, gere/instale o APK normalmente. O script não instala automaticamente.

## Conferir na TV

Abra Sua avaliação, confirme uma nota de cada faixa e aguarde o fechamento automático para a mesma página. Reabra e confirme que a nota foi mantida. Teste também Voltar durante a animação e Remover avaliação. Não é necessário apagar dados.

Consulte verificacao.txt. A compilação foi verificada numa estrutura Android isolada; não houve execução visual/controle remoto no dispositivo nesta máquina. A escala de duração de animações do sistema pode reduzir ou suprimir o movimento.
