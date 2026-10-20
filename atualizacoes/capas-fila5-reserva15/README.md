# Capas: grupos de cinco e reserva de quinze

Base: atualizacao capas-tmdb-menores (f93f435), sobre o teste 400x600. Instalador verifica todos os arquivos antes de alterar, cria backup exato e nao toca Gradle/configuracao privada.

- Filmes e series usam a mesma regra, restrita a categoria e janela do Paging atuais.
- As capas visiveis continuam sendo solicitadas imediatamente. A fila de fundo comeca depois do ultimo item visivel, aguarda a pausa existente de 100 ms na TV / 80 ms no celular e baixa grupos de ate cinco. Cada grupo termina antes de iniciar outro.
- A reserva chega a quinze posicoes a frente. Quando restam dez ou menos, repoe ate quinze. Saltos maiores podem exigir mais de um grupo; o ultimo pode ter menos de cinco, especialmente em grades de quatro, no fim do catalogo ou da pagina.
- Prepara na RAM somente as proximas cinco a frente, lendo arquivos ja disponiveis no disco. Essa preparacao nunca abre rede. Usa o mesmo LRU de 20 MiB e os mesmos dois slots de decodificacao; nao cria outro cache. Em pressao de memoria, pula a preparacao antecipada.
- A fila de fundo usa as duas conexoes ja existentes; o limite total de rede continua seis. Nao altera animacao, foco, geometria, controle remoto ou configuracao de 4/5 colunas.
- Usa somente itens ja recebidos pelo Paging: nao forca leitura de todo o catalogo ou importacao. Se faltarem itens, continua quando a pagina chegar. Capas indisponiveis nao causam repeticao infinita de tentativas.
- O cursor avanca apos concluir um grupo. Mudanca de foco, rolagem, categoria e saida da tela cancelam a fila pelo ciclo Compose existente; arquivos concluidos permanecem reutilizaveis. Trabalho bloqueado em rede pode aguardar seu timeout para encerrar.
- Cache existente e preservado. Ao reiniciar o processo a RAM precisa ser reconstruida, mas as imagens existentes no armazenamento nao exigem novo download.
- Limites continuam 100 MB para imagens e 350 MB para cache gerenciado. Links TMDB w342/w300, intensidade de cor, importacao, fundos, logos e artistas permanecem como antes. Nao antecipa as quinze imagens como bitmaps residentes.

Backup: `.navigation-backups/capas-fila5-reserva15-*`. Os guardas de navegacao e outros componentes nao sao alterados.

Testes incluem grupos de cinco, gatilho dez/quinze, grades de quatro/cinco, janela parcial do Paging, fim de categoria, cancelamento sem salto de grupo e integracao HTTP local para verificar quinze downloads, reposicao de cinco, apenas cinco futuras na RAM e reuso do disco apos limpar a RAM.

Validacao concluida: 26 testes locais passaram (fila, cache HTTP, resolucao, links TMDB e foco/retorno das categorias). APK debug e APK de testes compilados. Instalador PowerShell validado com LF/CRLF, backup, reaplicacao e rejeicao de fonte editada. Desempenho real na TV ainda depende de teste no aparelho.
