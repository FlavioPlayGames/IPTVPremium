# Atualização de navegação

Atualizador para o projeto do arquivo `IPTVPremium (5).zip`.

No terminal PowerShell do Android Studio, na raiz do projeto, execute o comando indicado no chat. Ele baixa e executa `Atualizar-Navegacao.ps1` deste diretório. O script verifica a versão dos arquivos e cria um backup em `.navigation-backups` antes de atualizar. Se detectar mudanças posteriores, interrompe sem sobrescrevê-las.

- Voltar da grade ou da barra superior retorna à última categoria.
- Cima na primeira fileira vai à pesquisa.
- Baixo na barra superior retorna à grade.
- Direita nas categorias entra na grade.
- Capas visíveis recebem foco diretamente; cache e antecipação de capas preservados.

Aplicativo e testes Android compilados. Atualizador testado com aplicação, backup, repetição e conflito. Testes instrumentados de navegação ainda precisam ser executados em aparelho ou emulador.

`navegacao.patch` contém as mudanças de código para revisão. Nenhuma configuração local ou credencial está incluída.
