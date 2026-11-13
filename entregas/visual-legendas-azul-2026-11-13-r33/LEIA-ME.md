# Azul royal e painéis de legendas r33

Base obrigatória: r32 já aplicada no projeto Android C:\Users\flavi\AndroidStudioProjects\IPTVPremium. O aplicador valida todos os hashes antes de escrever, guarda backup fora do projeto e recusa sobrescrever outra versão. Atualizar-TV.ps1 aplica, compila e instala na TV já conectada/autorizada. Mantém o pareamento, assinatura e dados locais; não desinstala ou limpa caches. JAVA_HOME usa o JBR do Android Studio se necessário.

## Alterações conforme as fotos

- Azul royal #0047FF compartilhado pelos destaques da interface e pelas telas de legendas, substituindo os dois azuis divergentes anteriores. As cores dos outros temas permanecem. O fundo metálico e seus filtros existentes foram mantidos; nenhuma nova imagem, blur de fundo ou biblioteca.
- Resumo do filme em um painel arredondado, com título em negrito, tipo do conteúdo, divisor discreto, orientação e botão Conectar para enviar dentro do mesmo painel.
- Três instruções do provedor agrupadas em painel arredondado à esquerda, com divisores sutis e círculos azul royal com números brancos. QR real, status e Gerar novo acesso permanecem à direita.
- Sem marca IPTV Premium no cabeçalho das legendas, sem logo no cabeçalho principal de configurações nem no controle parental. Em Meu aparelho, a versão aparece sem repetir o nome do aplicativo. Os títulos e Voltar permanecem.
- Texto do controle de sincronia ajusta o contraste quando o foco usa o azul mais escuro. Não modifica os cálculos, arquivos de legenda ou regras de persistência da r32.

## Instalação

powershell -ExecutionPolicy Bypass -File .\Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"

Para comparar com a foto enviada, use tema Azul e fundo claro. Os painéis também se adaptam ao fundo escuro. Aplicar.ps1 aplica sem instalar; Desfazer.ps1 -Backup "pasta indicada" -Instalar restaura e instala, recusando sobrescrever edições posteriores.

## Validação

Consulte VALIDACAO.md. A comparação é baseada nas fotos enviadas; câmera, brilho e processamento da TV alteram a cor percebida. Não houve captura/instalação física remota neste ambiente. Nenhuma alteração no site, D1, histórico, cache, limites de armazenamento ou rotatividade faz parte deste pacote.
