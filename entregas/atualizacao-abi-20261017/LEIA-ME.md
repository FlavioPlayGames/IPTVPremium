# Atualização por arquitetura — preparação

O aplicativo novo escolhe pela lista SUPPORTED_ABIS do Android, na ordem do sistema.
Não presume que uma TV com processador de 64 bits tenha Android de 64 bits.
A versão universal continua obrigatória e fica nos campos principais, compatíveis com aplicativos antigos.
As quatro variantes opcionais compartilham o versionCode e versionName da publicação.
O aplicativo verifica tamanho, SHA-256, bibliotecas nativas, pacote, número de versão e assinatura antes de abrir o instalador Android.

Painel: mantém a janela escura/dourada e acrescenta um seletor de APK. Cada opção preserva seu link, tamanho e hash.
Selecionar um arquivo calcula tamanho e SHA-256 no navegador; não envia o APK ao Cloudflare.
Os APKs devem continuar em hospedagem externa com links HTTPS diretos.
D1: apenas JSON de metadados, com limite de 16 KiB por publicação. Um único registro app_update é substituído; não cria histórico nem armazena APKs.
A publicação usa a mesma autenticação administrativa, origem e token CSRF; não há alteração de esquema do banco.

Android: executar na raiz Android. O script cria backup, aplica os dois arquivos e compila com a assinatura local.
Site: executar na raiz do site com public/app.js. O script cria backup e prepara os seis arquivos, incluindo worker-atualizacao-abi.js.
A preparação não modifica wrangler.toml/json/jsonc, banco, credenciais, fontes antigas do Worker nem publica o site.
Para orientar a implantação preservando ASSETS, DB, domínios e agendamento, conferir a configuração Wrangler atual antes de publicar.
As alterações do site ainda não foram implantadas em produção; os testes usam Worker local, banco simulado e navegador real.
Não publicar APK de teste gerado na nuvem: gere e assine no seu computador, mantendo a chave atual e aumentando o versionCode.
