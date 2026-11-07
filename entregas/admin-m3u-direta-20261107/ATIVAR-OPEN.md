# Habilitar OpenSubtitles uma vez para o aplicativo

1. Entre na conta do OpenSubtitles.com (não OpenSubtitles.org). Na área API / API Consumers, registre seu aplicativo como IPTV Premium. Guarde a chave técnica gerada e o User-Agent aceito para ele (o projeto usa IPTVPremium v1.13). A criação da conta pessoal, por si só, não faz esse registro. Não use chave de outro aplicativo e não envie a chave nesta conversa.
2. No PowerShell da pasta extraída, execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\Configurar-OpenSubtitles.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Site "C:\Users\flavi\Downloads\site-identidade-aprovada"
```

O script pede a chave em entrada oculta e o User-Agent. Salva o recurso local app/src/desempenho/res/values/opensubtitles-private.xml, adiciona regras ao .gitignore, preserva eventual cópia anterior em backup ignorado e configura os segredos OPENSUBTITLES_API_KEY e OPENSUBTITLES_USER_AGENT na Cloudflare. Requer Node e login Wrangler na conta Cloudflare correta. Só execute depois de possuir a chave: não preencha com senha da conta ou valor fictício.

3. Compile e instale novamente o aplicativo após configurar a chave. Se estiver instalando esta atualização pela primeira vez, pode executar o configurador antes de Aplicar.ps1 -Testar, pois o instalador não toca o recurso privado. Se já aplicou, basta compilar pelo Android Studio/gradlew e instalar novamente.
4. Abra a etapa 2 de legendas na TV e acesse o QR do nosso portal. O login será habilitado quando o servidor e a TV reconhecerem o mesmo registro do aplicativo. Depois, cada pessoa entra com seu próprio usuário e senha. Limites são os definidos pelo serviço; o download ocorre somente ao escolher uma legenda no player.

Nenhuma chave ou senha real vem neste pacote. A configuração e o teste com a conta real ainda precisam ser concluídos pelo responsável.
