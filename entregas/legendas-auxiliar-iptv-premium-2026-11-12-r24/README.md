# Legendas e auxiliar de sincronização — r24

Pode aplicar diretamente sobre r20, r22 ou r23. Não exige instalar r23 antes. Não aplicar sobre r21: ela restringia todos os idiomas do SubSource. O aplicador escolhe o manifesto pela base e valida todos os hashes antes de alterar fontes. A recuperação fica fora do projeto, em Downloads/IPTV-restauracao-r24-....

## Idiomas

- Português Brasil e Português Portugal aparecem em grupos separados; inglês e espanhol continuam disponíveis no OpenSubtitles e no SubSource.
- OpenSubtitles pesquisa pt-br,pt-pt,en,es e distingue os dois códigos na resposta, conferindo série, temporada e episódio.
- SubSource pesquisa brazilian_portuguese, portuguese, english e spanish. Sua categoria portuguese fica no grupo Portugal. Mantém até cinco páginas e 30 opções por idioma (até 21 requisições de busca contando a obra), com intervalo e tratamento de limites preservados. A confirmação do filtro autenticado com brazilian_portuguese ainda depende da TV; o relatório real já comprovou esse código nas respostas sem filtro.
- SubDL solicita os códigos BR_PT,PT encontrados no relatório real, mantém até 30 opções por grupo regional e verifica os arquivos individuais de um pacote. A categoria PT fica no grupo Portugal. A busca filtrada com esses códigos precisa ser confirmada na TV; não se presume que a consulta sem filtro anterior valide todos os parâmetros.
- Faixas internas sem região explícita continuam no grupo Português. Uma faixa interna pt não é rotulada como portuguesa de Portugal por suposição.
- Uma legenda SubDL brasileira salva não encerra a pesquisa antes de procurar o outro grupo regional. O atalho local só encerra se ambos os grupos já tiverem arquivos salvos; o cache normal de pesquisa continua evitando consultas repetidas.
- Cache de pesquisa renovado para descobrir o idioma novo e ignorar resultados vazios anteriores. Arquivos de legenda, conta e ajustes salvos preservados.

## Auxiliar

1. Selecione uma legenda externa, abra Sincronizar legenda e escolha Auxiliar na faixa inferior.
2. Leia a explicação e escolha Iniciar. O vídeo volta ao começo e reproduz. A medição só é armada após confirmar a reprodução perto do início; se não confirmar em 15 segundos, cancela.
3. Com o botão Primeira fala em foco, pressione OK ao ouvir a primeira fala.
4. O cálculo substitui o ajuste anterior: instante do clique no vídeo - início da primeira entrada no arquivo - 1 segundo de reação. Limite de ajuste preservado em +/-120 segundos.
5. O ajuste é salvo por temporada em séries e por filme individual, com perfis normal/Kids separados, até alterar ou zerar. Volta ao ponto de reprodução anterior e restaura o estado de reprodução/pausa. Atrasar, Adiantar, Ajuste e Zerar continuam disponíveis.

O auxiliar é uma estimativa humana. O primeiro texto pode ser crédito ou música; versões com cortes e diferenças de FPS podem exigir ajuste manual. Não analisa áudio nem realiza sincronia automática. Cancela sem salvar um novo valor ao usar Cancelar/Voltar ou ir para segundo plano. O ponto de histórico fica protegido durante o retorno temporário ao início e até a confirmação de volta ao ponto anterior. Se o player rejeitar o retorno, mostra mensagem e deixa de proteger a posição antiga.

Disponível somente para legendas externas já carregadas, em filmes/episódios que permitam busca de posição. Usa o controlador comum ao ExoPlayer e ao VLC e o renderizador externo já existente. Não baixa outro arquivo nem altera os horários originais do SRT/VTT. A explicação tem altura maior; durante a medição, a faixa fica com os mesmos 48 dp da barra manual, abaixo da legenda, centralizada, no tema azul/ciano existente.

## Instalar

Use o PowerShell em que a TV está conectada e autorizada:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\legendas-auxiliar-iptv-premium-r24\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O script compila desempenho e instala com assinatura local usando adb install -r. Não desinstala, não apaga dados e não muda pareamento. Não precisa atualizar a lista nem publicar o site. Nenhuma credencial, chave, assinatura ou APK assinado é incluído no pacote.

## Diagnóstico e restauração

Coletar-Diagnostico.ps1 -AoVivo grava Downloads/IPTV-legendas-r24.txt. Aceita Portugal e o novo evento ASSIST com tempos numéricos do vídeo, ajuste e cue. Não exporta texto da legenda, URL ou chave. Abra a pesquisa ou execute o auxiliar com o coletor ativo e encerre por Ctrl+C.

O aplicador mostra o comando exato Desfazer.ps1 -Backup "caminho". Acrescente -Instalar para compilar e reinstalar a base restaurada. A restauração recusa sobrescrever edições feitas depois da aplicação. Não reverte escolhas/ajustes que o usuário salvou na TV.

## Conferência na TV

- Pesquisar filme e série; conferir os dois grupos de português e as opções em inglês/espanhol dos provedores correspondentes.
- Selecionar uma legenda, usar Auxiliar no ExoPlayer e no VLC. Verificar foco, preparação, marcação e retorno ao ponto anterior.
- Conferir que Cancelar, Voltar e segundo plano não alteram o ajuste salvo e preservam Continuar.
- Testar refinamento manual e Zerar; reabrir o app, trocar de episódio da mesma temporada e comparar com outra temporada/filme.

Limites de cache de arquivos, importação em lote de 2.000, posters e regras de assinatura não mudaram. O uso total de armazenamento/RAM na TV não foi medido; esta entrega não comprova o teto global de 400 MB.
