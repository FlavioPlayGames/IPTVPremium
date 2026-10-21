# Remover aba Legendados — 1.8

Remove a aba automática Legendados/Legendadas de Filmes e Séries, inclusive no Kids e no menu demonstrativo de filmes. Categorias do provedor identificadas como Legendados também deixam de aparecer como atalhos separados; os filmes continuam disponíveis em Todos e nas demais categorias correspondentes.

Desfaz o filtro de exclusividade introduzido na entrega anterior. Consultas simples e contadores de filmes voltam à versão anterior, sem a análise extra dos marcadores de legenda. O app não chama mais as contagens especiais de legendados nem cria categorias/contadores sintéticos. Uma seleção antiga salva com o ID da aba removida é interpretada como Todos, inclusive no pré-carregamento das capas. Não classifica nem baixa o catálogo novamente e não altera schema/versão do banco.

Favoritos, histórico, capas de 125 por tipo, Home com foco ampliado e catálogo preservado ao atualizar continuam como antes. A correção do histórico Kids permanece: somente filmes e episódios da categoria infantil, mesmo sem PIN habilitado. Não apaga conteúdo ou preferências.

## Executar

Na raiz do projeto já atualizado com a entrega de categorias: `Remover-Aba-Legendados-1.8.ps1 -Emulador emulator-5554 -Testar`.

Valida todos os hashes antes de escrever, cria backup em `.navigation-backups/remover-aba-legendados-*`, aplica cinco arquivos, compila e instala com `-r`, sem desinstalar ou apagar dados. Não altera `local.properties`, Gradle, versão ou imagens Kids. Nenhum APK publicado.

Resultado esperado: **OK (54 tests)**. Diagnóstico: `diagnostico-remover-aba-legendados-testes.txt` na raiz do projeto. Os oito testes Room de filmes foram atualizados para verificar a retirada da aba, as contagens completas, os legendados nas categorias normais, a compatibilidade com seleção antiga e a preservação de Favoritos/Kids.

Opções `-Verificar`, `-SomenteAplicar`, `-Projeto` e `-PacoteLocal` disponíveis. A medição opcional `-Medir` da entrega anterior permanece disponível. Alterações locais desconhecidas bloqueiam a aplicação antes de modificar arquivos.

## Validação realizada

- `assembleDebug assembleDebugAndroidTest compileReleaseKotlin`: compilação concluída.
- 8 verificações SQLite com o SQL real do DAO: aprovadas. Incluem Todos, gênero, busca, favoritos, contagem e restrição infantil com filmes normais e legendados juntos.
- Instalador PowerShell em cópia da entrega anterior: validado com backup exato, preservação dos demais arquivos, reaplicação e recusa de mudanças desconhecidas.
- 54 testes Android compilados, não executados aqui: a nuvem não possui emulador. O comando acima os executa no emulador do usuário.
- A compilação usa os mesmos aliases temporários para seis recursos Kids ausentes do upload Java. Não são entregues e não substituem as imagens Kids reais do projeto local.

A retirada do filtro elimina o trabalho extra conhecido, mas o tempo real de abertura na TV ainda precisa ser observado/medido. Não há garantia de um tempo específico sem teste no aparelho.
