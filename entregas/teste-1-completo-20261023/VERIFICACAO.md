# Verificação do Teste 1 completo

## Ambiente

Compilação de verificação com AGP/Gradle do projeto, JDK 25, Kotlin do projeto e SDK Android. Os recursos locais da cópia de compilação incluem substitutos de imagens; o APK final deve ser gerado no projeto Windows com seus recursos reais. Não foi realizado teste físico de TV nem medida de tempo em TV.

Compilação Kotlin e processamento do manifesto concluídos. Treze cenários do núcleo passaram; os testes HTTP, SQLite/FTS e instalador também passaram. A análise completa de lint também passou, sem erros; permanecem avisos não bloqueantes do projeto. Compatibilidade com API 26 verificada por lint.

## Núcleo de importação

Harnesses externos ao aplicativo executam as classes Kotlin compiladas de produção. O JsonReader no harness JVM usa uma ponte Gson; o APK usa o JsonReader real do Android. Os DAOs falsos mantêm registros em mapas, mas executam os corpos transacionais de produção; as consultas e o esquema gerado pelo Room são verificados separadamente em SQLite.

Cobertura: leitura de 100 mil itens sem coleção completa, abertura antes de concluir o download, primeiro lote utilizável, reaproveitamento persistente, atualização sem regravar payloads inalterados, mudança de ordem, exclusão apenas após sucesso, erro de disco, autenticação, lista vazia inesperada, resposta truncada, recuperação por categoria e cancelamento ao trocar de lista.

M3U: uma única leitura de rede, classificação de canais/filmes/séries/episódios, título com vírgula, número de episódio, primeiro lote antes do EOF, reaproveitamento na reabertura, comparação incremental, exclusão de seção removida, rejeição de HTML/EXTINF incompleto/lista vazia inesperada, cancelamento de resposta pendente.

## Banco e busca

Consultas reais e esquema gerado pelo Room verificados em SQLite: isolamento de lista, poda por escopo, preservação de itens sem categoria na recuperação parcial, rollback de falha tardia e cascata de exclusão. FTS4 unicode61 verificado nas três tabelas: acentos, prefixo, várias palavras, inserção, renomeação, substituição, exclusão e preenchimento inicial. Testada substituição de registro mesmo com recursive_triggers desligado, sem deixar entrada órfã no índice.

## HTTP e DNS

Servidor HTTP local com a implementação real de DefaultPlaylistHttpClient e OkHttp 5 JVM: gzip transparente, leitura linha a linha, erro 403, limite de metadados e cancelamento de leitura bloqueada em menos de dois segundos. Não representa velocidade de provedor/TV. Configuração DNS: presets, literais IPv4/IPv6, URL DoH HTTPS, bootstrap obrigatório e rejeição de domínio no campo de IP e credenciais na URL. Não foi simulado o comportamento da rede do usuário nem a resolução de cada provedor DNS na TV.

## Instalador

PowerShell real, quatro bases conhecidas: aplicação, reaplicação sem novo backup, BOM/CRLF, recusa de alterações desconhecidas antes de mutação, reversão com bytes originais, remoção apenas de novos arquivos e preservação de local.properties. Nenhum teste é incluído no APK.

## Limites

Coil e Koin estão conectados ao fluxo real e compilados; renderização/foco/cache Android e Media3 precisam do teste visual do APK na TV. O VLC mantém DNS do sistema. Não foi implementado cadastro por QR/PIN. O cadastro pelo site e os recursos próprios continuam.
