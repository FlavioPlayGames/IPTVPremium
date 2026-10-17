# IPTV Premium 1.04 / número interno 6 — refinamento para TV

- Mesmos desenhos, formatos e tamanhos da Home, sem novas imagens ou aumento de brilho.
- Redução das imagens na resolução física, com filtragem de alfa para minimizar serrilhado e preservar detalhes. O dourado e os outros temas passam pelo mesmo caminho.
- Recorte de cada ícone antes da redução, sem misturar pixels dos ícones vizinhos no atlas.
- Atualizar e engrenagem usam a mesma filtragem, com margem para o foco.
- Entrada em Aparência foca a última cor selecionada; Voltar continua acessível pelas setas.
- Trailers de filmes e séries abrem os links cadastrados no YouTube/navegador. Sem link, abre uma busca pelo título e avisa que está buscando.
- Carregamento de séries usa a foto original da sala em todos os temas; só os dois LEDs verticais recebem o tom suave do tema.
- Carregamento de filmes conserva a imagem original de faixas metálicas, recolorida por tema.
- Tempos, movimentos, abertura da TV, zoom e transições das animações preservados.
- Contornos azuis 18% mais escuros; a paleta principal de textos e os outros temas são preservados.

Preserva a variante recente de Minhas Listas com lixeira externa e confirmação de exclusão quando ela já estiver aplicada.
Em projetos sem essa variante, aplica apenas a correção de contornos à tela original, sem introduzir a lixeira.

Executar Preparar-app-1.04-TV.ps1 na raiz Android. Aceita -Projeto caminho e -NaoCompilar.
Cria backup, valida todas as fontes antes de aplicar, prepara APP_VERSION_CODE=6 / APP_VERSION_NAME=1.04 e compila com a assinatura local.
O instalador reconhece arquivos já aplicados e recusa versões desconhecidas; não força substituição de alterações locais.
Não modifica banco, listas, favoritos, progresso, credenciais, assinatura ou Cloudflare.
Não contém APK, configurações privadas ou chaves da nuvem.

Instalar por cima e conferir primeiro na TV 4K de 65 polegadas. O teste real de nitidez/foco/abertura de YouTube ainda é necessário.
Compilação e testes em nuvem não confirmam a resolução real da superfície do Android na TV nem a disponibilidade de aplicativos externos.
Somente depois publicar os novos APKs 1.04/code6 no GitHub e cadastrar seus hashes no painel opcional.
