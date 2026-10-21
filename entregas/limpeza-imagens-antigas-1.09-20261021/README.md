# Limpeza de imagens antigas — IPTV Premium 1.09

Remove dez arquivos antigos da Home, somando **6.748.472 bytes (6,44 MiB)** da pasta res: os ícones isolados home_channels, home_movies, home_series, icon_lista, icon_atualizar e icon_configuracoes; e as quatro folhas antigas theme_blue_icons, theme_pink_icons, theme_purple_icons e theme_red_icons.

Esses recursos estavam referenciados somente pelo encaminhamento antigo de imagens e por uma preparação da Home sem chamadas nas telas atuais. O encaminhamento e a função sem uso foram removidos. A Home atual continua usando os PNGs separados, seus fundos e rótulos nativos. O recorte de urso antigo usado pelo fallback Kids e a sala de preparação das séries foram preservados.

A limpeza é conservadora: outros recursos ainda referenciados continuam no projeto. Não reduz resolução nem converte imagens atuais. O tamanho exato economizado no APK depende da compressão da compilação.

O pacote não contém imagens e não repõe arquivos que você excluiu. Se uma imagem antiga já estiver ausente, permanece ausente. Se um arquivo com esse nome tiver conteúdo personalizado, será preservado. Uma referência adicional ou carregamento dinâmico no código local interrompe a limpeza antes de qualquer alteração.

Execute Limpar-Imagens-Antigas-1.09.ps1 na raiz do projeto. Com o emulador aberto, acrescente -Emulador emulator-5554 -Testar. É criado backup dos arquivos efetivamente alterados em .navigation-backups, fora de res. Nenhum dado do aplicativo é limpo. A versão permanece 1.09.

Validação: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin; auditoria de referências; testes do instalador com arquivos normais, BOM/CRLF, imagens já excluídas, imagem personalizada e referência XML adicional. Os testes instrumentados são compilados na nuvem; -Testar executa a suíte local de 97 testes no emulador.
