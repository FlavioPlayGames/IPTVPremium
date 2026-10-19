# M3U direta no administrador — 1.07

Opção adicionada ao campo Formato de entrada, no cadastro/edição de listas: **M3U direta — sem conversão**. Somente aparece para a sessão de administrador. A API também verifica o papel da sessão; enviar uma opção/flag forjada como cliente é recusado com HTTP 403.

A preparação direta valida nome e URL pública HTTP/HTTPS, preserva parâmetros/caminho/porta e envia um cadastro normal `type=m3u`, sem configuração Xtream nem link alternativo. Não consulta o provedor no site e não afirma acesso verificado: estado `device_required`. O aplicativo verifica/importa o conteúdo no emulador. Essa preparação não garante que um link inválido funcionará; permite testar o caminho M3U real, inclusive quando o provedor bloqueia consultas do site.

A opção automática M3U continua identificando Xtream e enviando a M3U como alternativa. Xtream direto também mantém a alternativa anterior. Alterar o formato invalida a preparação anterior. Editar mantém ID e estado habilitado da lista. O formato M3U direto não é um novo tipo persistido: chega ao aplicativo como M3U normal e permanece assim até uma nova edição/envio. Ao editar depois, selecione novamente M3U direta para evitar a conversão automática.

Não requer nova compilação/APK: o aplicativo já instalado possui importação M3U nativa. Esta entrega atualiza o site e mantém a funcionalidade Xtream + alternativa M3U da entrega anterior.

## Publicar

Usa o login Wrangler local e Node.js 22+. Não faz migração de banco. `-NaoPublicar` apenas extrai para revisão. O site não foi publicado ao vivo nesta nuvem.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/m3u-direta-admin-1.07-20261019'
$arquivo = Join-Path $env:TEMP 'Publicar-M3U-Direta-Admin-1.07.ps1'
Invoke-WebRequest -UseBasicParsing -Uri "$base/Publicar-M3U-Direta-Admin-1.07.ps1" -OutFile $arquivo
if ((Get-FileHash $arquivo -Algorithm SHA256).Hash -ne '7bbf1572ba25d0e06476f1024b62bffa383c7e876bb01b6a1c5b74a05a083444') { throw 'Download diferente do esperado.' }
& $arquivo
```

Depois da publicação: recarregue o site com Ctrl+F5; entre como administrador; abra Gerenciar no aparelho do emulador; edite a lista desejada; escolha **M3U direta — sem conversão**; confira o link; clique Preparar lista e Enviar lista. O resultado da preparação deve mostrar **Lista M3U**. O cliente comum não recebe essa terceira opção. Não é necessário criar outra lista: editar preserva o ID.

## Medir no emulador

Inicie o medidor antes de Enviar lista:

```powershell
& $medidor -Dispositivo 'emulator-5554' -Formato M3U -PastaSaida 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
```

O medidor é o `Medir-Importacao-1.07.ps1` já entregue e usado no diagnóstico Xtream. Depois de iniciar a captura, envie a lista pelo site em M3U direta. Compartilhe `diagnostico-importacao-m3u.txt`; o evento `SYNC_RESUMO tipo=M3U` confirmará o protocolo realmente usado. Todos os testes solicitados ao usuário permanecem no emulador, sem conectar TV.

## Validação

58 testes de servidor passaram, incluindo permissão do administrador no Worker, recusa de tentativa de cliente, preservação do link sem consulta ao provedor, restrições de URL, regressão da conversão automática, fila criptografada e demais fluxos anteriores. 14 verificações no navegador passaram: acesso real pelo fluxo de sessão administrativa e seleção do aparelho no servidor simulado, envio direto de um get.php reconhecível como `type=m3u`, ausência da opção na sessão cliente e os fluxos anteriores. Wrangler dry-run e extração/verificação do publicador passaram. Não foram usadas credenciais reais nem chamadas ao provedor. Não há alegação de ganho de tempo: a opção permite medir o importador M3U separadamente.
