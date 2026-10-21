# Categorias de filmes, Legendados e histórico Kids — 1.8

## Resultado

As entradas pessoais (Todos, Continuar assistindo e Favoritos) permanecem em suas posições. Nas categorias do provedor, a ordem passa a ser:

1. Lançamentos (anos mais recentes primeiro dentro desse grupo).
2. Legendados.
3. 4K/UHD.
4. Categorias comuns em ordem alfabética.
5. Categorias XXX/adultos/+18, cursos e nomes que começam com símbolo, no fim.

Símbolos no início têm precedência sobre Lançamentos/4K. Prefixos visuais como `Filmes |` são desconsiderados; o cadeado de controle parental inserido pelo app não muda a ordem. Não apaga ou desbloqueia conteúdo adulto. A classificação das séries permanece como antes.

Filmes identificados como legendados por marcadores `[L]`, `[LEG]`, `LEGENDADO` ou por sua categoria do provedor ficam fora de Todos, 4K e outros gêneros/categorias. A categoria própria Legendados reúne esses itens. Por confirmação do usuário, **Favoritos e Continuar assistindo mantêm os legendados salvos/assistidos**. Os contadores, buscas dentro de categorias e o download antecipado de capas usam a mesma regra. Detalhes/identificação por ID continuam disponíveis e nenhum histórico/favorito é apagado.

No Kids, Continuar assistindo verifica a categoria infantil do filme ou da série, mesmo sem PIN/controle parental habilitado. Itens desconhecidos ou sem correspondência infantil não aparecem. Favoritos também permanece sujeito ao filtro infantil; a exceção para legendados nas áreas pessoais não desativa o filtro Kids. O histórico normal permanece preservado.

## Aplicação

Na raiz do projeto: `Preparar-Categorias-Legendados-Kids-1.8.ps1 -Emulador emulator-5554 -Testar`.

Valida hashes, cria backup, aplica e compila. Instala com `-r`, sem desinstalar/limpar dados. Resultado esperado: **OK (54 tests)**. Diagnóstico: `diagnostico-categorias-legendados-kids-testes.txt` na pasta do projeto.

Aceita a entrega original de ícones/capas, a correção de paginação ou a entrega da Home/cache preservado. Inclui as correções anteriores de paginação/Home/cache para que a atualização seja cumulativa. Não altera versão, schema do banco, listas, credenciais ou recursos Kids locais. Não força reimportação do catálogo. Nenhum APK publicado.

Opções `-Verificar`, `-SomenteAplicar`, `-Projeto` e `-PacoteLocal` disponíveis. `-Medir` mantém a medição opcional de abertura da entrega anterior. Arquivos locais desconhecidos interrompem a aplicação antes de gravar.

## Validação

- APK debug, APK de testes e Kotlin release compilados.
- 7 testes JVM do código de classificação: aprovados.
- 10 verificações SQLite com o SQL real das anotações do DAO: aprovadas. Cobrem exclusão em Todos/gênero/busca, reconhecimento de marcadores/categoria, tratamento de categoria nula, exceção em favoritos, contadores e restrições Kids.
- Instalador PowerShell validado contra as três entregas anteriores, incluindo backup, preservação dos demais arquivos e bloqueio atômico de alterações desconhecidas.
- 54 testes Android compilados, incluindo 8 novos testes Room sobre legenda, contagem, prefetch e histórico Kids. Não executados na nuvem: o ambiente não tem emulador. O comando acima os executa no emulador do usuário.
- Recursos Kids ausentes do upload Java foram representados por aliases exclusivamente na compilação da nuvem, como antes. Não são entregues; esta atualização não altera imagens.
