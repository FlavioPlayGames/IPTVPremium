# Envio simples de listas e aviso OpenSubtitles

Atualização incremental do aplicativo 1.13 e do site identidade aprovada. Nada foi publicado automaticamente.

## Listas

O formulário tem somente Cancelar e Enviar lista. Não há botão de verificar, confirmação extra ou formato identificado para escolher. A preparação ocorre após o clique, só conferindo os dados básicos. O servidor não consulta o provedor nem testa credenciais: entrega o link ao aparelho. Para entradas manuais Xtream, gera get.php com as credenciais corretamente codificadas. Links M3U genéricos, filtrados ou assinados são preservados; não são convertidos indevidamente.

No novo cadastro compatível, o aplicativo tenta Xtream primeiro. Se a consulta/API ou a importação falhar por problema do provedor, tenta a M3U original e memoriza o transporte que concluiu a importação. Cancelamentos e falhas locais de gravação não acionam outra importação. Catálogos já existentes mantêm o transporte e os IDs para preservar o conteúdo e as associações; uma atualização malsucedida não troca silenciosamente um catálogo existente. Listas Xtream antigas explícitas continuam no fluxo anterior. O fallback desta entrega vale para o cadastro automático enviado pelo novo site.

## Aplicar no Windows

Extraia o ZIP em C:\Users\flavi\Downloads\envio-lista-simples. Os scripts ficam diretamente nessa pasta.

```powershell
Set-Location "C:\Users\flavi\Downloads\envio-lista-simples"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
powershell -ExecutionPolicy Bypass -File .\Aplicar-Site.ps1 -Site "C:\Users\flavi\Downloads\site-identidade-aprovada" -Testar
```

Execute a instalação da TV somente após BUILD SUCCESSFUL. Os instaladores validam os arquivos antes de escrever, salvam backup e recusam versões desconhecidas. Desfazer.ps1 e Desfazer-Site.ps1 restauram com proteção contra conflitos. O patch do site não altera wrangler.jsonc, vínculos, domínio, agendamento, segredos nem banco. Depois do teste do site, para publicar:

```powershell
Set-Location "C:\Users\flavi\Downloads\site-identidade-aprovada"
npx.cmd wrangler deploy
```

## Por que o login OpenSubtitles está bloqueado

Criar a conta pessoal não habilita a integração do aplicativo. O responsável deve registrar o IPTV Premium na área API / API Consumers do OpenSubtitles.com e obter a chave técnica e o User-Agent. Os usuários não precisam registrar aplicativos: entram apenas com suas contas quando o serviço estiver habilitado. O novo aviso distingue configuração do servidor, aplicativo desatualizado, registro ausente na TV e configuração divergente. Não existe chave real no pacote.

Depois de obter a chave, sem enviá-la na conversa, execute:

```powershell
Set-Location "C:\Users\flavi\Downloads\envio-lista-simples"
powershell -ExecutionPolicy Bypass -File .\Configurar-OpenSubtitles.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Site "C:\Users\flavi\Downloads\site-identidade-aprovada"
```

O configurador pede a chave em entrada oculta, salva o recurso privado ignorado pelo Git e configura somente OPENSUBTITLES_API_KEY e OPENSUBTITLES_USER_AGENT na Cloudflare. Faz backup local se o recurso já existir. Não modifica APP_KEY, ADMIN_PASSWORD ou Firebase. É necessário compilar e instalar novamente o APK após configurar a chave; abra a etapa 2 de legendas na TV para atualizar os dados e liberar o botão no site.

A habilitação real ainda depende desse registro; as verificações usaram apenas credenciais e serviços simulados. O pacote não faz login em contas reais nem baixa legendas. O download continua somente quando a pessoa escolhe uma legenda no player.
