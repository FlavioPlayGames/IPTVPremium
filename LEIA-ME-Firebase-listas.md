# Sincronização automática de listas por Firebase

Implementação preparada para o aplicativo `br.com.flaviotorres.iptvpremium` e o painel Cloudflare já existente.

## O que muda

- O site continua salvando o comando na fila cifrada. Para inserir/editar/excluir listas, o Worker dispara um aviso FCM direcionado ao token daquele aparelho.
- O aviso contém somente `type=lists_changed` e `version=1`; não contém MAC, código, URL da lista, usuário, senha ou chave de legendas.
- O aplicativo usa WorkManager com rede disponível para consultar a fila autenticada em modo automático (somente listas e snapshots), aplicar alterações e confirmar o recebimento.
- Quando o aplicativo está aberto, o fluxo existente de carregamento importa o catálogo antes de liberar a navegação. Em segundo plano, a preparação do catálogo acontece quando a interface for retomada.
- Tokens são cifrados com a APP_KEY existente, um por aparelho, e apagados em cascata ao excluir o aparelho. Trocas de token substituem a mesma linha. Não existe nova cópia permanente das listas.
- As legendas continuam com sincronização manual. Verificação na abertura e botão manual permanecem disponíveis.
- Aparelhos sem serviços Google mantêm esses mecanismos de recuperação. O FCM não garante entrega instantânea, especialmente em segundo plano, com restrições de bateria, sem internet ou após encerramento à força.
- Mensagens silenciosas usam prioridade NORMAL. Não simulamos notificações visíveis nem usamos alta prioridade para contornar as regras do Android/FCM.

## 1. Criar o projeto gratuito

1. Acesse https://console.firebase.google.com/ e crie um projeto, por exemplo `Central IPTV Premium`.
2. Google Analytics é opcional e pode ficar desativado; não é necessário para esta integração.
3. Adicione um aplicativo Android com pacote exato `br.com.flaviotorres.iptvpremium`.
4. Baixe `google-services.json`. O SHA de assinatura não é necessário apenas para FCM.
5. Este projeto já inicializa o SDK e gera os recursos a partir desse arquivo. Não aplique outro plugin Gradle nem copie o código genérico do assistente Firebase por cima do projeto.

## 2. Preparar Android e site

Na raiz do projeto Android, baixe e execute `Implementar-Firebase-listas.ps1`. O parâmetro opcional `-SitePath` seleciona a pasta ORIGINAL do site publicado. Padrão: `Downloads\Central-IPTV-Premium-site\site`.

O auxiliar verifica os trechos antes de gravar, cria backups, adiciona a migração 0007 (tokens), reutiliza o banco e publica pelo script existente. Não cria outro banco, não troca APP_KEY/senha administrativa e não configura plano pago.

Se aparecer `Versao diferente`, pare e envie apenas os arquivos de código indicados para adaptar a atualização. Não envie local.properties, senhas ou chaves privadas.

## 3. Credencial do servidor

O Worker usa a API HTTP v1 do FCM com conta de serviço e OAuth de curta duração. Não usa chave legada do FCM.

No projeto Google Cloud correspondente ao Firebase:

1. Confirme que a API **Firebase Cloud Messaging API** está habilitada.
2. Em IAM e administrador → Contas de serviço, crie uma conta dedicada para o envio.
3. Conceda a função **Firebase Cloud Messaging API Admin** (`roles/firebasecloudmessaging.admin`), que permite `cloudmessaging.messages.create`. Não é necessária a função de proprietário.
4. Gere uma chave JSON dessa conta de serviço e guarde fora das pastas `app` e `public`, em local privado no computador. Não coloque em Releases, GitHub, no APK ou neste chat.

A chave privada será salva como segredo `FCM_SERVICE_ACCOUNT` da Cloudflare, nunca como variável pública ou arquivo servido pelo site. Consulte também as políticas de chaves da sua conta Google; organizações podem bloquear a criação de chaves.

Execute `Configurar-Firebase.ps1` na raiz Android, informando os caminhos de `google-services.json` e da chave JSON privada de servidor. O auxiliar confere o pacote e se ambos pertencem ao mesmo projeto, envia o segredo ao Worker e copia apenas a configuração cliente para `app\google-services.json`.

Exemplo (ajuste os caminhos, sem colar o conteúdo dos arquivos):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Configurar-Firebase.ps1 -GoogleServicesPath "$env:USERPROFILE\Downloads\google-services.json" -ServiceAccountPath "$env:USERPROFILE\Downloads\firebase-service-account.json"
```

Mantenha backups seguros da APP_KEY e da chave da conta de serviço. Elas têm finalidades diferentes; uma não substitui a outra.

## 4. Compilar e testar

Compile no seu computador com a assinatura original. Para distribuição pela atualização do site, use número interno MAIOR que o instalado; mudar o código não altera automaticamente versionName/versionCode.

Instale o novo APK sobre o atual, sem desinstalar. Abra o app com internet e serviços Google funcionando; ele obtém o token e o registra na central automaticamente. O primeiro registro e tentativas após falha podem levar alguns segundos.

Com a TV aberta, envie uma lista pelo site. O aplicativo deve iniciar a sincronização e mostrar o carregamento do catálogo. O status do site deve mudar para `Recebido pelo aplicativo` somente após o ACK autenticado. Aceitação da mensagem pelo Firebase não significa que o catálogo já foi baixado ou que o conteúdo de vídeo funciona.

Teste também com Wi-Fi temporariamente desconectado: a lista deve permanecer pendente e ser recuperada ao abrir novamente. FCM não abre Activities nem instala APKs silenciosamente.

## Verificações realizadas no desenvolvimento

- Android: assembleDebug sem Firebase configurado e com JSON cliente de teste; compilações bem-sucedidas. A configuração de teste não é distribuída.
- API: 47 testes passando, com SQLite local, WebCrypto real para JWT RSA/OAuth e transporte Google simulado. Cobrem isolamento, cifra, rotação de token, cascata, filtragem de legendas, ACK, waitUntil, falhas externas e UNREGISTERED.
- Atualizador: payload aplicado em memória e comparado integralmente aos arquivos compilados.
- Pendente: credenciais reais do proprietário, publicação na sua Cloudflare, instalação do APK original e teste físico na TV. Os scripts PowerShell não foram executados em Windows neste ambiente.

Referências: https://firebase.google.com/docs/cloud-messaging/android/client e https://firebase.google.com/docs/cloud-messaging/auth-server
