# Home, canais e reutilização do catálogo — 1.8

Entrega incremental sobre os ícones/capas enviados em 21/10. Também inclui a correção anterior da paginação, aceitando tanto a instalação anterior quanto a versão com essa correção já aplicada. Mantém versão, dados de acesso, listas, favoritos, histórico, recursos Kids e artes dos sete temas. Não publica APKs.

## Alterações

- Home: volta a seleção por ampliação suave do ícone (+10%, 180 ms), retirando a barra. Texto fica alinhado e parado. Não adiciona contorno/quadrado.
- Canais normais e Kids: remove a entrada **Assistidos recentemente**. Não apaga os registros de histórico e não altera Continuar assistindo de filmes e séries.
- Atualizar na Home: continua limpando arquivos temporários/imagens e reiniciando o processo, mas **preserva o Room com o catálogo já importado**. Antes, `clearAllTables()` apagava esse banco, causando nova importação. O comportamento passa a se aproximar de Limpar cache do Android, sem apagar dados do aplicativo.
- Recebimento da central: repetir a mesma fonte, renomeá-la ou reativá-la com a mesma conexão não cria outra solicitação de importação. Mantém pedidos genuínos pendentes e seu marcador de conclusão. Se mudar formato, URL, servidor, usuário, senha ou alternativa M3U, continua solicitando importação. Uma conexão editada enquanto desativada é lembrada para importar ao reativar.
- Abertura: a atualização automática de catálogo aguarda pelo menos 60 segundos depois da liberação da Home, sem bloquear seus controles. Catálogo existente sem o antigo carimbo de atualização passa a ser reaproveitado; a ausência desse carimbo não força uma importação. A verificação de licença/central permanece e as atualizações periódicas existentes continuam após a espera, respeitando seus intervalos. Não há promessa de um tempo específico de abertura na TV sem medição no aparelho.
- Primeira lista, banco inexistente/dados apagados e alterações reais de conexão continuam exigindo importação. Instalar APK com `install -r` preserva o catálogo; não exige troca de versão/schema do banco.

## Instalação e medição no emulador

Na raiz do projeto: `Preparar-Home-Cache-1.8.ps1 -Emulador emulator-5554 -Testar -Medir`.

Valida hashes antes de tocar nos arquivos, cria backup em `.navigation-backups/home-cache-preservado-*`, aplica 11 arquivos, compila e instala sem desinstalar. Resultado esperado: **OK (39 tests)**. Testes usam fontes fictícias e não acessam o provedor da lista.

Depois dos testes, `-Medir` fecha e reabre o processo sem limpar cache/dados e coleta somente os logs de abertura/armazenamento. A licença e a lista do emulador precisam estar configuradas para medir uma abertura normal.

Arquivos de saída na pasta do projeto:

- `diagnostico-home-cache-preservado-testes.txt` — testes Android.
- `diagnostico-home-cache-abertura.txt` — tempo até interface liberada, cache disponível e etapas da abertura. A coleta espera no máximo 90 segundos. Se o acesso estiver bloqueado ou faltar lista, o resultado precisa ser interpretado nesse contexto.

`-Verificar` confere sem aplicar; `-SomenteAplicar` não compila; `-Projeto` informa outra raiz. `-PacoteLocal` aceita o mesmo ZIP verificado por SHA-256. Não altera `local.properties`, Gradle ou recursos privados.

## Validação

- APK debug, APK de testes e Kotlin release compilados.
- 6 testes de integração JVM com o DataStore real e o código de produção do registro: aprovados. O teste usa diretório temporário em vez do ApplicationProvider Android; não representa execução no emulador.
- 9 testes JVM de identidade de fonte, espera da atualização e limpeza de cache preservando arquivos de banco/configuração/histórico: aprovados.
- Instalador PowerShell validado em cópias com e sem a correção anterior de paginação: backup exato, preservação dos demais arquivos e recusa de modificações locais desconhecidas antes da aplicação.
- Os 39 testes Android foram preparados/compilados. Não executados aqui: este ambiente não tem emulador. O comando de instalação os executa no emulador do usuário. Não foi medido tempo de abertura ou PSS da TV.
- Como nas entregas anteriores, o ZIP original contém apenas Kotlin/Java; seis imagens Kids ausentes foram representadas por aliases exclusivamente na compilação da nuvem. Esses aliases não são entregues: as imagens Kids locais são preservadas. Esta correção não altera recursos de imagem.
