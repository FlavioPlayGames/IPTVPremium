# Retorno ao catálogo, sessões e prioridade dos detalhes

Corrige três comportamentos sobre o último `java.zip` e mantém a atualização anterior dos ícones.

Ao voltar dos detalhes de um filme ou série, remove somente o fade de retorno para a grade e restaura o foco nos primeiros frames. A posição e o último título aberto continuam sendo usados para restaurar o controle, mas o contorno pertence ao foco atual, inclusive durante o deslizamento, e deixa de ficar preso no título clicado. Os pedidos de foco deixam de repetir durante 280–850 ms. A navegação e animação de deslizamento da grade não foram modificadas.

Ao sair para a Home e entrar novamente em Canais, Filmes ou Séries, os ViewModels e seus Pagers pertencem à sessão do aplicativo, em vez de serem descartados junto com a rota. A lista de categorias e as contagens são reaproveitadas. Há atualização quando mudam a playlist, a geração importada ou o acesso parental; erros anteriores podem ser tentados novamente. A troca de playlist cancela consultas antigas de canais. Não mantém novas cópias de bitmaps nem aumenta limites de cache. Uma categoria diferente ainda precisa consultar seus dados; imagens que saíram do cache RAM podem ser decodificadas novamente do disco, e arquivos ausentes/evictados podem precisar de rede.

Ao clicar em um título, novas transferências de capas são suspensas e as que estavam em andamento recebem a interrupção de prioridade, incluindo pedidos dos cards visíveis. As imagens locais continuam acessíveis. A abertura do filme aguarda metadados, fundo e logo, depois até seis fotos iniciais do elenco, com duas consultas de fotos simultâneas e reutilização do carregador existente. O prazo total de preparação passa a 15 segundos, com até 8 segundos para metadados; se a preparação expirar, permanece o carregamento com opção de tentar novamente em vez de navegar com trabalho ainda pendente. A espera mínima e a animação existentes continuam iguais. Uma preparação concluída não dispara outra consulta de metadados ao entrar na ficha. Dados/imagens indisponíveis no provedor continuam usando os fallbacks da interface.

Séries reaproveitam imediatamente episódios já disponíveis no cache válido para a playlist/geração. A janela de espera da arte inicial passa a 15 segundos, incluindo a transição original de 2 segundos; a velocidade da transição não muda. Falhas de rede/imagens não bloqueiam a abertura indefinidamente. O botão Voltar permanece disponível durante o carregamento.

## Aplicação

Execute `Atualizar-Catalogo.ps1 -Projeto CAMINHO_DO_PROJETO` no PowerShell, na raiz do projeto, e depois `.\gradlew.bat assembleDebug`.

O pacote altera dez fontes Kotlin e cria um helper. Confere todos os hashes antes de gravar, aceita LF ou CRLF/BOM, cria backup em `.navigation-backups/catalogo-retorno-prioridade-*`, não sobrescreve uma base diferente e restaura os arquivos caso a gravação falhe. Pode ser reaplicado. Não altera imagens, Gradle ou assinatura.

## Validação

Testes cobrem reentrada sem repetir a carga de metadados; invalidação por playlist, geração e controle parental; recuperação após falha de vinculação; carregamento HTTP real de fundo/logo antes do elenco e ausência de novas requisições na reabertura; ausência de transferências de capas enquanto detalhes têm prioridade; retirada do destaque anterior em Filmes e Séries; retorno com foco e resposta ao controle; espera limitada da arte de séries. O instalador é validado com LF, CRLF/BOM, reaplicação, recusa de fonte modificada e restauração após falha simulada de escrita, incluindo remoção do helper recém-criado.

A cópia local não tem doze recursos de arte presentes no projeto do usuário. Os testes de compilação usam aliases temporários locais para eles, não incluídos no pacote. Os testes da grade usam dados sintéticos, o carregamento usa um servidor HTTP local e a captura usa o Canvas da View no Robolectric. Não foi realizado teste em TV física nem validação visual dos fundos atuais.

Para repetir os testes no projeto completo:

```powershell
.\gradlew.bat -I caminho\testes\validar-retorno.gradle :app:testDebugUnitTest --tests '*CatalogSessionRevisionsTest' --tests '*SelectedDetailsPreloadTest' --tests '*CatalogReturnFocusHostTest' --tests '*SeriesOpeningReadinessTest'
```

O kit não é necessário para aplicar a correção. Limites de disco/RAM, qualidade JPEG, dimensões e lotes de capas, ícones, temas, importação, classificação, favoritos e player são preservados. A retenção tratada aqui é o retorno da Home para as seções durante a sessão; não promete manter a RAM após encerramento do processo.
