# Legendas: cadastro e conexão em duas etapas

Versão 1.13 mantida. Incremental sobre opensubtitles-integracao dd7d5e4. Inclui aplicativo e alteração coordenada do portal, com backups separados. Nada é publicado automaticamente.

## TV

Passo 1: “Crie sua conta”, com QR estático e botão para cada site oficial (SubDL e OpenSubtitles). Texto aprovado: “Se o seu provedor não oferecer legendas, use estas opções gratuitas. Recomendamos cadastrar-se nas duas, pois cada serviço tem limites de uso.”

Passo 2: “Conecte sua conta à TV”, com o QR do nosso portal, exclusivo desta TV, e instruções para colar a chave SubDL ou entrar no OpenSubtitles pelo celular. Gerar novo QR, status dos serviços e Concluir. Não há passo 3. Configuração pelo controle remoto fica em “Prefere configurar pela TV?”.

Cores e foco usam os componentes do tema atual (SettingsLook/CatalogStyle/MarbleSettingsButton), sem fixar azul ou mudar a seleção de outros menus. Voltar sai da edição local para o passo 2, do passo 2 para o passo 1 e daí para a tela anterior. O botão Concluir pode ser usado com só um provedor ou sem conectar.

Enquanto o passo 2 está visível e a Activity ativa, consulta a central a cada 8 segundos por até 15 minutos. A consulta é suspensa no segundo plano e encerrada ao sair. Pode-se gerar novo QR para reiniciar. Canais, player, qualidade de vídeo e histórico não foram alterados.

## Portal

“Conectar serviços” separado de “Aparência das legendas”. SubDL com chave pessoal e breve ajuda; OpenSubtitles com usuário/senha, sem chave técnica para o usuário. Links de cadastro oficiais permanecem no site. Identifica a TV de destino, mostra envio pendente e só confirma conexão após recebimento no aparelho. Layout adaptado para celular e computador, preservando a identidade visual do site.

O servidor autentica a conta no endpoint oficial HTTPS do OpenSubtitles. A senha fica apenas na memória durante o pedido: não é gravada, não vai para a fila e não é entregue à TV. Só a sessão validada é enfileirada, criptografada com a chave já existente da central e vinculada ao aparelho. O ciphertext é apagado após confirmação ou expiração (até 15 minutos para sessões). A TV valida registro do aplicativo, host oficial, prazo e identidade da conta antes de salvar a sessão criptografada com Android Keystore. Listas, licenças, banco, credenciais da central e configurações de publicação não são modificados.

Login inválido do serviço não encerra a sessão do nosso portal. Resultados de outro aparelho e chaves de aplicativo incompatíveis são recusados. Configurações antigas do SubDL e a aparência das legendas continuam disponíveis. Não há download de legenda no cadastro/login/sincronização: a regra continua sendo baixar somente após a escolha no player, com cache antes do pedido ao serviço.

## Aplicar no aplicativo

Na pasta extraída, execute no PowerShell:

```powershell
.Aplicar.ps1 -Projeto 'C:UserslaviAndroidStudioProjectsIPTVPremium' -Testar
```

Depois de BUILD SUCCESSFUL:

```powershell
.Instalar-TV.ps1 -Projeto 'C:UserslaviAndroidStudioProjectsIPTVPremium'
```

Desfazer.ps1 restaura o aplicativo com verificação de conflito. Recursos privados de configuração do OpenSubtitles não são tocados.

## Atualizar o projeto do site

O ZIP enviado nesta conversa contém só wrangler.jsonc, package.json e instruções. Não contém as páginas nem o Worker. Não foi possível consultar o site público neste ambiente; portanto, não afirmamos que o código em produção é igual à base local.

A atualização é por trechos protegidos: não substitui integralmente o Worker/app.js, preserva alterações fora desses trechos e recusa trechos desconhecidos antes de escrever. Não altera wrangler.jsonc, IDs do D1, .dev.vars, segredos, dependências, migrations ou dados do banco.

Informe a pasta completa do site que contém worker-atualizacao-abi.js, public/app.js e wrangler.jsonc, e não apenas a pasta do ZIP de configuração:

```powershell
.Aplicar-Site.ps1 -Site 'C:CAMINHODOPROJETO-DO-SITE' -Testar
```

O teste executa verificações JS e `wrangler deploy --dry-run`: não publica. Se houver recusa de versão, envie os fontes atuais do Worker e da pasta public para compatibilização; não force a substituição. Desfazer-Site.ps1 restaura os originais e preserva configurações, recusando edições posteriores.

A pasta site/referencia contém o código preparado para revisão, sem wrangler nem segredos. Não publique essa referência por cima de uma versão desconhecida. Reutilize o projeto atual, aplique o patch e confira o resultado antes da publicação pelo processo habitual.

## Habilitar OpenSubtitles

A chave técnica do aplicativo ainda não foi fornecida nesta conversa. Ela é necessária no APK e no servidor; usuários entram apenas com suas contas. Leia CONFIGURACAO-OPEN.md. Sem essa configuração, o SubDL funciona e o login OpenSubtitles fica indisponível, sem pedir senhas para uma integração desabilitada.

## Validação

Compilação Kotlin/Lint Android; 56 testes do cliente/protocolo reais com serviço simulado; 75 regressões do player; 24 verificações do Worker com SQLite/criptografia e API simulada; 24 verificações de navegador Chromium em celular 390px e computador 1440px; verificação de bundle Cloudflare em dry-run; instaladores app/site, reaplicação, backup byte a byte, corrupção, versão desconhecida, alteração de terceiros preservada e conflito ao desfazer. Prévias em previas/site-mobile.png e previas/site-desktop.png usam conta e dados fictícios.

Não houve publicação, acesso a contas reais OpenSubtitles ou teste em TV/emulador. Compatibilidade real da conta/API depende da chave registrada e do teste final no aparelho. A falha HEVC do VLC diagnosticada anteriormente não é corrigida por esta mudança de configuração.
