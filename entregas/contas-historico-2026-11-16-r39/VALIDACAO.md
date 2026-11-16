# Validação r39 — 16/11/2026

## Aplicativo

Compilação final :app:assembleDesempenho aprovada em 5m30s: Kotlin, lint vital, R8 e APKs. Hashes dos oito fontes novos/alterados conferidos com o manifesto após compilação. Dezessete guardas de dependências, recursos originais preservados byte a byte. Sem mudanças de Gradle, assinatura, orçamento de memória/cache ou layout.

15 verificações de restauração com classes reais compiladas e DataStore real gravando arquivo de preferências temporário. Context/SharedPreferences hospedados em memória para as dependências Android. Cadastros A/B/C retornam; ativa B mantida; somente uma solicitação de importação para B; demais não agendadas. Progresso de filme e episódio gerado pelo WatchHistoryStore real, restaurado e decodificado pelo próprio histórico; perfil Kids separado, coordenadas de episódio, favoritos tipados, exclusão de dados manuais e round trip GZIP. Preferência não autorizada rejeitada antes da escrita; cadastro vazio continua vazio.

7 verificações do cliente OpenSubtitles com os fontes reais compilados para JVM: conta mantida após expiração; buscas simultâneas renovam uma vez; token novo salvo; logout limpa conta e sinaliza revogação; renovação tardia não reconecta após saída; falha de rede preserva conta; entrega antiga da central após logout recusada por geração, sem depender do relógio da TV.

24 verificações da r38 repetidas nas classes finais: duração inconsistente não conclui filme/episódio, conclusão real em 95% e fim mantidos, reabertura/progresso, capa sem interferência, mapeamento de série não apagado por carregamento vazio e próximo episódio pendente preservado.

## Site

23 verificações adicionais com Worker real, SQLite em memória e provedor simulado: autenticação/isolamento entre TVs, gravação/restauração exata, backup vazio deliberado, revisão CAS recusa sobrescrita concorrente, criptografia em repouso, limite/corrupção comprimida recusados, login persistente/renovação, ausência de credenciais na resposta, revogação e corridas logout/login/renovação. Credenciais apagadas e marcador de versão preservado para impedir recriação por requisição antiga.

Suíte anterior npm test aprovada: 24 testes de conta/provedor, fluxos manuais e armazenamento D1/ZIP, sem regressão. npm run check aprovado, incluindo empacotamento Wrangler deploy --dry-run, sem deploy real. O módulo novo usa apenas D1 e criptografia existente; tabelas permanentes sem expiração automática. Nenhuma chave técnica real nos testes.

## Aplicadores

Aplicação, repetição, desfazer byte a byte e bloqueio de base divergente/edição posterior aprovados para APK e site. Aplicador APK verificado em PowerShell 7 e na forma de arrays JSON do Windows PowerShell 5.1. Parsers de todos os scripts aprovados. Caminho oficial do site mantido em C:\Users\flavi\AndroidStudioProjects\Site IPTV; publicação reutiliza Publicar-Site.ps1 existente, mantendo verificações e pacote de fontes atualizado.

## Limites da evidência

Sem TV/ADB e sem limpeza real de dados neste ambiente. Sem deploy Cloudflare ou autenticação real em conta pessoal do provedor. É necessário publicar o site, instalar o APK, refazer login OpenSubtitles uma vez e confirmar Sincronizar agora antes do teste físico. Mudanças não enviadas à central não são recuperáveis após limpeza. Troca de usuário/assinatura ou reset de fábrica pode exigir recuperação administrativa. A central mantém credenciais OpenSubtitles criptografadas para renovar sessão; a senha não é salva no APK/fila. Arquivos de legendas baixadas ou manuais não fazem parte desta persistência.

Uma execução intermediária do lint apresentou falha interna de análise em arquivo não alterado; nova compilação completa final com fontes congelados passou sem desativar lint.
