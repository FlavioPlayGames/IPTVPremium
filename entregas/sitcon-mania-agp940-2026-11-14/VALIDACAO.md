# Validação — 14/11/2026

- Projeto compilado com Gradle 9.6.0, AGP 9.4.0, Kotlin 2.2.10 e SDK 37. O AGP foi ajustado para a versão suportada pelo Android Studio informado. Nenhuma alteração no IPTV Premium.
- APK debug gerado e lido com aapt2: com.example.sitconmania, MainActivity lançável, minSdkVersion 28, targetSdkVersion 37, nome Sitcom Mania.
- Treze testes JUnit do GameEngine passaram, sem erros ou falhas. Cobrem integridade das 40 perguntas, jogadores vazios, quatro jogadores/oito rodadas sem repetições, limite exato do bônus de velocidade, episódio especial, alternância de turnos, erro/timeout e duração do intervalo, congelamento do relógio de pergunta no feedback, clique duplicado, dica única com duas alternativas incorretas, pausa na confirmação de saída, partida completa, empate, revanche e serialização/restauração do estado.
- Android Lint concluiu sem erros, incluindo verificação de APIs com minSdk 28. Os avisos encontrados são do template/versões: rótulo redundante da Activity, novas versões disponíveis de duas dependências e cores antigas do template sem uso. Não houve erro de API incompatível com Android 9 no código analisado.
- APK de testes de interface compilado. O teste abre o jogo nativo, usa a dica e solicita recriação da Activity, conferindo a manutenção da questão.
- Os testes de interface NÃO foram executados em aparelho/emulador neste ambiente. Não houve teste físico no Android 9 nem capturas reais do app. Compatibilidade mínima configurada e compilação/lint não substituem essa execução final.
- O app não solicita acesso à internet e utiliza sons locais. Não contém o HTML embutido ou WebView.

Checklist no dispositivo: iniciar com 2 e 4 jogadores; testar teclado/avatares; responder correto/errado e aguardar timeout; usar dica uma vez; alternar entre app e segundo plano; girar a tela; finalizar/revanche/empate; desativar o som e confirmar que não toca em segundo plano.

Revisão de compatibilidade: assembleDebug, lintDebug, testDebugUnitTest e assembleDebugAndroidTest passaram com AGP 9.4.0 em 2m18s. Os 13 testes de regras continuam sem falhas.
