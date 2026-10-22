# Home: artes harmonizadas e atualização da lista ativa

Base: última versão publicada com `home-branco-cinza` e `catalogo-retorno-prioridade` aplicados.

Os cinco temas recebem novas artes dos cinco ícones, com painéis em pedra clara levemente quente no modo claro e grafite no escuro. Os tons se aproximam dos fundos enviados, preservando diferença de material e contraste. Os recortes foram refeitos; a geometria da Home, base dos ícones, nomes, áreas de clique, reflexos e seleção por aumento continuam na mesma estrutura. As prévias em `previas/` foram renderizadas pelo componente real da Home com os dois fundos enviados.

A leitura deixa de reduzir as faixas dos ícones para metade da largura/altura. Cada faixa é decodificada em 2172 × 724 e compartilhada pelos cinco ícones: cerca de 6 MB por faixa ARGB, em vez de aproximadamente 1,5 MB. O cache de arte continua limitado a 18 MB e pode retirar outros temas antigos para acomodar a imagem atual. Não existe novo cache e não há alteração de resolução/qualidade das capas do catálogo. Fundos e logo mantêm suas regras de leitura. Os fundos do projeto não são substituídos por este pacote.

## Botão Atualizar

O botão passa o ID da lista ativa ao fluxo existente de limpeza e reinício. Após limpar o cache temporário, registra uma solicitação durável para essa lista. Na reabertura, o aplicativo consulta o provedor pelo mesmo sincronizador M3U/Xtream já utilizado na importação, mesmo que haja catálogo local. As outras listas não são sincronizadas por esse pedido.

O catálogo anterior, configurações, favoritos e histórico permanecem salvos. O novo catálogo só é ativado depois de uma importação válida. A solicitação só termina depois da preparação da Home; a geração já importada é registrada para não baixar novamente se houver interrupção entre importação e abertura. Falhas deixam a solicitação pendente para uma tentativa posterior. Sem uma lista ativa, o botão apenas executa a limpeza/reabertura existentes.

Abrir o aplicativo normalmente ou voltar da Home para uma seção continua aproveitando o cache existente. A atualização forçada acima é solicitada pelo botão Atualizar. Não modifica lotes, formatos ou regras de importação.

## Aplicação

Na raiz do projeto, execute `Atualizar-Artes.ps1 -Projeto CAMINHO_DO_PROJETO` no PowerShell, depois `.\gradlew.bat assembleDebug`.

O instalador inclui quatro fontes Kotlin substituídas, um helper novo e dez PNGs. Confere hashes antes de escrever, aceita LF ou CRLF/BOM, cria backup em `.navigation-backups/home-icones-harmonizados-*`, recusa sobrescrever uma base diferente e restaura imagens/fontes em caso de falha na gravação. A reaplicação não duplica alterações. Gradle e assinatura não são alterados.

## Validação

Dez testes passaram: mapeamento dos temas/modos e limites dos recortes; PNGs decodificados em resolução completa e com tons claros/escuros apropriados; compartilhamento de bitmap; navegação e aumento dos utilitários sem mudança de cor; renderização clara/escura com os fundos fornecidos; solicitação de atualização restrita à lista ativa e persistente entre instâncias; importação concluída sem repetição; limpeza preservando catálogo/histórico/solicitação; ID correto no Intent do botão.

O instalador foi verificado com LF, CRLF/BOM, imagens reais, reaplicação, fonte/imagem modificada e restauração após falha simulada de escrita. As correções de retorno/foco/prioridade anteriores permanecem; a alteração adicional em `AppNavigation.kt` trata somente da atualização manual.

A compilação de testes utiliza aliases locais para dez recursos não enviados da cópia completa do projeto; esses aliases não integram o instalador. As prévias usam os PNGs reais e os fundos do ZIP enviado. Não foi feita validação em TV física nem sincronização com as credenciais reais do provedor.

Para repetir no projeto completo:

```powershell
.\gradlew.bat -I caminho\testes\validar-artes.gradle :app:testDebugUnitTest --tests '*HomeFinishedIconsTest' --tests '*HomeUtilityZoomTest' --tests '*HomeRealArtworkPreviewTest' --tests '*ActivePlaylistRefreshTest'
```
