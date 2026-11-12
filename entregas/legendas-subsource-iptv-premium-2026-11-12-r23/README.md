# Código de idioma SubSource — r23

Base: r20 ou r22 (os dois fontes alterados nessas bases são idênticos). Não aplicar diretamente sobre r21. O aplicador valida hashes antes de modificar arquivos e cria backup externo dos fontes.

O site extraído pelo usuário identifica Português brasileiro como brazilian_portuguese, inclusive no mapa público de idiomas e nas legendas da temporada 5 de The Handmaid’s Tale. O cliente consultava brazillian-portuguese. A r23 usa brazilian_portuguese na consulta. Mantém inglês e espanhol, rejeitando português genérico. A leitura ainda aceita aliases brasileiros antigos para compatibilidade.

A pesquisa passa ao cache v3 para ignorar o resultado vazio anterior, sem apagar dados da TV ou outros caches. Mantém o máximo de 32 pesquisas e os limites existentes dos arquivos. Paginação, pacotes, cache por episódio e ajuste manual continuam iguais. Não muda o site nem implementa o auxiliar de sincronia.

No PowerShell com a TV autorizada, execute Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium". Compila desempenho e instala com assinatura local e preservação dos dados. O aplicador mostra o caminho do backup e o comando para desfazer.

Após instalar, pesquise novamente O Conto da Aia T05E06 e confira Português Brasil → SubSource. Não precisa atualizar a lista. Se não aparecer, execute Coletar-Diagnostico.ps1 -AoVivo e envie Downloads\IPTV-legendas-r23.txt. O cache v3 força uma consulta nova para esse conteúdo na primeira abertura após a atualização.

A extração comprova o código usado pelo site, não uma resposta da API autenticada. A consulta real com o novo parâmetro precisa ser validada na TV.
