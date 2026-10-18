# Séries e miniaturas — 1.06 / interno 8

Base analisada: src.zip enviado pelo usuário. O EpisodeFrameLoader enviado correspondia à versão com apenas MediaMetadataRetriever remoto; os seis fundos de temas enviados diferiam do workspace. Os fundos enviados foram preservados no build de validação e não fazem parte deste patch, evitando sobrescrever o visual local. O instalador aceita o extrator enviado ou o da última entrega Frame-Local e confere todos os fontes antes de escrever.

## Carregamento

- Cache local somente de detalhes já consultados: até 40 séries, 12 MiB totais, 4 MiB por entrada e expiração de seis horas. Fica em cacheDir/series_episode_details. Não baixa todo o catálogo. Além dos limites de RAM já existentes, há esse limite separado em disco. Não usa Cloudflare.
- Chave por lista, geração, ID/endpoint e credenciais, com nome SHA256. Credenciais não aparecem no nome dos arquivos. Serializa somente os campos necessários de temporadas/episódios, excluindo direct_source e dados de autenticação presentes na resposta. Miniaturas podem conter as URLs normais do provedor; dados ficam no cache privado do app.
- Cache válido abre sem nova consulta mesmo após reiniciar o processo. Atualizar a lista/geração ou alterar credenciais invalida a chave. Limpar cache remove esses detalhes e permite reconstruí-los. Favoritos e progresso permanecem no armazenamento próprio.
- O endpoint get_series_info devolve temporadas juntas: preserva uma única consulta, evitando repetir o download por temporada. A tela prioriza a temporada do último episódio confirmado nesta lista (ou seleção manual da rota). Sem episódio seguro, usa a primeira temporada disponível; não inventa vínculo para históricos somente da série. Apenas as imagens da temporada exibida são solicitadas. Outras temporadas de episódios já vêm na mesma resposta e ficam no cache.
- Foco antecipa consulta após 150 ms (era 450). Clique e foco continuam compartilhando o resultado pelo cache existente. Aquecimento mantém limite de duas consultas. Não antecipa o catálogo inteiro.
- get_series_info usa conexão de 5 s e leitura de 8 s de inatividade; consultas de catálogo permanecem com os prazos anteriores. Cancelar fecha a conexão; erro de socket causado por cancelamento é convertido em cancelamento, sem virar falha normal nem gerar novas tentativas.
- Mantém recuperação automática limitada já existente. A primeira consulta de uma série ainda depende da rede/provedor; não promete carregamento instantâneo sem resposta disponível. Dados em cache podem refletir até seis horas atrás, como o TTL em memória já existente.

## TV durante o carregamento

A sala e a TV original permanecem. O painel já mostra um indicador animado (anel giratório e pulso suave) na cor do tema, com o nome da série. Não usa ponto/linha/flash para simular a TV ligando. Quando os dados estão prontos, o indicador sai em 180 ms, a página aparece no mesmo painel e continuam o enquadramento, o zoom de 420 ms e o fade final de 70 ms já programados. Mantém o marco de 1.580 ms antes da aproximação. Erro libera a página de erro; Voltar cancela a entrada. Salvaguarda de 60 s para não prender a navegação indefinidamente.

Os episódios reais e a sala precisam estar prontos. O fundo continua carregando, mas ganha tolerância máxima de 1,2 s após os episódios ficarem prontos: uma imagem lenta não segura temporadas já disponíveis. O teste nativo também verifica que a TV mantém o carregamento enquanto ready=false e só termina a entrada após ready=true.

## Imagens somente nos cards vazios

Preserva fotos específicas de TMDB/provedor e o JPEG já extraído. Só quando não houver foto tenta extrair o próprio episódio. Novo caminho usa os extratores de formatos do Media3 Inspector 1.11.1 (mesma versão do player) e MediaCodec do Android, com conversão YUV para miniatura de até 640x360. Não abre player extra nem áudio. Sem Bitmap 4K intermediário. Apenas um extrator por vez, fora da UI.

O caminho Media3 lê até 6 MiB / 24 requisições por faixas e prazo de leitura de 10 s; a decodificação verifica um orçamento de 12 s entre operações. Se não funcionar, tenta MediaMetadataRetriever remoto como alternativa com seu próprio limite de 6 MiB / 10 s. Até 12 MiB de dados lidos no pior caso; chamadas HTTP/nativas em andamento podem terminar depois dos prazos. Não baixa o vídeo inteiro (um arquivo menor que o limite pode ser lido por completo). Remove a alternativa anterior por prefixo temporário, que não resolve contêineres com metadados na cauda. O leitor por faixas pode buscar a cauda sem percorrer o filme todo quando o servidor suporta Range.

Limitações reais: codecs/perfis não suportados pelo emulador/TV, YUV de 10 bits sem saída compatível, DRM, HLS nesta extração, servidores sem Range quando os metadados estão além do prefixo e arquivos maiores que 4K podem continuar sem imagem. O player mantém seus próprios mecanismos e não é alterado. JPEGs usam o cache combinado de imagens existente de 64 MiB. Falhas mantêm intervalo de dois minutos por sessão. Logs EpisodePreview informam via/etapa/classe sem URL, senha ou texto da exceção.

## Instalação e teste

Preparar-Series-Rapidas-1.06.ps1 aplica 16 arquivos com backup/preflight/rollback e preserva configuração privada, assinatura e versão 1.06 / interno 8. -Verificar confere sem alterar; -NaoCompilar aplica sem build. Preserva fundos, temas, favoritos, progresso e listas. A animação de entrada da série recebe o ajuste pedido abaixo. Não publica APK nem altera o servidor.

-TestarFrames compila também o APK de testes, instala por cima o APK universal e o APK de testes APENAS no emulador informado (-Emulador emulator-5554 por padrão) e executa três provas nativas: frame azul real de um MP4 H264 e de um MKV H264, mais a espera/continuação da entrada animada servidos por HTTP local dentro do emulador. Sem credenciais/lista/rede externa no teste. Salva diagnostico-frames-emulador.txt na raiz. O teste não cobre os streams reais de NCIS. Sem -TestarFrames, apenas compila os cinco APKs e abre a pasta, como as entregas anteriores. Não é necessário desinstalar ou limpar dados.

## Validação nesta máquina

Compilação assembleDebug, assembleDebugAndroidTest e lintDebug; cinco APKs com assinatura válida para Android 9. Testes JVM de recuperação, cache/foco, serialização persistente/invalidação/limites, conversão YUV com strides/crop e HTTP real com cancelamento; regressão da escolha de imagens. Testes PowerShell de preflight, aplicação, idempotência, rejeição de base desconhecida/downgrade, bytes privados e variante do src.zip.

Os três testes nativos foram compilados, mas NÃO executados nesta máquina, que não possui emulador nem /dev/kvm. O script -TestarFrames permite executar e registrar essa validação no emulador do usuário. Não afirmamos que NCIS Origins passou apenas pela compilação. Conferir também os episódios reais 3 e 4 após os testes.
