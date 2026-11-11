# Validação do teste r15

- Alteração restrita a `ImportTuningPolicy.kt`: PADRAO de 500/262.144 para 2.000/2.097.152 registros/caracteres. Parâmetros de M3U nativa e limites do escritor não mudaram.
- Testes JVM com os parsers reais `XtreamDirectCatalogReader`, `DefaultXtreamParser` e `XtreamArrayStream`; adaptadores explícitos para Android JsonReader/Log usando Gson e JSON real.
- Casos 0, 1, 1.999, 2.000, 2.001 e 44.192 registros: contagens/ordem/IDs preservados, sem duplicação. A fixture sintética de 44.192 filmes produz 23 lotes nos leitores direto e de enquadramento legado. Isso não é uma medição dos dados reais da TV: apenas a contagem corresponde ao relatório anterior.
- Objetos maiores descarregam lotes antes de 2.000 pela barreira de caracteres. Contrapressão e cancelamento preservados.
- Aplicação, repetição idempotente, desfazer/reaplicar, proteção de edições e sintaxe dos scripts. PowerShell 7 e simulação do JSON do Windows PowerShell 5.1; não executado em Windows nativo.
- Comando de reversão testado em projeto isolado: ignora backup de outro projeto, restaura os valores anteriores, solicita instalação, permite repetir após restauração e recusa edições posteriores. Instalador substituído por stub nesse teste, sem conexão com TV.
- Nenhuma chamada real ao provedor ou medição de velocidade/RAM na TV. Sem promessa de ganho, pois as transações do escritor já eram limitadas a 2.000 antes do teste.

Configurações de assinatura/versão/Gradle e orçamento de armazenamento não foram alterados. Sem APK, chaves, listas completas, fontes privados da referência ou dados do usuário no pacote. A reversão restaura somente o arquivo desta experiência, preservando as correções anteriores.

Compilação Android `assembleDesempenho` concluída em 11/11/2026. APK arm64 de referência: 97097414 bytes, não distribuído. JDK 25, Gradle 9.6, SDK 37 e NDK 28.2.13676358 no host. Heap de 6 GiB foi somente parâmetro do comando de validação, sem modificar Gradle do projeto.
