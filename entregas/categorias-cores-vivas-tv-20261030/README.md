# Retorno as categorias e cores vivas

O retorno executava scrollToItem mesmo quando a categoria selecionada ja estava visivel, causando reposicionamento desnecessario. Agora preserva o offset atual e apenas devolve o foco; se a categoria estiver cortada/ausente, traz para a tela.

Amarelo aprovado #FBBC04 tambem no escuro. Azul #007AFF, Rosa #FF2384, Roxo #AF32FF, Vermelho #FF3B30 nos dois modos. CatalogStyle.borderColor passa a usar o destaque da interface em vez do antigo tom metalico. Arte da Home, textos neutros, player e motor de rolagem preservados.

Base 32f4dac. Baixe categorias-cores-vivas-tv.zip, execute Aplicar.ps1 -Projeto <pasta> -Testar e Instalar-TV.ps1. Backup e Desfazer.ps1 incluidos, sem apagar dados. [Validacao e limites](VALIDACAO.txt).
