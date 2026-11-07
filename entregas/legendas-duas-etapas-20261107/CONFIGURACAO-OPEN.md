# Ativação técnica do OpenSubtitles

A conta pessoal do usuário e o registro do aplicativo são coisas distintas. Quem usa a TV só cria uma conta no OpenSubtitles.com e entra com usuário/senha. O responsável pelo aplicativo registra o app na área API / API Consumers do OpenSubtitles.com e obtém a chave técnica e o User-Agent. Não envie essas credenciais na conversa e não use chaves de outros aplicativos.

Esta conversa ainda não forneceu a chave técnica; os testes usaram chaves e respostas fictícias. Não há login real ativo só com a instalação do pacote.

## APK

No computador de build, use o arquivo privado já previsto:

`app/src/desempenho/res/values/opensubtitles-private.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="opensubtitles_api_key" translatable="false">CHAVE_DO_APLICATIVO</string>
    <string name="opensubtitles_user_agent" translatable="false">IPTVPremium v1.13</string>
</resources>
```

Substitua a chave e use o User-Agent aceito para o registro. Não versione/compartilhe esse arquivo; mantenha no .gitignore `/app/src/*/res/values/opensubtitles-private.xml`. Para outro build type, use sua pasta correspondente. O instalador não sobrescreve esse arquivo.

## Cloudflare

Na pasta do site, configure os dois novos segredos com os mesmos valores do APK:

```powershell
npx wrangler secret put OPENSUBTITLES_API_KEY
npx wrangler secret put OPENSUBTITLES_USER_AGENT
```

Digite os valores nos prompts. Não coloque a chave em public/app.js, HTML, wrangler.jsonc ou no ZIP público.

Não gere nem troque APP_KEY, ADMIN_PASSWORD, IDs do D1 ou chaves Firebase por causa desta alteração. Nenhuma migração ou recriação de banco é necessária.

Após instalar o APK registrado e preparar o servidor atualizado, abra a etapa 2 na TV e escaneie o QR do portal. O site solicita uma atualização de status. O login fica habilitado quando o aparelho informa protocolo compatível e a impressão SHA-256 da chave coincide com a configuração do servidor. A impressão não é a chave; token e senha não entram no snapshot.

A autenticação é feita no servidor oficial do OpenSubtitles, e a TV recebe só a sessão verificada, criptografada durante a entrega. Não existe captura automática de cookies/login dos sites oficiais pelos QR de cadastro. O usuário precisa preencher sua conta ou colar sua chave no nosso portal.

## Conferência após habilitar

Testar SubDL por chave, OpenSubtitles por login válido/inválido, confirmação na TV, logout e troca de conta; legenda em PT-BR, inglês e espanhol; seleção que baixa um único arquivo, reutilização de cache, expiração e cotas do serviço. Os limites reais são os definidos pelo serviço, sem prometer downloads ilimitados. Nenhum arquivo é baixado automaticamente ao criar ou conectar a conta.
