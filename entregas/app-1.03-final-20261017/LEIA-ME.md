# IPTV Premium 1.03 — número interno 5 — revisão de instalação

Esta revisão inclui os dois ajustes aprovados e o prazo de 15 dias solicitado:

- Ao autorizar a instalação, orienta ativar “Permitir desta fonte” e voltar ao aplicativo; com a permissão concedida, abre o instalador.
- Solicita o resultado do instalador Android. Cancelamento exibe mensagem com Tentar novamente e Mais tarde.
- Tentar novamente revalida o APK já baixado; falha e cancelamento recebem mensagens diferentes.
- Mais tarde (ou Voltar no aviso) adia os avisos por 15 dias. O prazo é salvo no aparelho e respeitado também se outra versão for publicada durante esse período.
- Após o prazo, o aviso pode voltar na próxima abertura ou consulta bem-sucedida. A busca manual nas configurações permite atualizar antes.

Não inclui informações de arquitetura na tela do cliente, não muda os temas, teclado, favoritos, histórico, catálogo ou listas.
A confirmação e a autorização do Android continuam necessárias; não há instalação silenciosa.

## Aplicação pelo PowerShell

Executar Preparar-app-1.03-final.ps1 na raiz do projeto Android, com gradlew.bat e local.properties.
Também aceita -Projeto caminho e -NaoCompilar.
O preparador verifica as fontes, cria backup, prepara 1.03/code5 e compila com a assinatura existente no computador.
Depois verifica todos os cinco APKs na versão correta.
Aceita o projeto da 1.02/code4 ou o primeiro pacote de teste da 1.03/code5; reconhece arquivos já aplicados.
Aborta diante de fontes desconhecidas ou versão maior que 5, sem rebaixar.
Não inclui APK compilado na nuvem, chaves de assinatura nem configurações privadas.

A versão foi mantida em 1.03/code5 a pedido do usuário. Para substituir a primeira compilação de teste, instalar pelo computador com adb install -r (mesma assinatura), sem desinstalar ou limpar dados.
O atualizador dentro do aplicativo só oferece números internos maiores que o instalado.

## Verificação

Compilação Android assembleDebug concluída; todos os APKs gerados em 1.03/code5.
21 cenários do gerenciador em JVM, com serviços Android simulados: prazo de 15 dias, persistência após reabrir, limite exato do prazo, renovação, busca manual, versões novas durante adiamento, migração do primeiro adiamento, cancelamento, repetição, falha e retorno atrasado.
10 cenários do preparador: integridade das fontes, backup, preservação dos demais bytes locais, repetição segura, rejeição de versão maior, fontes alteradas, compilação e verificação da versão dos cinco APKs.
As verificações de tamanho, SHA-256, ABI, pacote e assinatura continuam antes de cada tentativa de instalação.

O usuário confirmou preservação dos favoritos e do progresso na primeira atualização para 1.03/code5. Conferir novamente após esta revisão.
Autorização e retorno de instalação precisam de validação real nos instaladores da TV e do celular; os testes em nuvem não substituem essa conferência.

## Site

Não é necessário mudar o Worker para contar os 15 dias: esse estado fica somente no aparelho.
O painel opcional completo permanece no pacote anterior:
https://github.com/FlavioPlayGames/IPTVPremium/tree/c1756420709015ef7be1149e410f2e3cee5aee88/entregas/atualizacao-opcional-1.03-20261017

Esta entrega não implanta Cloudflare nem publica APK em Releases. Continua sem hospedar APK no Cloudflare e sem ampliar o registro D1 de metadados.
