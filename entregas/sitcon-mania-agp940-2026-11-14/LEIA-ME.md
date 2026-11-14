# Sitcom Mania — versão nativa Kotlin

Conversão do HTML recebido para Jetpack Compose. Não utiliza WebView, HTML ou JavaScript no aplicativo. Projeto independente do IPTV Premium.

## Abrir no Android Studio

Extraia o ZIP em uma pasta nova e abra a pasta **SitconMania**, onde estão settings.gradle.kts e gradlew.bat. Aguarde a sincronização do Gradle e execute a configuração app.

Foram preservados os identificadores do projeto enviado: namespace/applicationId **com.example.sitconmania** e pasta **app/src/main/java/com/example/sitconmania/**. O nome exibido é **Sitcom Mania**, como no HTML; o pacote mantém a grafia sitconmania do ZIP original.

- Android mínimo: **9 / API 28** (minSdk 28).
- compileSdk e targetSdk: 37, conforme o projeto original. Isso é requisito de compilação, não o Android mínimo do aparelho.
- Gradle 9.6.0, Android Gradle Plugin 9.4.0 e JDK 25. O AGP foi ajustado para o limite suportado pelo Android Studio informado.
- Android Studio precisa do SDK 37 e do JDK configurado. As dependências são baixadas na primeira sincronização; o jogo executa offline depois de instalado.
- local.properties, caches, build, assinatura e configurações específicas do computador não estão no pacote. O Android Studio cria/configura local.properties para o SDK do seu PC.

## Organização dos arquivos Kotlin

Dentro de app/src/main/java/com/example/sitconmania/:

| Arquivo/pasta | Responsabilidade |
| --- | --- |
| MainActivity.kt | Entrada Android, ciclo de vida e pausa/retomada |
| model/GameModels.kt | Jogadores, perguntas, fases e estado serializável |
| data/QuestionBank.kt | As 40 perguntas originais e suas alternativas |
| game/GameEngine.kt | Regras, pontuação, bônus, dicas, turnos e resultado |
| game/GameViewModel.kt | Estado da tela, relógio monotônico e restauração |
| audio/GameAudio.kt | Música, efeitos locais e foco de áudio |
| ui/SitconManiaApp.kt | Navegação entre telas e confirmação de saída |
| ui/components/ComicComponents.kt | Fundo, painéis, botões, cabeçalho e barra de tempo |
| ui/screens/HomeScreen.kt | Apresentação, jogadores, avatares, rodadas e regras |
| ui/screens/GameScreen.kt | Pergunta, respostas, placar, tempo, dicas e progresso |
| ui/screens/CommercialDialog.kt | Intervalo de 15 segundos |
| ui/screens/ResultsScreen.kt | Vencedor/empate, ranking e revanche |
| ui/theme/Color.kt | Paleta azul-marinho/creme/coral/amarelo/verde-água |
| ui/theme/Type.kt | Tipografia |
| ui/theme/Theme.kt | Tema Compose aplicado ao jogo |

Os sete sons em **app/src/main/res/raw/** são necessários para compilar GameAudio.kt. Foram sintetizados localmente a partir das notas/efeitos do HTML; não dependem de streaming ou de serviços externos. O ícone da aplicação também utiliza vetor nativo.

## Regras preservadas

2 a 4 jogadores; campos vazios ignorados; nomes até 18 caracteres; 5 ou 8 episódios por jogador; 40 perguntas sobre dez sitcoms, sem repetição na partida, com alternativas embaralhadas. São 20 segundos por pergunta. Acerto vale 2 pontos; resposta em até 5 segundos soma mais 1. Toda quinta pergunta da partida tem bônus de 2 pontos quando acertada. Cada jogador pode eliminar duas alternativas erradas uma única vez por partida. Erro ou tempo esgotado abre intervalo de 15 segundos. Acerto permanece visível por 1,5 segundo antes de avançar. Ao final aparecem ranking, vencedor ou empate, revanche e retorno ao início.

O intervalo é o mesmo recurso do jogo HTML: uma pausa temática, sem integração com publicidade real. A música da apresentação e os efeitos respeitam o botão Som ligado/desligado e o foco de áudio do Android.

## Adaptações para Android

Telas responsivas para telefone/tablet em retrato ou paisagem. Campos usam teclado nativo e os avatares têm menu próprio. O cronômetro pausa quando a Activity perde o estado RESUMED, inclusive ao trocar de aplicativo. A partida mantém questão, alternativas, pontos, dicas e tempo na rotação; SavedStateHandle permite a restauração disponibilizada pelo sistema. Encerrar explicitamente o app não é um mecanismo de salvar partidas permanentes.

Voltar durante a partida abre confirmação e pausa o jogo enquanto ela estiver aberta. Respostas duplicadas e alternativas eliminadas são recusadas pelas regras, não apenas pela aparência dos botões. A tela permanece ligada enquanto há uma partida em andamento. Áudio e timers são liberados/pausados no ciclo de vida correspondente.

## Aplicar somente os fontes ao projeto original

O pacote separado **SitconMania-fontes-app.zip** deve ser extraído na raiz do projeto original, onde existe a pasta app. Faça backup antes de sobrescrever os fontes. Ele contém app/src/main, os testes e os recursos necessários; não copie apenas MainActivity.kt, pois ela utiliza os outros arquivos Kotlin. O pacote inclui gradle/libs.versions.toml com AGP 9.4.0. Copie também esse arquivo para corrigir a incompatibilidade com o Android Studio informado.

## Verificar e gerar APK

No terminal do projeto no Windows:

    .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug

APK de teste gerado em app/build/outputs/apk/debug/app-debug.apk. Para distribuição assinada, gere o APK/AAB com a chave do responsável pelo projeto no Android Studio.

Há um teste de interface em app/src/androidTest/java/com/example/sitconmania/GameFlowTest.kt. Para executá-lo, conecte um aparelho/emulador Android 9 ou superior e use:

    .\gradlew.bat :app:connectedDebugAndroidTest

Consulte docs/VALIDACAO.md para os resultados e as limitações da verificação realizada.

## Correção de compatibilidade AGP

Se você já abriu a versão anterior, edite gradle/libs.versions.toml e deixe a linha:

    agp = "9.4.0"

Depois clique em Sync Now / Sincronizar projeto com arquivos Gradle. O Android Studio informado aceita até AGP 9.4.0; a versão 9.5.1 do catálogo original foi substituída. Os fontes Kotlin e a compatibilidade Android 9 permanecem iguais.
