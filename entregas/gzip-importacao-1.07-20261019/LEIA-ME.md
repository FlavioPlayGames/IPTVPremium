# Compactação das listas — 1.07

O Android HttpURLConnection normalmente já oferece Gzip transparente quando Accept-Encoding não é definido. Esta entrega NÃO presume que a versão anterior transferia tudo sem compactação, nem promete acelerar um provedor que já comprimia as respostas.

Agora negocia explicitamente Accept-Encoding: gzip nos três modos do cliente de listas (GET JSON Xtream, reader e linhas M3U). Essa configuração desativa a descompactação implícita do Android: um único decodificador Gzip em fluxo lê a resposta e permite medir bytes de rede e bytes descompactados. Respostas sem Content-Encoding/identity continuam aceitas. Gzip inválido gera erro, não texto corrompido. Brotli não é anunciado: não há decodificador Brotli no cliente e não foi adicionada dependência.

Somente buffer fixo de 32 KiB para Gzip, sem uma nova cópia integral da lista. A API get continua retornando String, como antes; reader/linhas continuam em fluxo. Mantidos timeouts, cancelamento existente, erros HTTP, credenciais e importação. Não muda servidor do provedor, site, velocidade da rolagem, banco, histórico ou imagens. O ganho real depende da resposta do provedor e de qual etapa domina a importação. Sem medição de PSS na TV.

Logcat PlaylistConnection: COMPACTACAO endpoint=XTREAM_DATA codificacao=gzip bytesRede=... bytesTexto=... . Não registra URL, senha ou corpo. Os bytes de uma leitura parcial correspondem apenas ao trecho consumido, podendo incluir leitura antecipada do buffer; não são necessariamente o tamanho completo da resposta.

Nove testes passam na JVM com servidor HTTP local usando o cliente de produção e um substituto de android.util.Log: seis combinações de modo/resposta, Gzip corrompido, HTTP 403 e contagem de bytes em um corpo maior sem dupla descompactação. App e APK de testes compilados. Os mesmos nove testes são incluídos para rodar no Android com -TestarCompactacao; não foram executados em emulador nesta nuvem.

Instalador valida hashes, faz backup e instala por cima, sem limpar dados. Resultado diagnostico-gzip-importacao.txt. Depois importe uma lista real e confira as linhas COMPACTACAO: codificacao=identity indica que o servidor não comprimiu aquela resposta. Nenhum teste com credenciais reais foi realizado nesta entrega.
