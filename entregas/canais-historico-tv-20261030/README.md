# Canais e historico

Entrada em Canais retoma o ultimo canal confirmado do perfil ou o primeiro permitido; historico normal/Kids separado por playlist. Categorias apenas movem o foco para a primeira linha, sem trocar reproducao. Caminhos explicitos entre lista, categorias, video pequeno e Organizar/Pesquisar; sem liberar o foco por temporizador de dois frames ou focar linhas antigas durante refresh. Logo centralizado e flecha CANAIS maior/alinhada.

Continuar de series usa a capa da serie do catalogo e nao o fundo do episodio. Mesmo filme Xtream/M3U pode ser identificado pelo arquivo do mesmo provedor; series M3U sao ligadas pelo mesmo arquivo de episodio e temporada/numero, usando IDs reais de destino. Sem novos pedidos de rede ou migracao de schema. Titulos/IDs externos e regras seguras anteriores preservados.

Base: java.zip atualizado do usuario. Baixe `canais-historico-tv.zip`, execute `Aplicar.ps1 -Projeto <pasta> -Testar` e `Instalar-TV.ps1`. Para reverter fontes, `Desfazer.ps1 -Projeto <pasta> -Testar`, depois reinstale. Sem apagar dados.

O historico anterior de canais misturava normal/Kids: fica preservado, mas as novas chaves comecam separadas. No primeiro acesso pode tocar o primeiro canal. Para refazer vinculos entre listas, selecione a origem e volte ao destino apos instalar.

[Detalhes e limites](LEIA-ME.txt). [Validacao](VALIDACAO.txt). [Regressoes de historico](TESTES-HISTORICO.txt). Navegacao e lista real precisam ser avaliadas na TV.
