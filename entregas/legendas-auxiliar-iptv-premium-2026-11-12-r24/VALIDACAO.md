# Validação r24

- 37 verificações passaram para sincronização, agrupamento, persistência e cálculo do auxiliar: Brasil/Portugal separados, faixa interna genérica preservada, clique em 6 s com cue inicial em 0 s resulta em +5 s, cue inicial em 3 s resulta em +2 s, valores negativos e limites sem overflow.
- 40 verificações SubSource passaram com API simulada: quatro idiomas, paginação limitada, nomes de episódio e pacotes, seleção no ZIP, isolamento de conta/temporada/episódio e região portuguesa identificada.
- 105 verificações OpenSubtitles passaram com contrato simulado e os fontes reais: quatro idiomas com pt-BR e pt-PT separados, português sem região excluído dessa API, identidade de obra, episódio e redirecionamentos/autenticação preservados.
- 44 verificações do servidor existente passaram com SQLite/provedor simulados; não há alteração no site ou escrita em produção.
- Aplicação, reaplicação, desfazer, guardas de hash e proteção de edições posteriores passaram sobre as bases r23 e r22/r20. PowerShell 7 real e simulação do formato JSON do Windows PowerShell 5.1; não executado em Windows real.
- Todos os scripts passaram no parser. Coletor aceita os novos campos de Portugal e o evento ASSIST e rejeita campos privados extras.

A conta real e os players reais na TV não foram usados neste ambiente. Os contratos simulados não comprovam a disponibilidade de um arquivo específico. A navegação/foco do auxiliar, seek/retorno, segundo plano e proteção do histórico exigem a conferência na TV descrita no LEIA-ME. Não há análise de áudio ou garantia de sincronismo automático.

## Build final

compileDesempenhoKotlin e lintDesempenho passaram nos fontes finais: BUILD SUCCESSFUL em 2m03s. Relatório: zero erros, 158 avisos e 6 sugestões. Esses avisos não foram objeto desta entrega. O pacote não contém APK assinado; a compilação ocorre no projeto local.
