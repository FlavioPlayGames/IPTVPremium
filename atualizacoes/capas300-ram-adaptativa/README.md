# Capas 300 x 450 e RAM adaptativa

Base: restaurar-links-originais (8e28760). **JPEG permanece em qualidade 90.** O ajuste nao modifica Gradle, configuracoes privadas nem a navegacao por controle.

- Capas maiores passam a ter no maximo 300 x 450 pixels, mantendo proporcao, sem corte nem ampliacao das pequenas. Arquivos existentes continuam reaproveitados e sao reduzidos na RAM; novos downloads grandes sao reduzidos ao gravar no disco. Nao ha conversao em massa ou limpeza forcada.
- O carregador de cards e o fallback do carrossel solicitam RGB_565 para imagens opacas. Transparencia permanece em ARGB_8888; PNG/WebP sao tratados conservadoramente e so convertidos se o bitmap declarar ausencia de alpha. A reducao em disco preserva alpha em PNG quando necessario, sem alterar a qualidade JPEG 90. Fundos, logos e artistas nao passam pelo decoder de capas.
- Contagem dos caches usa allocationByteCount real (arredondada para cima em KB no cache de cards), substituindo a suposicao fixa de quatro bytes por pixel. Uma capa JPEG 300x450 em RGB_565 usa aproximadamente 264 KiB, fora objetos/texturas.
- ImageRamPolicy calcula um teto conjunto de ate um quinto do heap informado, limitado a 24 MiB em aparelhos low-RAM/ate 1,5 GiB fisicos e 56 MiB nos demais. Divide entre posters, detalhes e carrossel. Exemplo: heap de 128 MiB em aparelho low-RAM -> 10/10/4 MiB. O teto dos caches nao e um limite para o PSS inteiro, frames do player ou bitmaps ainda retidos pela interface.
- Ao ocultar o app, preserva ate 75% do limite de cada cache. Sinais de pressao liberam progressivamente metade ou mais; situacoes criticas/OOM ainda podem esvaziar. Nao recicla bitmaps que a interface possa manter.
- Retira a regra que esvaziava os tres caches a partir de PSS fixo em 280 MB. Monitora pressao sinalizada pelo Android, lowMemory do sistema e ocupacao de heap com histerese 80%/65%. Antecipacao pausa durante pressao e retoma quando aliviada. Nao confunde memoryClass com teto de PSS.
- Registra apenas cinco hashes de arquivos da ultima janela visivel num marcador de ate 1 KiB dentro da cota de imagens. Nao grava URLs ou credenciais. A Home espera a importacao terminar e a interface estabilizar, e prepara somente esses cinco arquivos ja existentes, sem rede, varredura de capas ou bloqueio da abertura. Esse preparo fica disponivel depois de visitar um catalogo nesta versao. Se arquivos ja tiverem sido removidos pela rotacao, simplesmente ignora.
- A preferencia de retencao passa a ser chamada para os arquivos recentes e os posters reais do carrossel. E prioridade LRU dentro da cota, nao protecao permanente contra remocao. O carrossel consulta a RAM na primeira composicao.
- Mantem links originais, grupos de cinco e reserva de quinze, importacao Xtream 250/M3U 2000, tamanhos visuais e animacao. Os limites de armazenamento permanecem 100 MB compartilhados por imagens e 350 MB para cache gerenciado; o novo marcador tambem e contabilizado.

Limpar o cache do Android ainda apaga as imagens. Forcar parada perde a RAM; a preparacao local reduz trabalho na proxima entrada, sem garantir instantaneidade em qualquer TV.

Instalador verifica todos os arquivos antes de alterar, cria backup exato em `.navigation-backups/capas300-ram-adaptativa-*` e pode ser reaplicado. Guardas de navegacao, importacao e cotas de armazenamento nao sao modificados.

Testes cobrem resolucao, proporcao, alpha, RGB_565 e sua alocacao, orcamentos/pressao, grupos de cinco, reuso com servidor offline, preparo de cinco hashes sem enderecos, limpeza parcial/critica, foco/retorno e cotas de armazenamento. A qualidade visual e o desempenho na TV precisam ser comparados no aparelho.

Validacao concluida: 37 testes locais passaram; APK debug e APK de testes compilados. Instalador PowerShell validado com LF/CRLF, backup, reaplicacao, rejeicao de fonte editada e rollback apos falha de gravacao. Nao houve medicao de FPS ou PSS na TV do usuario.
