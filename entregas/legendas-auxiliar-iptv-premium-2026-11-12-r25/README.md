# Arredondamento do auxiliar — r25

Aplicar sobre r24. Altera somente o cálculo e o texto da explicação do auxiliar: não desconta um segundo de reação; arredonda clique no vídeo menos primeiro horário da legenda ao segundo inteiro mais próximo. 4,5 s vira 5 s; 4,499 s vira 4 s. Valores negativos seguem a mesma regra em módulo: -4,5 vira -5 e -4,499 vira -4. Limite original de +/-120 s preservado.

Aplica-se aos próximos resultados do auxiliar. Não muda ajustes já salvos nem a precisão dos controles manuais. Persistência por temporada/filme, retorno ao ponto anterior, idiomas, cache e provedores permanecem como na r24. Não muda o site.

Executar Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium" no PowerShell com a TV já autorizada. O script valida hashes, salva backup externo, compila desempenho com assinatura local e instala preservando dados. O backup fica em Downloads\IPTV-restauracao-r25-.... O aplicador informa o comando para desfazer; Desfazer.ps1 -Backup "caminho" -Instalar restaura fontes e reinstala. Alterações posteriores nos fontes bloqueiam a restauração por segurança. Os ajustes salvos na TV não são revertidos.

Na TV: selecionar legenda externa → Sincronizar legenda → Auxiliar. A explicação agora indica arredondamento e não desconto de reação. Medir novamente se quiser substituir uma estimativa antiga; refinar manualmente continua disponível. A sincronização é aproximada e depende da primeira fala escolhida e da edição do vídeo.
