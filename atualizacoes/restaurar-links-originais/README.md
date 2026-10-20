# Restaurar links originais das imagens

Requer a atualizacao capas-fila5-reserva15 (63e97fb).

Remove somente a troca de tamanho do endereco TMDB introduzida em capas-tmdb-menores. Capas de filmes, series e carrossel voltam a baixar diretamente do link recebido da lista. Miniaturas de episodios voltam a usar diretamente o endereco recebido pelo carregador, inclusive o endereco fornecido pela consulta de metadados quando existente. Nao forca original no TMDB: se o link fornecido ja indicar w342 ou w300, continua com esse tamanho.

Antes, um link TMDB maior era tentado primeiro em w342 ou w300; uma falha podia causar espera adicional antes de tentar o endereco original. Essa e uma causa possivel, sem medicao na TV do usuario para afirmar que foi a causa da lentidao relatada. Agora ha apenas a tentativa com o link fornecido, sem endereco alternativo injetado.

Preserva fila de ate cinco, reserva de quinze, preparacao local das proximas cinco na RAM, reducao local de capas ate 400x600, cotas de 100 MB para imagens e 350 MB para cache gerenciado, navegacao e importacao. Nao esvazia o cache nem refaz downloads: imagens ja salvas menores continuam reaproveitadas ate sua rotacao natural. Fundos, logos e artistas nao recebem alteracoes adicionais.

O helper PosterDownloadUrl e seus testes antigos podem permanecer no projeto, mas nenhum carregador de producao o chama depois desta atualizacao.

Instalador verifica a base antes de escrever e cria backup exato em `.navigation-backups/restaurar-links-originais-*`. Inclui apenas tres alteracoes de producao; os demais arquivos sao guardas de compatibilidade e nao sao modificados. Nao inclui Gradle ou configuracao privada.

Validacao concluida: 19 testes locais passaram (fila, cache HTTP, reuso apos limpar RAM, resolucao, foco e retorno das categorias). APK debug e APK de testes compilados. Instalador PowerShell validado com LF/CRLF, backup, reaplicacao e rejeicao de fonte editada. Sem medicao de desempenho no aparelho do usuario.
