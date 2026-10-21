# Somente classificação: ordem das categorias

Base: **`java.zip` restaurado e enviado pelo usuário**. Esta entrega modifica exclusivamente a ordenação das categorias em canais, filmes e séries.

Os itens fixos permanecem no começo. A ordem das categorias passa a ser: lançamentos (anos do maior para o menor), legendados, 4K/UHD, demais categorias em ordem alfabética, categorias iniciadas por símbolos, cursos e, por último, adultos/XXX/18+. O comparador ignora acentos, cadeado e prefixos como “Filmes |”, “Séries |” e “Canais |”. IDs e categorias com nomes iguais são preservados.

**Os filtros existentes são mantidos exatamente como na base**, inclusive quais categorias são retornadas. Não são recriadas abas ou modificados filtros de conteúdo para fazer a ordenação.

## Escopo conferido

Dos 192 arquivos Kotlin do ZIP, **189 permanecem idênticos**. Os três ajustes estão em:

- `KidsPolicy.kt`: funções de ordenação em `CatalogSections`.
- `PlaylistCatalogRepository.kt`: somente o trecho de ordenação das categorias retornadas.
- `LiveTvScreen.kt`: uma linha para ordenar as categorias na apresentação dos canais.

Favoritos, capas, cache, downloads, navegação, player, importação, temas e imagens permanecem como no ZIP restaurado. `alteracoes.patch` contém a diferença completa (63 linhas); `auditoria-escopo.txt` registra a conferência.

## Aplicação

Execute `Atualizar-ApenasClassificacao.ps1` na raiz do projeto do Android Studio, usando o comando da entrega. Este é o atualizador específico desta correção. Exige a base restaurada, verifica os três arquivos antes de gravar, aceita LF/CRLF/BOM, permite reaplicação e cria backup em `.navigation-backups/apenas-classificacao-*`. Recusa alterações desconhecidas sem sobrescrevê-las e restaura os originais se ocorrer falha.

## Validação

Quatro testes focados em ordenação: grupos/anos/prefixos, acentos/cadeados, preservação de IDs duplicados e consultas reais do Room para os três tipos, mantendo os filtros originais. Instalador verificado com LF/CRLF+BOM, backup, reaplicação, preservação de recursos/Gradle, recusa de arquivo alterado e recuperação após falha injetada.

O ZIP contém apenas código. A compilação Kotlin e os testes locais usam aliases temporários para os 17 IDs de artes ausentes neste ambiente; esses aliases não integram a entrega. O APK final deve ser compilado com as artes reais do projeto do usuário. Esta validação não representa um teste do APK na TV.

Testes opcionais, com SDK/JDK e os recursos reais configurados:

```powershell
.\gradlew.bat --no-configuration-cache -I CAMINHO\apenas-classificacao\testes\validar-classificacao.gradle :app:testDebugUnitTest
```
