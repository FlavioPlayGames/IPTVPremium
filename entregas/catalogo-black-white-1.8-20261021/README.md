# Catálogos Black e White — 1.8

Os catálogos regulares de canais, filmes e séries usam fundo neutro Black ou White, escolhido em Configurações → Aparência → Fundo de canais, filmes e séries. A escolha fica salva e é independente das sete cores existentes.

O logo vetorial com coroa, os ícones vetoriais, a busca e o relógio usam a cor sólida do tema. Apenas o contorno da seleção usa um gradiente metálico acetinado, com sombras e reflexos contidos; o rosa segue a referência rosé. O botão Assistir dos detalhes acompanha a cor do tema. Os detalhes preservam a arte real, informações e avaliações pessoais.

Não altera a Home, imagens, controles ou paletas do Kids. Os componentes compartilhados delegam ao comportamento anterior quando KidsAppearance.active. Todos os arquivos da pasta ui/kids e seus recursos permanecem intactos. Preserva as regras de favoritos/continuar do Kids, cache, importação, prioridades, navegação, 4/5 colunas, 8/10 capas e preparação mínima de 3 segundos.

## Validação

- assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin: BUILD SUCCESSFUL.
- Instalador PowerShell validado: leitura sem alteração, aplicação de 13 arquivos Kotlin, backup exato, preservação de arquivos externos ao pacote, reaplicação e rejeição atômica de alterações locais desconhecidas.
- Não houve execução de testes Android nem renderização visual no ambiente de compilação. O parâmetro -Testar executa os 83 testes de regressão da entrega anterior no emulador do usuário; não substitui a conferência visual de Black e White e dos temas na TV.
- Não inclui APKs, arquivos de configuração privada, recursos substitutos nem alterações no banco ou na versão.

Abra o emulador antes de executar. Use o script a partir da raiz do projeto com -Emulador emulator-5554 -Testar. O instalador atualiza o app usando install -r e mantém seus dados. Esta entrega parte dos fontes de avaliações/prioridade publicados em e23a0e94cee446b284dc91e0f191cd5dfba07bed.
