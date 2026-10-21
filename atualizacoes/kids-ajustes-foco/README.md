# Kids: seleção visível, Home alinhada e vídeo ampliado

O item com foco do controle usa **borda branca de 3 dp, marcador lateral e fundo escuro com mais contraste**. O item selecionado, quando o foco está em outro lugar, conserva a cor azul/rosa. Aplicado às categorias, canais, busca, organizar e prévia do vídeo do Kids. A borda não usa animação ou desfoque adicional.

Na Home Kids, a seleção passa a ser uma **barra branca entre o ícone e o nome**, sem retângulo ao redor. Azul e rosa compartilham a mesma estrutura: três imagens separadas, bases alinhadas, nomes centralizados na mesma altura e espaço até a barra. A prévia em Aparência usa a mesma estrutura. O fundo rosa foi substituído por uma versão muito mais escura, com menos claridade no piso e nas nuvens; o urso do menu principal tem os reflexos brancos atenuados, acompanhando a cor do tema.

Nos canais Kids, foram removidos o **EPG e a coluna de dias**, e suas consultas deixam de ser feitas nessa tela. A área do vídeo recebe toda a altura e largura liberadas, preservando a proporção da transmissão (pode haver barras conforme o formato do canal). OK abre o canal em tela cheia. Os canais normais conservam o guia e a apresentação anteriores.

## Preservado

A rolagem, a estrutura de duas fileiras, a fila de capas e os limites de cache não foram alterados. Permanecem os grupos de 5, ciclo inicial de 30 e acréscimo de 30 ao restarem 25; JPEG 90, até 300×450, RGB_565 opaco, Xtream 250/M3U 2.000, imagens 100 MB dentro do limite de armazenamento gerenciado de 350 MB. Kids compartilha os mesmos dados/caches, filtros, favoritos e histórico.

As novas artes são recursos locais do APK e usam o cache de arte já existente (32 MiB fixados + 8 MiB LRU). O atlas dos ícones é compartilhado entre as três opções e contém as duas cores; não há três decodificações nem um novo cache. As artes desta revisão acrescentam cerca de 1,45 MiB de arquivos em relação à revisão anterior. O tamanho dos recursos do aplicativo é separado do cache gerenciado.

## Atualização pelo PowerShell

`Atualizar-KidsFoco.ps1` contém o código e as imagens. É cumulativo e aceita ZIP (11), Kids `fb94c04`, ciclos de capas `69b66a0` e visual Kids `54154df`. Verifica todos os hashes antes de gravar; aceita LF/CRLF/BOM; confere PNG por bytes; cria backup em `.navigation-backups/kids-ajustes-foco-*`; restaura após falha e permite reaplicação. Alterações desconhecidas são recusadas sem sobrescrever o projeto. Gradle, assinatura e arquivos privados são preservados.

Execute o comando fornecido na entrega na raiz do projeto aberto no Android Studio. `alteracoes.patch` mostra o ajuste sobre `54154df`. Os fundos em `artes/` estão sem os ícones: a tela monta o fundo e o atlas separadamente, permitindo alinhamento real.

## Validação

77 testes locais sem falhas ou testes ignorados e compilação dos APKs de aplicativo/instrumentação. Cobrem contraste branco acima de 7:1 nas superfícies de foco azul/rosa, alinhamento e espaço dos nomes, navegação por controle, ausência de EPG/dias somente no Kids, nenhuma consulta EPG nessa sessão e ampliação da área do vídeo. Também permanecem os testes de temas, PIN, filtros, retorno à categoria, importação e cache/resolução/armazenamento.

Instalador verificado nas quatro bases, com LF e CRLF/BOM, integridade binária, reaplicação, backup, preservação do Gradle privado, recusa de arquivo modificado e restauração após falha. A aparência final deve ser conferida na TV; os testes locais não medem o desempenho do aparelho.

Testes de revisão opcionais, com SDK/JDK do projeto configurados:

```powershell
.\gradlew.bat --no-configuration-cache -I CAMINHO\kids-ajustes-foco\testes\validar-kidsfoco.gradle :app:testDebugUnitTest
```
