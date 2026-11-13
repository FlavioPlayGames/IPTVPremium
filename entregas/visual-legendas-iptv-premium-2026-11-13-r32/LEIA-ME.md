# Visual de legendas r32 — Android TV

Projeto: C:\Users\flavi\AndroidStudioProjects\IPTVPremium. O site oficial em Site IPTV permanece como está.

Atualizar-TV.ps1 aplica fontes/arte com hashes e backup externo, compila a variante desempenho com a configuração/assinatura locais e instala na TV já autorizada no ADB. Não muda o pareamento, não desinstala, não limpa dados/cache da TV, não publica no Cloudflare. JAVA_HOME usa o JBR do Android Studio se necessário. Se houver mais de um aparelho, informe -Serial.

## Conteúdo

- Fundo estático existente e os cinco temas; início em três linhas, três cartões SubDL/SubSource/OpenSubtitles, cabeçalho e Voltar consistentes.
- Ícones vetoriais originais, fontes SVG em referencias/icones, usados via Canvas sem downloads/fontes externas. Arte pequena de prévia offline (4.648 bytes).
- Conectar provedor com QR real da TV e instruções numeradas; preserva cadastro oficial e edição local. SubSource também pode conectar pelo portal.
- Envio manual: filme/série, busca paginada/filtros parentais existentes, miniaturas já em memória, resumo ao focar e seleção de temporada/modo/episódio lado a lado. Confirmação dos arquivos antes de salvar, com nomes originais e retorno real à TV/site. Informação atualizada: ZIP 2 MiB e SRT 1 MiB.
- Aparência em linhas com valores à esquerda e prévia à direita, preservando todas as opções e preferências atuais. Escolher uma linha abre as opções daquele campo.
- Sincronia sempre visível e fixa no rodapé dos menus de idiomas/versões; desabilitada sem legenda ativa. Legendas externas/manuais usam o relógio existente; faixas internas usam o ajuste nativo do VLC ou um wrapper apenas no renderizador de texto Media3, sem alterar o relógio do áudio/vídeo. A troca de offset de uma faixa interna no ExoPlayer reposiciona no instante atual para atualizar o decoder.
- Painel baixo e centralizado: Ajuste (altera intervalo por clique), valor atual, − Adiantar, + Atrasar, Zerar, Auxílio e Fechar. Auxílio continua apenas para arquivos externos/manuais com horários e vídeo que permita voltar ao início. Arredonda ao segundo mais próximo sem descontar reação. Ajuste salvo por filme/temporada; cache rotativo não apaga esses valores.

## Instalar

powershell -ExecutionPolicy Bypass -File .\Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"

Use o PowerShell do Android Studio se a TV já estiver conectada nele. Aplicar.ps1 aplica sem instalar. Desfazer.ps1 -Backup "pasta indicada" -Instalar restaura e instala, recusando sobrescrever edições posteriores. Nenhum backup antigo é aplicado automaticamente; se a base divergir, envie a mensagem e os fontes atuais.

## Conferir na TV

Confira início, três provedores, QR, busca, temporada/episódio, conferir/salvar e aparência. No player, confirme Sincronia visível mas desabilitada sem legenda e habilitada após seleção. Confira ±, Zerar, Auxílio, retorno ao vídeo e persistência ao trocar episódio da mesma temporada. Teste ExoPlayer e VLC. QR é real/temporário: não compartilhe enquanto estiver válido.

Compilação e testes não comprovam igualdade pixel a pixel com a imagem gerada. Esta entrega implementa a referência com componentes funcionais; ainda é necessário comparar capturas da TV. A demonstração usa dourado, mas o tema selecionado continua sendo respeitado. Nenhum teste físico ou instalação remota foi realizado aqui.

Não inclui APK pronto com as chaves locais: o instalador compila no PC, preservando a configuração existente. Nenhuma nova biblioteca de produção ou alteração de Gradle é necessária. Limites de cache/armazenamento do aplicativo e rotatividade permanecem.
