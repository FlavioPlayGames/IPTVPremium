# Configurações — grade aprovada

Grade de nove opções, com três colunas em telas largas e adaptação em telas menores. Preto e dourado, ícones vetoriais, foco visível, retorno à última opção e botão Voltar. Não acrescenta bibliotecas ou imagens ao APK.

- Meu aparelho: identificação, acesso, versão e informações do Android.
- Minha assinatura: estado e validade recebidos da central, QR Code para renovação.
- Reprodução: preferência persistente Media3/VLC e próximo episódio automático. HDR/Dolby Vision continuam automáticos e o fechamento dos controles permanece em cinco segundos.
- Legendas: abre a tela existente de provedores, QR Code, sincronização e aparência.
- Categorias: ordenação padrão, A–Z ou Z–A, independente para canais, filmes e séries. A mudança vale ao reabrir a seção. Ocultação individual não faz parte desta entrega.
- Aparência: tamanho dos textos padrão, maior ou extra grande, preservando a escala de acessibilidade do Android.
- Histórico: exclusão confirmada de histórico e posições de filmes ou séries em todas as listas locais. Favoritos, cadastros e credenciais são preservados; histórico legado apagado não é reimportado. Limpeza de histórico de canais não foi acrescentada.
- Atualizações: versão atual e consulta ao mecanismo obrigatório já existente.
- Controle parental: PIN opcional de 4 a 8 números, inicialmente desativado, para acessar o aplicativo inteiro ao abrir ou voltar. Alterar ou desativar exige validar o PIN atual. Não é filtragem por idade ou categoria. O PIN não é o código do portal e não tem recuperação pelo site. Armazenado como hash PBKDF2 com sal, fora do backup Android; cinco erros impõem espera de 30 segundos.

## Aplicação

Execute `Aplicar-configuracoes-grade-aprovada.ps1` na raiz do projeto, depois `Gerar-APKs.ps1`. Instale um APK compatível sobre a instalação atual, usando a assinatura original, sem desinstalar. O script aceita a entrada de configurações original ou a tela anterior com dois cartões; valida todos os trechos e cria backups antes de alterar arquivos.

Não altera configuração de assinatura, versão, Firebase, domínio, site ou requisito de Android. Não implementa a otimização de armazenamento, que continua sendo tarefa separada.

## Verificação

Compilação assembleDebug concluída. Cinco APKs gerados, com código e recursos iguais e bibliotecas específicas por processador. Testes executando o código real das preferências, do armazenamento do PIN e do histórico, com adaptadores de ambiente: persistência, validação, limite de tentativas, troca de PIN, remoção seletiva e preservação de favoritos. Replay do patch conferido para a tela original e a anterior, com quebras LF/CRLF.

A aparência e a navegação ainda precisam ser conferidas em TV e celular físicos. Nenhum APK compilado no ambiente de desenvolvimento deve substituir o assinado no computador do usuário.
