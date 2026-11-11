# IPTV Premium r16 — revisão e preparação OpenSubtitles

Revisão dos fontes atuais do app e dos dois pacotes recebidos do site. A chave técnica ainda não foi configurada pelo responsável. **A integração real não está declarada concluída:** falta configurar o registro do aplicativo e validar com conta/serviço/TV reais.

## O que já existe

O site atual tem formulário de chave pessoal SubDL, login OpenSubtitles, associação à TV autenticada, entrega criptografada de sessão e confirmação por ACK/snapshot. O app tem pesquisa, cache, download após escolha e apresentação de legendas externas por uma camada comum ao ExoPlayer e ao VLC. O portal não recebe a chave técnica pelo formulário do usuário; ela pertence à configuração do aplicativo e do servidor.

Os testes com API simulada passaram nos fontes atuais. Ver `AUDITORIA-LEGENDAS.md` e `VALIDACAO.md`. Nenhuma publicação ou alteração de segredos/D1 foi feita neste ambiente.

## Correções da r16

- Após aceitar e salvar uma sessão OpenSubtitles recebida pelo portal, o app publica a atualização dos provedores. Antes, esse ramo não avisava o player, que podia conservar uma pesquisa antiga até reconstruir a tela.
- A orientação do player quando nenhum provedor está conectado passa a indicar Configurações > Legendas, em vez de mencionar somente o SubDL.

São dois arquivos Kotlin. Importação r15 com lote de 2.000, capas, histórico, versão, assinatura e orçamento de armazenamento permanecem como antes. O cache de legendas mantém os limites existentes. O pacote não inclui APK nem credenciais.

## 1. Registrar o aplicativo

Na sua conta em **OpenSubtitles.com**, localize **API / API Consumers** e registre seu aplicativo. Guarde a chave técnica do Consumer e o User-Agent aceito para esse registro.

Essa chave é diferente da chave pessoal SubDL, do login pessoal OpenSubtitles e do `APP_KEY` da central. Não envie chave, senha ou token na conversa.

## 2. Configurar a chave no projeto Android, localmente

Extraia o pacote em `Downloads\opensubtitles-iptv-premium-r16`. No PowerShell do Android Studio, use o User-Agent do registro:

```powershell
$pasta = "$env:USERPROFILE\Downloads\opensubtitles-iptv-premium-r16"
powershell -ExecutionPolicy Bypass -File "$pasta\Configurar-OpenSubtitles-App.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium" -UserAgent "IPTVPremium v1.13"
```

A chave é solicitada com entrada oculta. O script cria `app/src/desempenho/res/values/opensubtitles-private.xml`, escapa o XML e adiciona sua exclusão ao `.gitignore`. Não imprime a chave, não valida a API, não configura o servidor e não substitui uma configuração privada diferente. Se já houver esse arquivo com outros valores, ele é preservado para conferência/edição local.

O valor `IPTVPremium v1.13` é o padrão do projeto; se seu registro aprovar outro User-Agent, informe esse valor no parâmetro e use o mesmo no servidor. O arquivo privado não deve acompanhar ZIPs públicos nem ser enviado para revisão.

## 3. Configurar o mesmo registro no Cloudflare

Na pasta atual do site:

```powershell
Push-Location "C:\Users\flavi\Downloads\site-identidade-aprovada"
npx wrangler secret put OPENSUBTITLES_API_KEY
npx wrangler secret put OPENSUBTITLES_USER_AGENT
Pop-Location
```

Digite os valores nos prompts oficiais do Wrangler. Não coloque a chave como argumento do comando, em HTML/JavaScript público ou em `wrangler.jsonc`. Esses comandos atualizam a configuração do Worker publicado; execute no projeto/conta Cloudflare corretos, já usados pelo seu site.

Mantenha `APP_KEY`, `ADMIN_PASSWORD`, IDs D1, Firebase e demais configurações existentes. Os fontes de site recebidos já têm o protocolo necessário; esta entrega não os substitui nem publica código do site. Conta pessoal sozinha não habilita a integração sem esse registro técnico nos dois lados.

## 4. Aplicar, compilar e instalar na TV

Depois da configuração privada:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\opensubtitles-iptv-premium-r16\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

A aplicação valida hashes, guarda os dois fontes anteriores fora do projeto em `Downloads\IPTV-restauracao-r16-...` e instala com `adb install -r`. Não modifica o arquivo privado nem desinstala/limpa dados da TV.

## 5. Conectar a conta pelo celular

Na TV, abra Configurações > Legendas. Conclua o cadastro pessoal nos sites oficiais se necessário. Na etapa de conexão, gere o QR do nosso portal para esta TV e escaneie pelo celular.

Confira a TV identificada no portal. Para SubDL, informe sua chave pessoal. Para OpenSubtitles, informe usuário/senha da conta pessoal. Aguarde a confirmação **na TV**; envio pendente não é conexão concluída.

Se o portal mostrar preparação, registro incompatível ou aplicativo não configurado, confira os dois valores técnicos, a compilação/instalação correta e gere/atualize o QR/status na TV. O portal e o app precisam usar a mesma chave técnica. A configuração de uma chave sintaticamente válida não comprova que o serviço a aceitou.

## 6. Validação real restante

- Filme e episódio conhecidos: conferir obra, temporada/episódio e opções PT-BR, inglês e espanhol do OpenSubtitles.
- Abrir o menu Legendas pesquisa; escolher uma opção baixa um arquivo. Confirmar texto, sincronismo e ausência de cobrança/download repetido quando o cache é reutilizado.
- Ativar, trocar por outra externa, trocar por legenda interna e desligar nos dois mecanismos ExoPlayer e VLC.
- Fechar/reabrir o app antes da expiração da sessão, confirmar conta persistida e reutilização do arquivo. Sessão expirada deve pedir nova conexão, sem senha persistida.
- Senha inválida não encerra o login do nosso portal; limites do serviço e falhas de rede precisam ser apresentados sem repetição automática de download.
- Conferir duas TVs, remoção/troca de conta e isolamento dos estados/arquivos. Não é necessário provocar muitos downloads para testar cotas; os limites reais são os do serviço.

O caminho visual e a persistência no Android Keystore ainda exigem teste no aparelho real. Testes JVM/navegador com respostas simuladas não comprovam essa parte.

## Restaurar os fontes

Use `Desfazer.ps1 -Backup "PASTA_DA_RESTAURACAO" -Instalar`, com o caminho impresso ao aplicar. Restaura somente os dois fontes desta etapa; não apaga a chave privada, a conta salva, os dados da TV ou a configuração do servidor.
