# Validação r11

- Compilação final :app:assembleDesempenho :app:lintDesempenho --offline concluída com sucesso. Lint sem erros, 154 avisos e seis sugestões. APKs desempenho gerados para as quatro ABIs e universal. APK arm64 do host: 97.081.030 bytes. Não será distribuído; o usuário compila com sua assinatura.
- O host usou JDK 25/Gradle 9.6 e heap de 6 GiB por argumento, sem alterar as configurações locais do projeto.
- Suíte JVM completa passou: 39 verificações do histórico, pesquisa/limpeza de consulta, compaction de 2.200 fatos, progresso/avanço de episódios, regras de capas M3U, identidade de obra/temporada, reserva de downloads e rotação/orçamento.
- Parsers Xtream reais normal e streaming preservam TMDB/IMDb e distinguem stream_id de TMDB ID; também testados com 1.100 filmes em lotes.
- IDs M3U explícitos sobrevivem à serialização de filmes/séries; tvg-id não vira TMDB ID. O formato temporário de importação foi versionado para evitar leitura silenciosa de uma estrutura antiga.
- Provedor válido precede TMDB; detalhes de provedor sem capa podem fornecer ID e ano. Consulta de metadados extras é deduplicada e uma ausência é reaproveitada sem varrer o catálogo.
- Testes verificam ID antes de qualquer busca, sequência pt-BR/en-US/es-ES com o ano enviado na consulta e rejeição de resultados incompatíveis. Capas de episódio não substituem arbitrariamente a série.
- Observador do cartão não emite resultado genérico enquanto ainda há tentativa pendente; só emite ausência após a última tentativa. Testados retorno à área visível, nova tentativa limitada e conclusão de antecipação.
- SQL exato da migração 6→7 executado em SQLite: valores das linhas existentes preservados, duas colunas novas nulas e inserção M3U real de 15 colunas aceita. KSP/Room e compilação Android também passaram. Isso não substitui um teste instrumentado no SQLite da TV.
- Scripts testados em PowerShell 7 e simulação do retorno JSON do PowerShell 5.1: aplicar, repetir, restaurar mantendo os três arquivos de compatibilidade, reaplicar, arquivos novos, base diferente e edições posteriores. Sintaxe válida; coletor aceita somente contadores/tempos no formato previsto e rejeita campos adicionais/credenciais. Não executado em Windows nativo neste host.
- Os arquivos do orçamento, rotação, plano de reserva, navegação rápida e preparação 300x450 permanecem iguais aos da r10. Nenhum recurso de imagem de alta resolução foi acrescentado. Os limites não foram aumentados; a meta total de 400 MB precisa continuar sendo medida na TV.

As APIs e bitmaps dos testes JVM usam adaptadores/respostas simuladas. Os nomes citados pelo usuário são cenários de teste, não uma afirmação de que os IDs simulados sejam os filmes reais. Na M3U anteriormente enviada foi conferida a variação de imagens de Pesadelo na Cozinha; nenhuma credencial ou URL de reprodução foi publicada.

Tempo de rede, retorno atual do provedor/TMDB e resultado visual dos títulos citados ainda exigem validação na TV. O pacote não inclui APK, chaves, listas completas nem configurações privadas.
