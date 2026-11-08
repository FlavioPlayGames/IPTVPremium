# M3U nativa — menos trabalho na gravação de episódios

Atualização apenas do aplicativo, sobre m3u-leitura-rapida. A versão do aplicativo permanece a do seu projeto. Não há publicação de site, conversão para Xtream ou redução de conteúdo.

## Alteração selecionada

A tabela cached_episodes tinha dois índices secundários:

- playlistId, generationId, seriesId
- playlistId, generationId, seriesId, seasonNumber

O segundo também atende às consultas pelo prefixo de três campos do primeiro. Removemos somente o índice menor redundante. A chave primária e o índice completo permanecem. Os métodos de gravação e os limites de lote/fila continuam iguais; menos estruturas precisam ser atualizadas a cada episódio.

A versão interna do banco passa de 4 para 5. O Room executa uma migração com DROP INDEX IF EXISTS sobre esse único índice. Não apaga tabelas ou registros, não altera IDs, títulos, URLs, temporadas, números de episódio ou ordem. Favoritos, histórico e configurações de contas não são modificados. Instalações novas já criam o esquema otimizado.

## Evidência e limites

Foram comparadas inserções de várias linhas por comando e a remoção da redundância. As inserções múltiplas não trouxeram ganho no teste local, por isso não foram colocadas no aplicativo.

Na comparação SQLite/JDBC com arquivo local, WAL/NORMAL, 100 mil episódios de uma geração anterior preservados e 100 mil novos em 50 transações, o uso de IDs de 36 caracteres teve medianas de 2.539 ms com os dois índices e 2.040 ms com o índice completo: aproximadamente 19,7% a menos no tempo de escrita. A consulta de página de episódios manteve o mesmo plano, usando o índice completo.

É uma medição local de SQLite, não Room Android ou TV. O ganho no tempo total da sua TV ainda precisa ser medido; não prometemos reduzir os 2min22s na mesma proporção, porque processamento e banco se sobrepõem. A M3U continua importando o arquivo completo, inclusive todos os episódios.

## Aplicar e instalar

Extraia o ZIP em C:\Users\flavi\Downloads\m3u-banco-episodios. Os scripts ficam na raiz dessa pasta.

```powershell
Set-Location "C:\Users\flavi\Downloads\m3u-banco-episodios"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Depois de BUILD SUCCESSFUL:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Atualize por cima do aplicativo existente. Abra-o normalmente para o Room migrar o banco. Não é necessário desinstalar ou apagar os dados. Não há mudanças no site ou nos recursos privados de credenciais.

O instalador valida os fontes antes de escrever e guarda cópias em .m3u-banco-episodios-backups. Esse backup é dos fontes do projeto, não dos dados da TV. Depois de o banco migrar para versão 5, uma reversão de APK precisa ser compatível com esse esquema; restaurar os fontes antigos não reverte o banco já instalado. Por isso este pacote não inclui o desfazer genérico de código das atualizações anteriores. A transação da migração foi testada para restaurar o índice e os registros se ocorrer uma falha antes da conclusão.

## Medir na TV

Com o aplicativo atualizado e a TV conectada ao ADB, execute antes de iniciar a atualização da mesma lista M3U direta:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -PastaSaida "C:\Users\flavi\Downloads"
```

Quando aparecer Captura iniciada, atualize a lista existente. Mantenha M3U direta, sem excluir ou cadastrar outra entrada. O relatório novo fica em:

C:\Users\flavi\Downloads\diagnostico-importacao-m3u-atual-banco-v5.txt

Envie esse relatório para comparar com a medição anterior. Ele retém o marcador banco=M3U_EP_INDEX_R1 e versaoBanco=5, tempos e contagens. Só lê logs, não altera dados/perfil, não limpa o logcat e filtra URLs, senhas e textos livres.

## Validação

Compilação Kotlin/Lint; migração real executada em SQLite com a classe de migração do aplicativo; validação do esquema pelo RoomOpenDelegate gerado; preservação de todos os campos/IDs em 10 mil episódios com duas gerações, valores nulos e Unicode; páginas de episódios e estados ACTIVE/IMPORTING iguais; rollback transacional; integridade SQLite; instalação nova; paridade completa do importador; sincronização e M3U direta preservadas; instalador e filtro das métricas.

Não houve instalação automática, acesso ao provedor real, modificação do banco da sua TV ou teste de tempo real no aparelho neste ambiente.
