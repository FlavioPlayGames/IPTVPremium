# Ícones dos temas e cache de 125 capas — versão 1.8

Aplicação incremental sobre o `java.zip` enviado em 21/10/2026. Mantém a versão local, listas, histórico, limites de memória e recursos Kids existentes. Não contém APKs.

## Comportamento

- Ícones Canais, Filmes, Séries, urso Kids no modelo aprovado e Listas padronizados nos sete temas: dourado, azul, roxo, vermelho, rosa, esmeralda e ciano. Acabamento metálico com brilho moderado. Textos alinhados; seleção por barra curta entre ícone e texto.
- Prioridade de download para as capas visíveis. Depois, percorre a categoria na ordem atual em grupos de cinco, concluindo cada grupo antes do próximo. Com quatro colunas, os grupos de download continuam com cinco URLs, sem mudar o layout escolhido.
- Limites separados de **até 125 arquivos de filmes e 125 de séries em disco**. Ao continuar, substitui os menos recentes, protegendo capas visíveis e gravações ativas. Arquivos antigos compatíveis são reaproveitados sem nova requisição.
- Apenas pequenos lotes de URLs são lidos do banco. A consulta não avança o Paging da tela, não muda a seleção nem carrega o catálogo inteiro na memória. Mantém o limite adaptativo de bitmaps existente, de até 20 MB, e o orçamento global de armazenamento existente, que também pode remover arquivos sob pressão.
- Respeita categoria, pesquisa, ordenação, favoritos, controle parental e modo Kids. Uma mudança de permissão cancela o pré-carregamento anterior. Continuar assistindo usa suas capas visíveis, sem buscar uma categoria inteira diferente.
- Continua durante inatividade e em segundo plano enquanto a categoria permanecer aberta e o processo estiver ativo. O Android pode suspender ou encerrar processos; não é um serviço permanente e não continua após parada forçada.

## Instalação no emulador

Na raiz do projeto Android, execute `Preparar-Icones-Capas-125-1.8.ps1 -Emulador emulator-5554 -Testar`.

O instalador baixa o ZIP desta entrega e confere SHA-256, valida todos os arquivos antes de modificar o projeto, cria backup em `.navigation-backups/icones-capas-125-*`, compila e instala sem desinstalar nem apagar dados. Aceita APK universal ou por ABI, inclusive quando existe somente o APK x86. Não altera `local.properties` nem `app/build.gradle.kts`.

Opções: `-Verificar` valida sem aplicar; `-SomenteAplicar` aplica sem compilar; `-Projeto` informa a raiz; `-PacoteLocal` usa uma cópia do ZIP com o mesmo hash. Se houver código local diferente do arquivo enviado, interrompe antes de aplicar para preservar alterações.

Os testes selecionados geram `diagnostico-icones-capas-125-testes.txt`, com resultado esperado **OK (22 tests)**. Não acessam os servidores das listas.

## Validação realizada

- Compilação debug, APK de testes Android e Kotlin release: concluída.
- 14 testes JVM de limite, expulsão, proteção de visíveis, separação filme/série, retomada, grupos ordenados e cancelamento: aprovados.
- Instalador PowerShell: aplicação em cópia do código enviado, backup exato, preservação de configuração/recursos Kids, reaplicação e rejeição de mudanças desconhecidas: aprovados.
- 22 testes Android preparados e compilados; **não executados aqui**, pois este ambiente não tem emulador. O comando acima executa-os no seu emulador. Desempenho e consumo total de RAM na TV ainda precisam de medição no aparelho.

O upload contém apenas Java/Kotlin. Para compilar na nuvem foram usados aliases temporários para seis recursos Kids preexistentes, ausentes do ZIP. Esses aliases **não fazem parte desta entrega**: as imagens Kids reais do projeto local são preservadas. As sete novas artes padronizadas foram incluídas e compiladas. Não se considera validada aqui a renderização dos fundos Kids locais.
