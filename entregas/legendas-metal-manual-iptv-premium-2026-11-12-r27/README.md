# Fundo metálico e legendas manuais — r27

## Aplicativo

Esta entrega substitui o fundo de tema desfocado compartilhado por catálogos, listas, configurações e ativação pelo acabamento metálico nítido, com faixas laterais na cor do tema. Existem versões prata e grafite para claro e escuro nos cinco temas. A Home com suas imagens e ícones é preservada. Os fundos próprios do Kids e os desfoques temporários de sinopse/biografia não são alterados.

O novo fundo é desenhado e armazenado em uma camada estática do Compose: não faz download, não aplica blur e não gera ruído a cada quadro. A textura é discreta e as cores acompanham a seleção atual.

A entrada de Legendas tem três cartões horizontais com proporção de 1,55:1 na largura de TV. O azul da seleção usa #009EF5; os demais temas usam seus próprios acentos. Contas, escolha de filme/série e aparência receberam o mesmo acabamento. A frase sobre boa história não existe na tela.

O envio manual permite escolher filme, temporada inteira ou episódio no catálogo da lista ativa, e somente depois gera o QR. O site recebe o destino preenchido e aceita SRT ou ZIP sem renomear arquivos. A TV identifica episódios, permite conferir as associações e salva só as legendas selecionadas. O player usa as opções existentes de aparência e sincronização.

## Instalação Android

Execute `Atualizar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"` a partir da pasta extraída. O script cria backup, aplica os fontes, compila Desempenho e instala por ADB sem desinstalar nem apagar dados. Aceita `-Serial`/`-Tv`, `-Adb`/`-AdbPath` e `-SomenteAplicar`.

O pacote é cumulativo: aceita a base enviada em 12/11 e os arquivos conhecidos da r26. Arquivos desconhecidos são recusados antes da primeira escrita. Gradle, assinatura, versão, histórico, importação e os três arquivos da correção r25 não são substituídos.

## Site e conexão

**O site mantém sua identidade original preta e dourada.** Não recebe os temas visuais da TV. A tela nova usa as variáveis de cor, os botões, a tipografia e a estrutura atuais da central. O ZIP enviado novamente pelo usuário corresponde à mesma base de 15 arquivos; a entrega preserva essa base e acrescenta o envio manual.

- [Captura real do site no computador](capturas-site/envio-manual-computador.png)
- [Captura real do site no celular](capturas-site/envio-manual-celular.png)

As capturas foram feitas no navegador com os arquivos reais da interface e um destino de TV simulado. Não representam um upload já realizado na conta Cloudflare.

**A publicação do portal é separada da instalação Android.** O Worker completo, os assets e a configuração necessária estão descritos em [portal/LEIA-ME-PORTAL.md](portal/LEIA-ME-PORTAL.md). É necessário acrescentar o vínculo privado R2 `MANUAL_SUBTITLES`. DB e ASSETS existentes devem ser mantidos. O instalador Android não acessa sua conta Cloudflare e não publica o servidor.

## Validação

Veja [validacao.txt](validacao.txt). A compilação usa uma cópia isolada do projeto atualizado. O teste do aparelho físico e a publicação no Cloudflare ainda precisam ser feitos no ambiente do usuário.
