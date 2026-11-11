# Validação r10

- Compilação Android final: :app:assembleDesempenho :app:lintDesempenho --offline, concluída com sucesso. Zero erros de Lint, 154 avisos e seis sugestões. Os avisos nos arquivos modificados são sugestões de usar SharedPreferences.edit do KTX.
- Foram gerados APKs desempenho para arm64-v8a, armeabi-v7a, x86, x86_64 e universal. O pacote distribuído contém somente fontes/scripts/testes; o usuário compila com sua assinatura. APK arm64 do host: 97.048.262 bytes. Não há promessa de redução relevante do APK: o objetivo desta etapa é o carregamento de capas.
- O host usou JDK 25/Gradle 9.6 com heap de 6 GiB por argumento de execução, sem alterar gradle.properties do projeto.
- Testes JVM: provedor válido sem pesquisa desnecessária, capa ausente, provedor quebrado, fallback verificado para filmes, qualidade 4K/HD removida da consulta, reutilização de cache, recusa de homônimos/ano/título incompatíveis, capas de temporada e preservação dos nomes com e/&.
- Testes de prioridade: duas antecipações pendentes não ocupam ambos os espaços de busca TMDB; uma capa visível consegue ser resolvida enquanto a antecipação continua bloqueada. Imagens lentas não retêm os espaços de busca de metadados.
- Observação do cartão: uma nova tentativa limitada após falha, retorno à área visível e notificação de antecipação concluída; itens invisíveis não iniciam essas buscas.
- Falhas transitórias/limites/prioridade não recebem bloqueio longo; 404/410 são reconhecidos. Redução dos endereços TMDB original/w500/w600_and_h900_bestv2 para w342, preservação de query e de endereços de outros provedores.
- Parsers Xtream reais normal e streaming conferidos com campos alternativos, endereços inválidos e 1.100 filmes em múltiplos lotes. O adaptador de JsonReader no teste JVM usa Gson para JSON válido; não substitui testes Android de respostas malformadas.
- Suíte anterior passou: 39 verificações do histórico r9, pesquisa global/limpeza da consulta, compaction de 2.200 fatos, avanço de episódios, políticas de imagens M3U, reserva ordenada e rotação/orçamento.
- Os arquivos de orçamento, rotação, plano de reserva, navegação rápida e preparação 300x450 permanecem byte a byte iguais aos da r9. Todos os fontes do histórico r9 também permanecem iguais.
- Aplicador testado em PowerShell 7: aplicação, repetição, restauração, arquivos novos, base diferente e edição posterior; também com simulação do retorno JSON do Windows PowerShell 5.1. Scripts passam pela análise sintática e o coletor rejeita campos adicionais/credenciais. Não houve execução em Windows nativo neste host.

Os testes de rede, bitmap e provedor usam adaptadores/respostas simuladas indicados nos fontes. Os nomes citados pelo usuário aparecem em cenários simulados: não se afirma que os IDs de teste correspondam aos filmes reais, nem que a resposta atual do provedor desses filmes tenha sido consultada. Na lista M3U anteriormente enviada foi observada capa de Insaciável no preset TMDB w600_and_h900_bestv2; nenhuma URL de reprodução ou credencial foi exportada.

Tempo real, aparência e correspondência das obras precisam ser validados na TV. Os logs e APKs locais não são publicados. O coletor exporta apenas campos aprovados, hashes e tempos.
