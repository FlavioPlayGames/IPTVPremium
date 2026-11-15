# Sincronia no visual do player e altura da legenda r35

Base: r34 já aplicada em C:\Users\flavi\AndroidStudioProjects\IPTVPremium.

## Instalar

Execute Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium" no PowerShell do Android Studio com a TV conectada/autorizada. O script valida hashes, cria recuperação externa, aplica fontes, compila localmente e instala mantendo assinatura e dados. Não altera o pareamento ADB. Aplicar.ps1 apenas aplica fontes; Desfazer.ps1 -Backup "pasta indicada" -Instalar restaura protegendo edições posteriores.

## Sincronia

- Item Sincronia sempre visível no rodapé de Legendas e de suas opções, sem caixa própria: ícone, nome e valor. Só funciona com legenda ativa.
- Faixa baixa e centralizada no rodapé do vídeo, fundo translúcido já existente do player, comandos sem caixas/contornos individuais e destaque do tema somente no foco.
- Ajuste mostra o intervalo por clique; valor atual; menos Adiantar, mais Atrasar, Zerar, Auxílio e Fechar. As opções de intervalo e cálculos são os existentes.
- Auxílio segue o mesmo visual, mantendo o retorno ao ponto anterior, arredondamento e ausência do antigo desconto de um segundo.
- Tempos salvos por filme/temporada preservados; altura não altera os horários, o seek ou o relógio do player.

## Altura da legenda

Em Legendas → Estilo → Altura da legenda, o menu lateral sai e aparece o exemplo “Esta é a altura da sua legenda.” sobre o próprio vídeo, mesmo sem fala/legenda naquele instante. A faixa compacta fica no alto para não cobrir a posição ajustada. Use Subir/Descer ou setas ↑/↓; Padrão restaura somente a altura. Fechar/Voltar retorna a Estilo e remove a frase de exemplo.

Salvamento automático junto ao estilo, global neste aparelho, mantido após reabrir. O valor inicial 8 preserva a posição anterior; os limites são de 0 a 80 e cada clique muda um ponto percentual. Mudanças de cor/fonte/tamanho preservam a altura. Receber estilo pelo site também preserva a altura da TV, pois o formulário atual não transmite esse campo. Restaurar padrão de todo o estilo restaura também a altura.

Aplica-se às legendas de texto externas nos dois players e às internas do Exo Player. Legendas internas do VLC não expõem posição neste adaptador: o menu informa para usar Exo Player nesse caso, sem simular aplicação na legenda nativa. Legendas em imagem preservam sua apresentação. A prévia em Aparência das legendas acompanha a posição salva.

## Escopo

Sem alteração de temas, cores, fundos estáticos, arquivos/imagens, importação, capas, cache, limites de armazenamento ou código do site. Uma nova preferência inteira de altura e a preservação dessa preferência ao receber estilo são as únicas mudanças de persistência. QR, credenciais, APIs e cálculos de sincronia permanecem. Não há segundo player ou nova biblioteca.

Confira VALIDACAO.md. As propostas visuais não são capturas reais do APK; ainda é necessário conferir foco/legibilidade e posição na TV.
