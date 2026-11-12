# Idiomas SubSource — r22

Correção da interpretação da r21. Idiomas permitidos: português brasileiro, inglês e espanhol. Português genérico ou de Portugal não é aceito. Restaura exatamente os dois fontes de idioma da r20, preservando paginação, pacotes, cache por episódio e ajuste manual. Não altera os demais provedores nem o site.

Pode aplicar sobre r21. Se a r20 ainda estiver instalada, os dois arquivos já estão no estado desejado: o aplicador informa que a alteração já está aplicada. Não é necessário passar pela r21. Outras bases são recusadas antes de qualquer alteração.

No PowerShell com a TV autorizada, use Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium". Cria backup externo dos fontes modificados e compila/instala com a assinatura local, sem apagar dados da TV. O aplicador fornece o comando Desfazer.ps1 com o caminho exato do backup.

Validação: os fontes finais são idênticos byte a byte aos da r20, que passou em compileDesempenhoKotlin e lintDesempenho, com 29 testes de sincronização e 39 testes SubSource. Não foi necessário repetir a compilação da mesma revisão de fontes. Os testes de aplicação, reaplicação e restauração serão registrados em VALIDACAO.md. A disponibilidade de cada idioma depende dos resultados do provedor; a API real não foi acessada neste ambiente.
