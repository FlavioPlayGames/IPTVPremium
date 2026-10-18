# Alternativa local para miniaturas — 1.06 / interno 8

Os registros do emulador mostraram RuntimeException / frame_ausente. Isso confirma que a extração foi tentada, mas não determina a causa nativa: codec, formato, conexão e compatibilidade de MediaDataSource ainda precisam ser distinguidos no dispositivo.

Mantém fotos específicas existentes e o fluxo remoto de extração. Se a fonte remota falhar ou não gerar frame, tenta uma única alternativa: copia um prefixo limitado do próprio episódio para arquivo temporário e passa seu descritor ao MediaMetadataRetriever. Sem novo player ou dependência. Nunca substitui por uma capa genérica da série.

O arquivo temporário tem até 6 MiB, usa a pasta contabilizada no orçamento combinado de imagens (64 MiB) e é protegido enquanto estiver em uso. É apagado após sucesso, erro, cancelamento ou frame nulo. Só um extrator por vez. A tentativa remota e a alternativa possuem até 6 MiB e prazo de leitura de 10 s cada: até 12 MiB lidos em duas tentativas, com timeout HTTP em andamento podendo terminar depois do prazo. O decodificador nativo é síncrono e não tem limite rígido de execução. Não baixa o episódio inteiro; um episódio menor que o limite pode ser lido por completo. O JPEG obtido fica no cache existente. Não muda Cloudflare, reprodução, episódios, favoritos, progresso ou temas.

Limitações: MP4 que precisa de metadados na cauda além do prefixo, formatos de playlist e codecs sem suporte ainda podem não gerar quadro. A alternativa por arquivo não é garantia de suporte a todos os streams. Falhas continuam com intervalo de dois minutos por sessão. Instalar por cima reinicia o processo, permitindo novo teste sem limpar dados.

Diagnóstico seguro EpisodePreview: extracao_falhou via=remota/local etapa=fonte_remota/frame_remoto/leitura_local/fonte_local/frame_local tipo=Classe; frame_nulo; frame_extraido com etapa; frame_ausente. Nunca registra URL, usuário, senha ou texto da exceção.

Exige o último pacote Recuperacao instalado nos fontes; confere os dois arquivos antes de alteração, faz backup e preserva assinatura/configuração local. Mantém 1.06 / interno 8. Instalar APK por cima. -Verificar somente confere; -NaoCompilar aplica sem compilar. O script não instala nem publica APK.

Validação: dez testes do arquivo temporário contra HTTP real local (bytes reais, limite 6 MiB, sucesso, erro nativo simulado, cancelamento, HTTP 503, limpeza), dez testes da integração com extrator Android substituído por fixture (falha de fonte remota seguida de sucesso local, frame nulo, cache, intervalo de falhas, liberação e logs), onze regressões do parser/escolha das fotos. Compilação, lint, cinco APKs Android 9 e seis testes PowerShell. Esses testes não executam o decodificador nativo do emulador nem comprovam extração em NCIS Origins. Testar episódios 3 e 4 no emulador e, se necessário, recolher os novos registros EpisodePreview.
