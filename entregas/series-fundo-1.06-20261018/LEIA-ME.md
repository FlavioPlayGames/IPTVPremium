# Séries: fundo nos episódios sem foto — 1.06 / interno 8 / r4

## Alteração solicitada

Cards sem miniatura específica reutilizam o mesmo Bitmap do fundo já carregado na ficha. Não baixam uma cópia por episódio, não aguardam TMDB para mostrar esse fundo e não acessam vídeo. Fotos válidas do episódio (TMDB/provedor) continuam prioritárias. Quando uma referência falha, o card mantém o fundo; quando uma foto válida chega, ela aparece normalmente. Se nem o fundo da ficha estiver disponível, permanece o tratamento visual de carregamento existente.

Removidos os seis arquivos do extrator/leituras de vídeo, a dependência Media3 Inspector e os testes/assets antigos de extração. O player Media3/VLC e a animação aprovada da TV permanecem. O pacote remove somente arquivos com hashes conhecidos, com backup e rollback.

## Carregamento automático

O log fornecido demonstra respostas rápidas rejeitadas ao construir o JSON completo, antes de ler episódios. A recuperação nova é restrita: quando há um único bloco episodes no nível raiz e ele é um objeto/array íntegro no final da resposta, o app pode usá-lo mesmo que outro metadado esteja inválido. A leitura do bloco continua sendo feita pelo parser JSON, preservando IDs, temporadas e números originais. Não reconstrói episódios por regex nem tenta adivinhar aspas no texto.

A recuperação aceita também o caso em que apenas a chave de fechamento externa está ausente e o bloco episodes está completo. Rejeita bloco de episódio cortado/quebrado, candidatos ambíguos, marcador dentro de metadados, campos extras após o bloco e lixo final. Metadados descartados usam as informações da ficha/catálogo e o enriquecimento normal, sem afetar o vínculo dos episódios.

Nos testes, episódios íntegros com sinopse opcional quebrada abrem na primeira requisição, sem clicar em Tentar. Permanecem antecipação no foco, cache local de detalhes por lista/geração e recuperação automática limitada de rede. Se o próprio bloco de episódios estiver inválido ou o servidor estiver indisponível, não há garantia de abertura instantânea; esses casos continuam sendo diagnosticados, sem repetições infinitas.

## Lista fornecida e verificação real

O acesso HTTP/HTTPS à API fornecida foi bloqueado pela rede deste ambiente, inclusive fora do sandbox. Por isso não afirmamos ter reproduzido a resposta privada da lista. As credenciais não estão no código, pacote, GitHub ou relatório.

-TestarLista consulta no emulador a lista ATIVA já cadastrada no aplicativo, lendo o catálogo local e testando até três séries: American Hostage, Volte Sempre e Marília Mendonça, quando presentes. Não baixa o catálogo inteiro, não reproduz vídeo, não altera preferências/lista/histórico e não usa senha gravada no teste. A consulta de episódios é real e sem cache de detalhes, para verificar a primeira abertura. O resultado registra quantidade, chamadas e tempo, sem corpo/URL/credenciais.

## Instalação

Preparar-Series-Fundo-1.06.ps1: patch cumulativo com 25 entradas (16 fontes presentes e nove arquivos antigos removidos), preflight integral, backup, rollback e idempotência. Aceita o src.zip e as entregas r2/r3 conhecidas. Preserva configuração privada, assinatura local, listas, favoritos, progresso, temas e versão 1.06/interno 8. Não publica APK no servidor.

-Verificar não altera nada. -NaoCompilar aplica sem build e é incompatível com testes/captura. Testes são restritos a IDs emulator-N, nunca telefone. Execução padrão compila os cinco APKs.

-TestarSeries executa três regressões locais de carregamento; -TestarApresentacao executa três testes da TV e dos cards (fundo imediato e foto específica que substitui o fundo); -TestarLista executa uma prova na lista cadastrada, com até três séries. Testes são compilados aqui, mas não executados nesta máquina sem emulador.

-CapturarLog também ativa -TestarSeries. Depois dos testes abre o aplicativo e salva diagnostico-series-fundo-logcat.txt na raiz do projeto. Abra uma série com problema e finalize a captura com Ctrl+C. Resultados dos testes ficam em diagnostico-series-emulador.txt, diagnostico-apresentacao-emulador.txt e diagnostico-lista-emulador.txt. Em modo de captura uma suíte que falhar gera aviso e permite continuar o diagnóstico, sem exibir PASS para essa suíte.

## Validação

Compilação assembleDebug + assembleDebugAndroidTest + lintDebug; cinco APKs assinados e minSdk 28. Testes JVM verificam recuperação conservadora/limites, privacidade do diagnóstico, prioridade das fotos, recuperação de rede, cancelamento e compartilhamento de cache. O instalador é testado inclusive para exclusão, rollback e rejeição de arquivos desconhecidos. Os testes da lista real dependem da execução no emulador do usuário.
