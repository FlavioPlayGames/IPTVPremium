# XMLTV automatico e retorno ao canal

M3U identifica url-tvg/x-tvg-url/tvg-url durante a importacao; Xtream usa xmltv.php no mesmo servidor. Listas existentes tambem descobrem a fonte automaticamente. O guia baixa XMLTV em segundo plano, associa pelo identificador EPG ou nome exato e unico, e prioriza portugues com ingles como fallback. Se a fonte nao funcionar, permanece a API atual. Sem traducao automatica.

Inclui os retornos pendentes de Canais: Esquerda no video/EPG, Voltar em Organizar/Pesquisar e saida da tela cheia levam a categoria e linha do canal em reproducao. Visitar outras categorias sem confirmar canal nao muda essa referencia. Sem requesters antigos de linhas descartadas.

Base: canais-historico-tv 3cb7c03. Baixe xmltv-retorno-canais-tv.zip, execute Aplicar.ps1 -Projeto <pasta> -Testar e Instalar-TV.ps1. Backup e Desfazer.ps1 incluidos; sem desinstalar/limpar dados. Depois de instalar, abra Canais e aguarde o primeiro carregamento do guia em segundo plano; nao precisa reimportar toda a lista.

[Detalhes e limites](LEIA-ME.txt). [Validacao](VALIDACAO.txt). O provedor precisa oferecer XMLTV e descricoes portuguesas para que sejam exibidas em portugues; caso contrario, ingles/API permanecem. Navegacao e dados particulares ainda requerem teste na TV.
