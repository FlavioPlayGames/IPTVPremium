# Mármore branco/preto e cinco cores — 1.8

A Home usa as duas próprias pinturas aprovadas: mármore branco e mármore grafite, com a mesma coroa, logo e cinco objetos (canais, pipoca/rolo, claquete, urso e listas). Não foram substituídos por ícones genéricos ou redesenhados. Uma transformação de cor preserva o desenho, sombras e reflexos dos objetos nas variantes Dourado, Azul, Rosa, Roxo e Vermelho, compartilhando a mesma imagem em vez de carregar cinco cópias na memória.

Configurações → Aparência oferece cinco cores, nesta ordem, e as duas opções de mármore. A escolha preto/branco também vale para a Home e a prévia das configurações. Verde esmeralda e Ciano saíram do modelo e do menu. Uma preferência antiga nessas cores passa a Dourado. O instalador remove os oito recursos conhecidos dessas cores com backup; se uma dessas imagens foi personalizada localmente, preserva o arquivo, sem disponibilizar o tema.

## Interação e catálogos

- Na TV, preserva a composição completa da arte, sem cortar objetos. Os sete controles (cinco destinos, atualizar, configurações) são interativos e têm nomes acessíveis. A seleção amplia o objeto em 10%, em 180 ms, sem quadrado ou barra de seleção. Cima alcança os controles superiores; baixo retorna ao último destino da fileira. Direita/esquerda não atravessam as pontas.
- Modo parental Kids-only impede foco e clique nos destinos regulares. Em telas verticais menores, os mesmos destinos usam uma grade com rolagem.
- Fundos dos catálogos são duas imagens estáticas já embaçadas, sem desfoque calculado durante a navegação. Painéis translúcidos, ícones e seleção metálicos; capas comuns sem contorno, exceto quando selecionadas. A cor do botão Assistir continua acompanhando o tema.
- O cache adicional de arte é compartilhado e limitado a 8 MiB. Não aumenta a quantidade de capas em RAM, nem baixa artes pela rede. O consumo total de 350 MB não foi medido neste ambiente.
- Conteúdo, aparência interna e controles do Kids preservados. Sem alterações em importação, Room, favoritos, histórico, prioridade da mídia selecionada, carregamento mínimo de 3 segundos, navegação dos catálogos ou grade de 8/10 capas.

## Verificação

- assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin: BUILD SUCCESSFUL.
- Cinco testes JUnit executados sobre a política real de navegação: movimento horizontal sem retorno à primeira coluna; utilitários e retorno; bloqueio parental; regiões sem sobreposição e sem recortar legendas; recuperação de foco inválido. OK (5 tests).
- Instalador PowerShell validado em dois cenários: recursos originais e recurso antigo personalizado. Verificação sem escrita, 18 atualizações, remoções conhecidas com backup, preservação de configurações/recursos Kids/arquivos alheios, reaplicação e rejeição atômica de alterações locais desconhecidas.
- Os dois testes antigos das sete folhas de ícones foram atualizados para validar as duas artes aprovadas, os fundos estáticos, as cinco cores e a conversão das preferências antigas. Os outros testes de regressão permanecem; o comando -Testar espera OK (88 tests).
- Os testes Android foram compilados, mas não executados aqui: são executados no emulador do usuário. A renderização nativa e o desempenho na TV ainda precisam dessa conferência.

## Aplicação

Abra o emulador e execute o script na raiz do projeto com -Emulador emulator-5554 -Testar. Parte dos fontes Black/White de df0b3aa7ced39d4ee9c59035feb41b704f9d6fb2. Não desinstala o app nem limpa dados. Inclui backup dos fontes e das imagens antigas removidas; não inclui APKs, local.properties, recursos substitutos do Kids ou mudanças na versão 1.8.
