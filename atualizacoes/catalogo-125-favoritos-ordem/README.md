# Catálogo: 125 capas, favoritos dos canais e ordem compartilhada

Atualização para a versão de **`java.zip` enviada nesta conversa**. Preserva a Home, os temas e as alterações recentes desse código. Altera somente sete arquivos Kotlin; não substitui imagens, Gradle, assinatura ou configurações privadas.

## Capas

- Mantém o cache comprimido de **125 capas para filmes e 125 para séries**, com rotação LRU e proteção das capas visíveis, conforme a base enviada. Kids e catálogo normal reutilizam o mesmo cache do respectivo tipo.
- Baixa em ordem, em lotes de até **5**, completando a fileira antes da seguinte. Na grade de quatro, respeita as fileiras de quatro; páginas e última fileira podem formar lotes menores.
- A antecipação para ao completar a janela de 125 posições a partir do começo da área visível. Na grade de dez visíveis, isso deixa até 115 posições à frente. Ao descer uma fileira, repõe somente o que falta para a nova janela. URLs repetidas, ausentes ou indisponíveis podem resultar em menos arquivos.
- Corrige a varredura que podia continuar até o fim da categoria, substituindo antecipadamente as primeiras capas no LRU. Consulta páginas locais do Room, sem avançar, descartar ou reposicionar as páginas da grade na tela.
- A rolagem e a mudança de foco deixam de pausar a fila. Enquanto a categoria continua aberta, ficar parado ou colocar a Activity em segundo plano não cancela o trabalho. A fila fica vinculada à categoria e ao processo; se o Android encerrar o processo, retoma na próxima entrada, reaproveitando os arquivos existentes.
- Trocar de categoria ou selecionar um filme/série cancela a fila anterior. A seleção de mídia preserva o bloqueio de antecipação e a prioridade da nova tela. Downloads fora da tela também recebem um observador de cancelamento para desconectar a conexão sem esperar o timeout de leitura.
- Não prepara 125 bitmaps na RAM. Conserva a arquitetura de arquivos comprimidos e carregamento dos cards da base, a pausa por pressão de memória, o máximo de duas requisições antecipadas, JPEG 90, dimensões 300×450 e os limites de **100 MB de imagens / 350 MB de armazenamento gerenciado**. Não inicia simultaneamente a antecipação de filmes e séries em telas fora da navegação ativa.

## Favoritos dos canais

OK curto abre o canal. Segurar OK por **1 segundo** adiciona/remove dos favoritos uma única vez, sem abrir o canal ao soltar. O timer funciona sem depender das repetições do controle, não reinicia a cada repetição e é cancelado ao perder o foco ou o foco da janela. Toque/mouse seguem o mesmo intervalo.

O coração agora usa o mesmo `CatalogFavoriteHeart` dos filmes: fino e vazio quando desligado, preenchido na cor do tema quando favorito. Não cria um foco separado no D-pad. A linha dos canais normais mantém o contorno de seleção sem o preenchimento preto sólido, e deixa de usar o `combinedClickable` que desenhava a indicação padrão. A seleção reforçada do Kids é preservada.

## Ordem das categorias

Os itens fixos de navegação continuam no começo. As categorias da lista usam o mesmo comparador em canais, filmes e séries:

1. Lançamentos, com anos do maior para o menor.
2. Legendados.
3. 4K/UHD.
4. Demais categorias em ordem alfabética.
5. Categorias iniciadas por símbolos.
6. Cursos.
7. Adultos/XXX/18+.

A ordenação ignora acentos, o cadeado de apresentação e prefixos como “Filmes |”, “Séries |” e “Canais |”. Conserva IDs distintos mesmo com nomes iguais. Categorias **Legendados fornecidas pela lista** deixam de ser ocultadas no retorno do repositório; não são criadas cópias de filmes ou séries. As regras do controle parental continuam valendo.

## Aplicação e backup

Execute `Atualizar-Catalogo125.ps1` na raiz do projeto aberto no Android Studio, conforme o comando da entrega. Todos os arquivos são verificados antes da gravação. Aceita LF e CRLF/BOM, permite reaplicação, cria backup em `.navigation-backups/catalogo-125-favoritos-ordem-*` e restaura após falha. Se houver outra versão de um dos sete arquivos, recusa a alteração sem sobrescrever o projeto. `alteracoes.patch` permite revisar a diferença exata sobre o Java enviado.

## Validação e limites

21 testes locais cobrem o limite e a reposição da janela, fileiras de quatro/cinco, cancelamento, leitores protegidos, LRUs independentes, reutilização dos arquivos após reinício, navegação durante a antecipação, Activity em STOP, prioridade da mídia selecionada, ordenação nas três consultas do Room, filtros Kids/parentais e gesto de favorito curto/longo sem roubar o foco. Instalador validado com LF/CRLF+BOM, reaplicação, preservação das artes/Gradle, recusa de arquivo desconhecido e restauração após falha de gravação injetada.

O ZIP recebido contém apenas Java/Kotlin e não inclui os 17 recursos novos da Home/tema referenciados pelo código. Para compilar o Kotlin e executar esses testes locais, os IDs ausentes receberam aliases temporários apenas no ambiente de teste. **Esses aliases não são publicados nem aplicados ao projeto. Não foi validado um APK final com as artes reais dessa versão nem o desempenho na TV.** A compilação final deve usar os recursos reais já presentes no seu projeto do Android Studio.

Com SDK/JDK e recursos do projeto configurados, os testes de revisão podem ser executados com:

```powershell
.\gradlew.bat --no-configuration-cache -I CAMINHO\catalogo-125-favoritos-ordem\testes\validar-catalogo125.gradle :app:testDebugUnitTest
```
