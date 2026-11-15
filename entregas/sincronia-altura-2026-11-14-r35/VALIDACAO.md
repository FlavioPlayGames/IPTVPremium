# Validação r35

- Compilação final :app:assembleDesempenho aprovada: Kotlin, R8, lint vital e APKs por arquitetura/universal. Manifesto confere com os fontes finais. A classe compilada da sincronização com o portal contém a preservação da altura local.
- Testes de altura: padrão legado, cópia de estilo sem perder altura, limites e saturação de inteiro, escala em múltiplas resoluções, movimento monotônico, posições não negativas. Cue real do Media3: preservação da posição original no padrão e ancoragem/posição vertical para altura personalizada, mantendo texto e posição horizontal.
- Store real compilado exercitado em um host de preferências em memória: padrão legado, salvar e abrir outra instância, editar outros campos sem perder altura, restaurar só altura, remover observador e limitar valor salvo inválido. Isso não substitui teste de persistência no Android real.
- Regressão do tempo: estados ativado/desativado, sinais, limites, saturação e delegação de Renderer real do Media3, incluindo offset em tempo de execução e relógio/elapsed do vídeo inalterados. Cálculos/armazenamento do ajuste por filme/temporada não foram editados.
- Aplicação, repetição, restauração byte a byte, proteção de base divergente e edições posteriores aprovadas em PowerShell 7 e simulação do formato de arrays do Windows PowerShell 5.1. Parser dos scripts aprovado. Execução Windows/ADB real depende do PC.
- Dez fontes Kotlin alterados/adicionados e nove dependências verificadas pelo aplicador. Todas as imagens/recursos existentes permaneceram byte a byte. Nenhuma imagem, dependência, gradiente de cenário ou alteração de paleta/tema/modo neste pacote.
- Sincronia segue NeutralPlayerPalette e playerThemeAccent existentes; o ajuste manual e o auxiliar usam controles sem caixas individuais. Item de menu sempre visível e desabilitado sem legenda ativa.
- Nova preferência bottom_percent de 0 a 80; ausência em instalação anterior usa 8. Exemplo exibido sobre a área real do vídeo ao ajustar, removido ao sair; não cria player, não busca mídia e não muda timestamps. Foco ao entrar em Subir, setas ↑/↓ e retorno a Estilo; persistência preservada ao receber somente o estilo do site.

Limites: não houve TV/emulador para verificar visualmente ou reproduzir arquivos reais. Confirmar na TV a posição, legibilidade, foco, retorno após fechar e reabrir e cada engine. As pranchas aprovadas são propostas, não capturas do APK.

Altura/estilo de legendas internas do VLC não são expostos pelo adaptador; a ação informa para usar Exo Player. Legendas de texto externas funcionam nos dois players; internas de texto no Exo. Legendas em imagem mantêm seu posicionamento original. O site não ganhou campo de altura; mantém-se a altura local ao receber uma configuração de estilo.

Não inclui correção de capas genéricas nem alterações de importação, memória/cache, credenciais ou código do site. APK de validação não distribuído: instalador compila com configuração/assinatura locais.
