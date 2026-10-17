# Ativação e recuperação de capas — 1.05 / número interno 7

Complemento ao pacote de ativação 1.05. Mantém a mesma versão e assinatura: instalar por cima para testar, sem desinstalar ou limpar dados. Não publica APK nem altera a central. A integração de ativação da central do pacote anterior continua necessária.

Duas telas independentes e centralizadas: boas-vindas com logo, validade real dd/MM/yyyy e botões largos; ativação com logo, título, QR real, site, MAC/código e Copiar, Abrir site e Voltar. Na TV não divide mais QR e dados em colunas. O painel translúcido exibe o fundo original de cada tema, com contorno metálico. Em telas menores há rolagem. Conteúdo, textos e botões não recebem desfoque. Os valores do exemplo não são usados.

Capas: recria o diretório se a limpeza o removeu durante a mesma sessão. Revalida também antes de gravar. Filmes e séries fazem até três tentativas com pausas de1s/3s quando a capa visível falha; sair do cartão cancela as repetições. Cache RAM/disco e limite existente preservados. Servidor sem imagem, URL inválida ou indisponível ainda pode deixar capa vazia. Não baixa o catálogo inteiro.

PowerShell verifica todos os fontes e os hashes antes de aplicar, faz backup, aceita repetição e recusa versões locais diferentes. Nenhum dado de listas/favoritos/histórico é alterado.

Validação: compilação Android e testes JVM de recriação de diretório e repetição/cancelamento. Testes de instalação em pasta temporária. Aparência e recuperação real após limpar cache ainda precisam de verificação na TV/celular.
