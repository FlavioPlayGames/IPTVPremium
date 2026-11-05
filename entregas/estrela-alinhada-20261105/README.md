# Alinhamento da estrela da nota

Alinha Nota:, estrela amarela e valor em uma unica linha de texto, compartilhando a mesma linha de base. Antes, tres componentes separados eram centralizados pelas caixas de texto, cujas metricas podem diferir para o simbolo de estrela.

Aplica-se aos cartoes do catalogo dentro do player, em filmes e capas de series, incluindo Kids e Continuar. Mantem a estrela amarela, tamanho do texto, ano acima, temas, foco, gaveta, navegacao e todas as configuracoes de video. Apenas PlaybackCatalogBrowser.kt alterado. Versao 1.13 mantida; nao modifica historico nem a correcao de abertura do player.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Apos BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Backup automatico e Desfazer.ps1 incluidos; arquivos desconhecidos e pacote corrompido sao recusados. Sem APK ou configuracoes privadas.

Validacao visual final deve ser feita na TV: cartoes normais/Kids, selecionados e sem selecao, notas com e sem valor. Nao houve teste em TV ou emulador.
