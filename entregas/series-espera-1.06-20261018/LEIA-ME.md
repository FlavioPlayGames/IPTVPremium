# Abertura de séries: espera de artes — 1.06 / interno 8 / r5

A chave recolocada pelo usuário restaurou o título gráfico e as artes. Esta entrega ajusta somente a espera de entrada; não muda identificação, busca TMDB, chave, metadados ou prioridade de imagens da entrega anterior.

## Regra de tempo

A abertura original tem uma transição de dois segundos, preservada. Enquanto episódios reais e a sala do tema ficam prontos, a página pode preparar título gráfico, fundo e fotos dos cards visíveis por baixo da animação. A janela total para as artes é de dois a cinco segundos desde o início da entrada: imagens prontas liberam a transição original; imagens pendentes recebem apenas o tempo restante até iniciar essa transição aos três segundos. Assim não são cinco segundos extras somados à animação.

Se a janela se esgotar, a página abre com o fundo compartilhado da série nos cards ainda sem foto. Fotos válidas continuam prioritárias e podem substituir o fundo quando chegam. Não aguarda todos os episódios fora da tela e não acessa vídeos/extrator. O retorno do player permanece sem repetir a entrada.

O prazo é para artes opcionais. Uma consulta de episódios ou a preparação essencial da sala que ultrapasse esse período continua obedecendo à regra de dados reais; o app não inventa episódios para cumprir um prazo visual. Se dados essenciais já estiverem lentos, não acrescenta outra janela de espera pelas imagens. Voltar cancela a entrada; erro essencial interrompe a espera.

## Implementação

A página principal informa quando metadados/imagens acabaram de ser tentados: logos, fundo e fotos dos cards efetivamente visíveis. A prévia duplicada dentro da TV não sobrescreve esse estado. Referências novas só são consideradas concluídas pelo carregamento dessa mesma lista de URLs; o cancelamento de uma imagem antiga não marca outra como concluída. A navegação entre capas só acompanha mudanças nos IDs visíveis, evitando observar cada pixel de rolagem.

O limite usa relógio monotônico. As durações originais de fade, marco de 1.580 ms e zoom de 420 ms permanecem. Agendamento/renderização no aparelho podem introduzir pequena variação de quadros; não se trata de um prazo de rede/tempo real rígido.

## Aplicação

Preparar-Series-Espera-1.06.ps1: atualização cumulativa com 26 entradas (17 arquivos e nove exclusões antigas já solicitadas). Na base r4 já aplicada, altera somente AppNavigation.kt, SeriesDetailsScreen.kt, SeriesLoadingOverlay.kt e acrescenta SeriesOpeningTiming.kt. Os demais arquivos são reconhecidos como já aplicados. Não modifica preferências, chave de API, fundos dos temas, assinatura, favoritos ou progresso.

Mantém versão 1.06/interno 8. Não publica atualização no servidor. Verificação integral, backup, rollback, idempotência e rejeição de arquivos desconhecidos permanecem.

-Verificar confere sem alterações; -NaoCompilar aplica sem compilar. -TestarApresentacao compila e instala por cima exclusivamente no emulador e executa três provas de apresentação já existentes. TestarLista e CapturarLog continuam opcionais, como na entrega anterior. A execução normal apenas prepara cinco APKs.

## Validação

Oito testes JVM de janela de tempo: imagens em cache, conclusão antecipada, limite de cinco segundos incluindo a animação, tempo já gasto no carregamento, ausência de nova espera para dados essenciais lentos, erro e cancelamento. Compilação assembleDebug + assembleDebugAndroidTest + lintDebug; verificação de assinatura/versão/Android 9; testes do instalador, incluindo exclusão e rollback.

Os testes Android foram compilados, mas não executados aqui por falta de emulador. O teste real de episódios enviado pelo usuário passou: American Hostage 1.053 ms/6 episódios, Volte Sempre 568 ms/17 episódios e Marília 555 ms/4 episódios, cada um com uma chamada. O aviso de outra suíte não continha o erro detalhado no texto enviado; a entrega não atribui causa a esse aviso.
