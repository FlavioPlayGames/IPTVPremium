# M3U direta para administrador

Atualização compatível com envio-lista-simples ou lista-editada-xtream, com versão Android 1.13 mantida. Sem publicação ou instalação automática.

## M3U direta

O administrador volta a ter a opção M3U direta — sem tentativa Xtream no formulário de adição e edição. O botão continua sendo somente Enviar lista, sem validação visível ou tela extra de confirmação.

O modo direto é salvo no aparelho com a lista, sobrevive à reinicialização, é devolvido nos dados do portal e reaparece ao editar. O aplicativo pula identificação, consulta de conta e importação Xtream para essa opção, mesmo se o get.php for compatível ou se antes a lista usava API. O catálogo anterior fica ativo enquanto a nova importação é preparada. As próximas atualizações continuam diretas até mudar a opção.

A opção é exibida somente no administrador, e o servidor também recusa que um usuário comum force esse modo pela API. Entradas normais mantêm identificação automática, reavaliação ao reenviar e tentativa M3U quando necessário. A correção de ordem dos filmes continua incluída. Listas antigas sem esse campo mantêm o comportamento automático; não são marcadas como diretas por suposição.

## Aplicar no Windows

Extraia o ZIP em C:\Users\flavi\Downloads\admin-m3u-direta, com scripts diretamente nessa pasta.

```powershell
Set-Location "C:\Users\flavi\Downloads\admin-m3u-direta"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Somente após BUILD SUCCESSFUL:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
$env:Path = "C:\Program Files\nodejs;" + [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
powershell -ExecutionPolicy Bypass -File .\Aplicar-Site.ps1 -Site "C:\Users\flavi\Downloads\site-identidade-aprovada" -Testar
```

Após o teste do site concluir:

```powershell
Set-Location "C:\Users\flavi\Downloads\site-identidade-aprovada"
npx.cmd wrangler deploy
```

Instale o novo APK antes de usar a M3U direta do site; versões antigas não reconhecem a escolha persistente. No administrador, abra a TV, adicione ou edite a lista, escolha M3U direta e envie. Não é necessário excluir/criar outra entrada.

Scripts validam os arquivos e criam backups separados; Desfazer.ps1 e Desfazer-Site.ps1 restauram com proteção de conflito. O patch não altera wrangler.jsonc, domínio, banco, agendamento, APP_KEY, senha administrativa ou Firebase. Recursos privados OpenSubtitles não são tocados.

## OpenSubtitles

A conta pessoal está criada, mas ainda falta registrar o IPTV Premium na área API / API Consumers do OpenSubtitles.com. Não há chave técnica real no projeto; por isso não é possível ativar o login somente com esta atualização. Leia ATIVAR-OPEN.md. O configurador está incluído e foi testado anteriormente com dados simulados. Os usuários finais precisam apenas de suas contas depois que o responsável concluir a configuração do aplicativo e do servidor. SubDL já configurado continua disponível.
