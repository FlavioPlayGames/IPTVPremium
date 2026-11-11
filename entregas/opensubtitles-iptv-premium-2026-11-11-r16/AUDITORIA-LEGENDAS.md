# Auditoria do caminho site → TV → player

Fontes examinados em 11/11/2026: app atual após r15, Worker/módulos enviados em `worker-atualizacao-abi.zip`, Worker compilado enviado como texto e arquivos enviados em `site-public-atual.zip`. O ZIP completo do site excedia o limite de transferência e não foi usado. As partes menores foram lidas.

## Estado por etapa

| Etapa | Implementado nos fontes atuais | Validação nesta etapa | Falta para concluir |
| --- | --- | --- | --- |
| Registro técnico OpenSubtitles | Leitura da chave/User-Agent no APK, fingerprint e verificação no Worker | Rejeição de registro ausente/incompatível em simulação | Criar API Consumer e configurar valores reais nos dois lados |
| Formulários do celular | SubDL por chave pessoal; OpenSubtitles por usuário/senha; TV de destino e estados de configuração | Chromium em celular e desktop, com API simulada | Confirmar login real com o serviço |
| Entrega à TV | Comando `subtitle_session`, token verificado, fila criptografada, prazo, pull autenticado e ACK/snapshot | Worker atual com SQLite em memória e cliente Kotlin aceitando sua entrega fictícia | Pull/ACK no aparelho e ambiente Cloudflare reais |
| Atualização do player | Revisão observável dos provedores e pesquisa sob demanda | Ramo real de recebimento extraído/testado; correção r16 emite revisão após salvar | Confirmar atualização visual na TV |
| Pesquisa | Filtros de obra/ano/ID e temporada/episódio; OpenSubtitles PT-BR/en/es | Protocolo/cliente com respostas simuladas e rejeição de correspondências erradas | Conferir payloads/resultados reais de filmes e episódios |
| Download/cache | POST somente após escolha/cache ausente, escopo por conta, cache de arquivos limitado e seleção de cues | Cliente verifica uma chamada explícita e tratamento simulado de autenticação/limites | Arquivo real, cache, sincronismo, troca/desativação no ExoPlayer/VLC |
| Persistência | Sessão cifrada via Android Keystore, geração de conta, expiração | Inspeção do armazenamento; testes usam contas simuladas | Fechar/reabrir TV, expiração e troca de conta no hardware |

## Diferenças do site em relação à referência anterior

O Worker/módulos atuais e as páginas recebidas não são idênticos à referência de 07/11. O site atual inclui diagnóstico de disponibilidade OpenSubtitles (`server_not_configured`, `waiting_tv`, `app_update_required`, `app_not_configured`, `registration_mismatch`) e editores que abrem após escolher o provedor. Os testes foram adaptados a esses cliques e ao rótulo atual “Serviços”; não foi revertida a interface para o modelo antigo.

O `SHA256SUMS.txt` que veio com o Worker não corresponde aos bytes de três arquivos públicos recebidos (`app.js`, `subtitles.js`, `index.html`), inclusive após normalizar quebras de linha. Isso indica que o manifesto não acompanha essas revisões; não prova falha nem qual versão está publicada. A análise/testes usaram os conteúdos efetivamente enviados, não o manifesto antigo.

O Worker-fonte e o bundle colado passaram na suíte inicial de servidor. Não foi acessado o site publicado nem alterado seu código/segredos/banco a partir deste ambiente. Portanto, a igualdade entre fontes enviados e produção não é afirmada.

## Correção concreta encontrada no app

No recebimento de `subtitle_session`, o app validava e salvava a sessão OpenSubtitles, mas não chamava `SubtitleProviderUpdates.changed()`. O player observa essa revisão para invalidar uma pesquisa já realizada. A r16 acrescenta o aviso depois da gravação bem-sucedida. Sessões rejeitadas não emitem o aviso.

A orientação quando nenhum provedor está conectado também deixa de mencionar apenas o SubDL. Não foram alterados os clientes de API, regras de download, cache, tradução, renderização ou mecanismos de reprodução.

## Proteção de dados confirmada nos testes

Credenciais e registros usados na validação são fictícios. O navegador recebe recibo/estado, não o token OpenSubtitles. O Worker não grava a senha na fila/SQL; a fila recebe a sessão cifrada e vinculada à TV. Outra TV não recebe esse comando. A TV valida registro do app, host oficial, prazo e escopo antes de aceitar. Após ACK, o ciphertext de sessão é removido.

SubDL é recebido como chave pessoal cifrada e vinculada à TV. Esse fluxo comprova transporte/configuração na simulação, não que uma chave pessoal real seja aceita pelo serviço. Não houve autenticação/download real nem consumo de cota de usuários.

## Bloqueio confirmado

O usuário informou que ainda não registrou/configurou o API Consumer. O recurso técnico do app disponível para revisão também está vazio; uma configuração privada local não foi solicitada nem recebida. O login pessoal não substitui o registro técnico.

Os fontes do site já contêm os endpoints e formulários necessários. A próxima ação é configurar o registro e seguir a matriz de teste real do `LEIA-ME.md`. Só depois se pode declarar a integração de ponta a ponta concluída.
