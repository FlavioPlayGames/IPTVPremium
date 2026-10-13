O projeto foi limpo a partir de IPTVPremium (22).zip. O ZIP original foi preservado.

Foram removidas 1.656 linhas Kotlin. 7 dependências diretas e 8 recursos sem referências (1.600.423 bytes).
A revisão incluiu referências Kotlin, XML, manifesto, consultas Room e carregamento dinâmico de recursos.

As implementações ativas de reprodução Media3/VLC, legendas, episódios, navegação, cache e paginação foram preservadas. As versões de Gradle, AGP, Kotlin, SDK e banco de dados permanecem iguais às do ZIP fornecido.

O pacote entregue contém os fontes, recursos e Gradle Wrapper. Não inclui local.properties, credenciais, .idea, .gradle, .kotlin, arquivos gerados de build ou metadados Git.
Para usar as consultas TMDB no seu computador, configure TMDB_READ_TOKEN no local.properties por um meio seguro. A ausência desse token não impede a compilação.

Arquivos alterados:
- app/build.gradle.kts
- app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/AppNavigation.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/PlaybackController.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/PlaybackModels.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/PlaybackScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/SubtitleSettingsScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/dao/PlaylistCacheDao.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/entity/PlaylistCacheGenerationEntity.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/data/parser/DefaultXtreamParser.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/data/parser/XtreamParser.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/live/LiveTvScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/movies/ActorDetailsScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/movies/MovieDetailsScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/movies/MoviesScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/series/SeriesDetailsScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/series/SeriesScreen.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/ui/theme/Color.kt
- gradle/libs.versions.toml

Arquivos removidos:
- app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/SubdlSubtitleRepository.kt
- app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/PlaylistCacheReader.kt
- app/src/main/res/drawable/iptv_premium_tv_banner_640x360.png
- app/src/main/res/drawable-nodpi/icone_todos.png
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
- app/src/main/res/mipmap-hdpi/ic_launcher_round.webp
- app/src/main/res/mipmap-mdpi/ic_launcher_round.webp
- app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp
- app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp
- app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp

Validação concluída:
- Compilação debug do projeto original: aprovada; confirmação repetida aprovada.
- Compilação debug da versão otimizada: aprovada; script completo repetido e compilação Kotlin reexecutada com sucesso.
- Integridade do ZIP de entrega e dos APKs gerados: aprovada.
- Lint do original: 29 erros, 67 avisos e 4 sugestões.
- Lint da versão otimizada: os mesmos 29 erros, 59 avisos e 4 sugestões. A comparação das mensagens, arquivos e trechos de código confirmou que nenhum erro novo foi introduzido.
- Os 29 erros são UnsafeOptInUsageError em APIs experimentais Media3 já presentes no projeto original. Eles impedem o lint de passar; a compilação debug passa. Corrigir essas anotações é um trabalho separado da remoção de código sem uso.
- O aviso restante de UnusedResources se refere a series_living_room.webp, usado por getIdentifier; o recurso foi preservado.
- Não há testes automatizados no ZIP fornecido. Execução em dispositivo/emulador não foi testada.

APK de debug original: 280.952.777 bytes. Otimizado: 276.503.644 bytes. Redução: 4.449.133 bytes (4,45 MB).
ZIP original: 19.049.622 bytes. ZIP de entrega: 17.486.173 bytes. Redução: 1.563.449 bytes (8,21%).

Ambiente da nuvem preparado para compilação com JDK 25, Gradle Wrapper 9.6.0, SDK 37.0 e Build-Tools 36.0.0. Os campos install_script e start_skill foram salvos no rascunho. A publicação continua sendo uma ação do usuário nas configurações do ambiente; uma nova tarefa ainda não foi validada.

