# Experiência de sete dias e ativação — 1.05/code7

Aviso único no primeiro acesso com período de teste confirmado pela central: Ativar agora ou Decidir no vencimento. Mostra a validade real cadastrada, sem criar nova contagem local. A escolha persiste entre aberturas/atualizações e é registrada no aparelho na central, recuperada na reinstalação. Sem conexão, salva localmente e tenta confirmar depois com intervalos crescentes.

Ativar agora abre QRCode funcional, endereço da central, MAC e código com Copiar, Abrir site e Voltar. Abrir site tenta usar um link de acesso único; em falha, abre o endereço normal para entrada manual. Nunca exibe a credencial privada de instalação. QR mantém expiração de15min. NaTV, duas colunas; no celular estreito, conteúdo empilhado e rolável. Usa as imagens, logos, cores e contornos atuais de cada tema.

No término dos sete dias, o acesso vencido mostra convite à ativação; não oferece adiar um acesso já vencido. Ativados/bloqueados não recebem o aviso inicial. As regras de acesso e renovação existente continuam válidas. Se o usuário não responder e encerrar o app, o aviso inicial volta até registrar uma escolha.

É necessário publicar primeiro a atualização da central. O script combinado faz uma verificação dos fontes Android antes de publicar e, depois, aplica/compila a1.05/code7 localmente com a assinatura existente. Não instalaAPK, não publica lançamento ou muda as imagens do site. Instale por cima sem desinstalar/limpar dados.

Central: sem migração, novas tabelas ou histórico. Reutiliza um único registro em renewal_preferences para a confirmação do período. Compatível com apps antigos e preserva sincronização e atualização opcional. Usa os bindings, domínio, banco e segredos existentes.

Instalar-Ativacao-1.05.ps1: publica central + compila Android. Precisa de Node22+ e sessão Wrangler da sua conta no computador. -SomentePreparar extrai a central e prepara fontes Android sem publicar/compilar; -Verificar apenas confere fontes Android sem publicar/aplicar.

Somente nuvem: compilação, testes de política/persistência, Worker comSQLite e simulação de publicação. QR, navegação por controle, copiar e abrir navegador ainda precisam de teste real no aparelho. Não simula nem altera a licença de produção para testar.
