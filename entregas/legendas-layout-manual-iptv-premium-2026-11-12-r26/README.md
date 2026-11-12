# Legendas — layout e envio manual r26

Base: projeto Android, site e Worker enviados em 12/11/2026. Esta entrega não altera a versão do aplicativo.

## Android

A entrada de Legendas passa a ter Contas de legendas, Envio manual e Aparência. As cores vêm do tema atual do aplicativo, incluindo claro e escuro. A frase sobre boa história não foi incluída. O QR aparece somente depois da ação correspondente.

No envio manual, escolha filme ou série; para série, temporada inteira ou episódio. O aplicativo preenche o destino do envio no site. SRT e ZIP são aceitos sem renomear arquivos. A TV identifica episódios quando o nome permite, pede escolha quando há ambiguidade e guarda somente as legendas escolhidas. O player usa a mesma aparência e sincronização das legendas externas.

Execute `Atualizar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"`. O script valida os 12 arquivos, cria backup, aplica e chama o instalador existente para compilar Desempenho e instalar por ADB. Aceita `-Serial`/`-Tv`, `-Adb`/`-AdbPath` e `-SomenteAplicar`.

Arquivos modificados desconhecidos são recusados antes da primeira escrita. Em erro de escrita, restaura os arquivos existentes e remove somente os arquivos novos criados nesta execução. Falha de compilação ou instalação preserva a atualização e o backup para diagnóstico. Não desinstala o aplicativo.

As mudanças da r25 no assistente de sincronização são preservadas: esta entrega não toca nos três arquivos daquela correção. Não altera Gradle, assinatura, versão, importação de listas, histórico ou mecanismo de reprodução.

## Portal — necessário para envio manual

O instalador Android não publica o servidor. Leia **[portal/LEIA-ME-PORTAL.md](portal/LEIA-ME-PORTAL.md)** antes de testar o envio manual. Publique o Worker e os assets completos incluídos e configure o vínculo privado R2 `MANUAL_SUBTITLES`, mantendo DB, ASSETS e segredos atuais.

Sem essa publicação, o novo menu Android e a aparência podem ser utilizados, mas o envio manual ainda não funciona. Os QR e as contas dos provedores existentes continuam usando o fluxo anterior.

## Validação

Veja [validacao.txt](validacao.txt). Compilação e testes foram executados em cópia isolada. Não houve instalação na TV nem publicação na conta Cloudflare.
