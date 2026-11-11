# Validação r14

## Compilação Android

`assembleDesempenho` e `lintDesempenho` concluídos em 11/11/2026: 0 erros, 154 avisos e 6 sugestões. APK arm64 de referência: 97,097,414 bytes, não distribuído. JDK 25, Gradle 9.6, SDK 37 e NDK 28.2.13676358 no host. Heap de 6 GiB somente no comando do host; configurações do projeto não foram alteradas.

## Comparação de importação

Leitura do `java.zip` de referência sem executar seu conteúdo. Dez arquivos fundamentais de configuração/rede/sincronização e o trecho progressivo Xtream do repositório coincidem após normalizar BOM/quebras de linha. Ver `COMPARACAO-XTREAM.md` e o comparador reproduzível em `testes/Comparar-Xtream.py`.

Não houve teste de velocidade com o provedor/TV. Não se atribui a demora observada a uma causa específica sem o relatório de importação. A descoberta XMLTV Xtream não executa download do guia no caminho examinado.

## Testes

- Cache de série geral já resolvida evita repetir tentativa no provedor indisponível.
- Provedor válido pode aparecer antes de concluir a busca sazonal; a busca continua e chega à temporada solicitada.
- URL sazonal conhecida é recuperada diretamente após remover o bitmap da RAM; não consulta outra vez o provedor nem a identidade TMDB nesse caso.
- Antecipação completa a escolha da temporada. Temporada ausente não usa a imagem de outra temporada.
- Política real de histórico usa temporada conhecida em associação provisória entre listas, mantém episódios concluídos e especiais, ignora temporada desconhecida e sucessor enfileirado não assistido. Não modifica posição, duração ou validação de retomada.
- Prioridade de importação cancela e retoma consultas de capas/metadados visíveis e de segundo plano.
- Regressões executadas: filmes/vínculos da r13, isolamento entre listas/itens, recuperação persistente, normal/Kids, histórico/transferência, pesquisa global, reserva de capas, rotação/orçamentos e parsers M3U/Xtream com 1.100 filmes.
- Políticas de abertura da r12/r13 novamente verificadas: respostas após cinco segundos, cancelamento, prioridade de conteúdo, série independente de consulta antiga, falhas de imagem sem descartar os dados e carrossel.
- Aplicação/repetição/restauração/reaplicação sobre base r13 e proteção de edições; sintaxe dos scripts. PowerShell 7 com simulação do JSON do PowerShell 5.1, sem execução nativa no Windows.
- Coletor de importação usa os nomes reais das tags, filtra campos de rede/processamento/SQL/ativação/navegação e rejeita URLs, credenciais, endpoints desconhecidos e campos extras. Testado com ADB simulado nos modos gravado e ao vivo.

APIs, bitmaps, relógio e banco Android usam os adaptadores explicitados nos testes JVM. Isso não substitui validação visual ou instrumentada na TV. O relatório anterior de capas continha eventos de filmes e não fornece medição das séries mencionadas nesta etapa; a correção destas séries se baseia nos caminhos identificados no código.

Limites de cache, rotação, imagens de até 300 × 450 e meta total de 400 MB preservados. Não se afirma cumprimento da meta total ou resolução de vazamento sem nova medição de armazenamento/RAM. Não há APK, imagens adicionais, configuração privada, chaves ou listas completas no pacote.
