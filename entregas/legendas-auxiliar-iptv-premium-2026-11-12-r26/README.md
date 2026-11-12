# Auxiliar e sinais nos controles — r26

Pode aplicar diretamente sobre r24 ou r25; basta uma instalação. A r26 reúne as mudanças solicitadas: auxiliar sem desconto de um segundo, arredondamento ao segundo inteiro (4,5→5 e 4,499→4, simétrico para valores negativos) e botões + Atrasar / − Adiantar.

O instante inicial da legenda continua sendo descontado do instante do clique. Ajustes já salvos não mudam automaticamente. Os controles manuais mantêm a precisão anterior. Idiomas, provedores, persistência por temporada/filme, histórico e cache permanecem como na r24.

Executar Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium" no PowerShell em que a TV já está autorizada. O script valida hashes, cria backup externo, compila desempenho e instala com assinatura local preservando dados. Nenhuma alteração no site. Não precisa instalar r25 antes.

Recuperação em Downloads\IPTV-restauracao-r26-.... O aplicador mostra o comando Desfazer.ps1 com caminho exato; -Instalar compila e reinstala a base restaurada. Ajustes salvos na TV não são revertidos.

Na TV: Legendas → selecionar legenda externa → Sincronizar legenda → Auxiliar. A explicação agora menciona arredondamento. Executar o auxiliar novamente se quiser substituir uma estimativa antiga. + aumenta o atraso e − adianta a legenda. A medição continua aproximada e pode exigir refinamento manual.
