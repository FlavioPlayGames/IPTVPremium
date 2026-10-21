# Foco contido, vermelho/azul e fundo de filmes — 1.8

- Vermelho: reforço cromático de 1,30 para 1,17 (redução de 10%). Mantém as superfícies neutras e os reflexos da arte.
- Azul: separação cromática e pico de intensidade equivalentes ao vermelho, preservando a identidade azul. A transformação é compartilhada pela Home, logo, ícones, contornos e botão Assistir que usam a paleta metálica.
- Home: o foco ilumina o metal do ícone e a tinta do nome nas próprias posições. Não amplia um retângulo da cena nem desloca o mármore/reflexos ao redor. Não desenha os retângulos azuis da marcação da referência. O destaque entra suavemente em 180 ms; a variante vertical também deixa de ampliar o recorte de fundo.
- Preparação normal dos filmes: a antiga arte de fundo deixa de ser exibida. Usa o mesmo mármore estático já desfocado, preto ou branco, na cor selecionada, e o logo transparente. Mantém o carrossel, seu movimento, prioridade da mídia selecionada e os 3 segundos mínimos obrigatórios.
- Kids: mantém a preparação e seus recursos originais. Por isso o arquivo legado da imagem anterior permanece somente para esse caminho/compatibilidade, sem ser carregado pela preparação normal dos filmes. Não foram alteradas as regras de favoritos/continuar ou a aparência interna do Kids.

Pacote de sete arquivos: quatro ajustes de interface, teste de navegação atualizado e os dois arquivos da correção de compatibilidade EMERALD/CYAN. Continua com cinco cores reais no menu. A correção de aliases é cumulativa, podendo ser aplicada com ou sem a última correção já instalada. Não altera recursos gráficos, banco, versão, importação, listas, histórico ou dados do aparelho.

## Validação

- assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin: compilação aprovada.
- Cinco testes JUnit executados sobre a política real e a geometria: navegação sem retorno indevido à primeira coluna, ida/volta aos utilitários, bloqueio parental, limites de alvos e nomes dentro dos respectivos itens e recuperação de foco. OK (5 tests).
- Instalador validado antes e depois da correção de aliases: validação sem escrita, sete atualizações, backups exatos, preservação de teste local do Kids/configurações/recursos, reaplicação e rejeição atômica de edições desconhecidas.
- O código da navegação fora do composable de preparação não mudou; MovieOpeningPolicy.kt e os arquivos Kids permanecem idênticos. Nenhum download extra nem desfoque em tempo real foi acrescentado.
- A suíte Android de 88 testes foi compilada, não executada aqui. -Testar a executa no emulador do usuário. A renderização/percepção das cores na TV requer conferência visual.

Abra o emulador e execute Preparar-Foco-Cores-Fundo-Filmes-1.8.ps1 na raiz do projeto com -Emulador emulator-5554 -Testar. Mantém a captura completa UTF-8 do Gradle em diagnostico-marmore-compilacao.txt e preserva os dados. Não precisa executar novamente os instaladores anteriores.
