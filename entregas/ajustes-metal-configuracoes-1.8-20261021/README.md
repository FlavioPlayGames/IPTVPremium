# Metal uniforme, logo transparente e Configurações — 1.8

Entrega cumulativa sobre a Home de mármore branco/grafite nas cinco cores (Dourado, Azul, Rosa, Roxo e Vermelho). Inclui os ajustes pedidos após a primeira instalação.

- Vermelho com separação cromática reforçada em 30% e menor mistura rosada, sem aumento geral do brilho.
- Ícones dos catálogos e contorno de seleção usam os mesmos tons de sombra/reflexo e a mesma transformação de cor dos objetos metálicos da Home. Os ícones vetoriais conservam seus símbolos, recebendo o material metálico; não foram trocados por miniaturas dos objetos 3D.
- Atualizar e Configurações: reflexo de seleção localizado no desenho metálico, sem zoom ou deslocamento do mármore atrás do botão. Os cinco destinos mantêm o aumento suave anterior.
- Logo dos catálogos e cabeçalho das Configurações usa PNG com alpha real, sem retângulo de mármore. A coroa e as letras recebem a cor selecionada.
- Central de Configurações e suas páginas padrão: fundo de mármore estático, cartões translúcidos neutros, sombras discretas, textos adaptados ao preto/branco e foco metálico. As nove opções, navegação e ações foram mantidas. Componentes de licença, QR, player e a tela parental dedicada não tiveram suas regras reescritas.
- O interior do Kids, listas, importação, histórico, favoritos, prioridade da mídia selecionada e regras de cache permanecem preservados. O cache de arte continua limitado a 8 MiB; isso não equivale a uma medição do consumo total do app.

## Compilação e diagnóstico

O instalador cria automaticamente diagnostico-marmore-compilacao.txt na raiz do projeto antes de iniciar o Gradle e grava nele a saída de compilação. Se a falha local anterior se repetir, esse arquivo deve ser enviado para identificar a causa. A captura automática não resolve, por si só, um erro local cuja mensagem real ainda não foi recebida.

O pacote aceita os fontes Black/White anteriores e a entrega de mármore já aplicada; não precisa limpar dados nem reinstalar do zero. Mantém a versão 1.8. Remove apenas os recursos conhecidos de Verde/Ciano com backup, preservando imagens antigas personalizadas sem reativar os temas.

## Validação

- assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin compilados com sucesso.
- PNG do logo inspecionado: alpha entre 0 e 255 e canto inteiramente transparente. O teste de recursos existente agora também verifica a transparência real do logo.
- Cinco testes de navegação da Home preservados. O comando -Testar executa a suíte de 88 testes Android no emulador do usuário. Essa suíte foi compilada, não executada neste ambiente.
- Instalador validado com fontes anteriores, artes antigas personalizadas e instalação de mármore já aplicada: leitura sem escrita, aplicação, backups, remoções conhecidas, preservação de arquivos externos ao pacote e rejeição atômica de edições locais desconhecidas.
- Não houve renderização Android aqui; o visual e a seleção ainda precisam ser conferidos no emulador/TV.

Abra o emulador e execute Preparar-Ajustes-Metal-Configuracoes-1.8.ps1 na raiz do projeto com -Emulador emulator-5554 -Testar. O instalador não publica APKs, não limpa dados e não altera local.properties.
