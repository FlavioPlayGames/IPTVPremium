# SubSource somente português brasileiro — r21

Base exigida: r20. O aplicador valida hashes, salva recuperação externa e permite desfazer.

A consulta do SubSource usa somente brazillian-portuguese (grafia do código da API). O filtro da resposta rejeita inglês, espanhol e português genérico. O filtro do cache e das opções já baixadas também impede que opções antigas de outros idiomas do SubSource reapareçam. SubDL e OpenSubtitles mantêm suas regras anteriores. A paginação fica em até cinco páginas de 100 resultados para o único idioma, sem requisições de inglês ou espanhol.

Não muda o site, credenciais, ajuste manual, assinatura, versão, importações ou cache de arquivos. Não apaga dados da TV. O auxiliar de sincronia não faz parte desta entrega.

Use Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium" no PowerShell em que a TV está autorizada no ADB. O script compila desempenho e instala com a assinatura local, preservando dados. A base r20 precisa estar aplicada. Backup em Downloads\IPTV-restauracao-r21-.... O aplicador mostra o comando para restaurar; Desfazer.ps1 -Backup "caminho" -Instalar compila e reinstala os fontes restaurados.

Teste na TV: abrir um filme e um episódio, conferir que o SubSource aparece apenas em Português Brasil. Inglês e espanhol dos outros provedores continuam com o comportamento anterior. API real não foi acessada neste ambiente; requer validação na TV.
