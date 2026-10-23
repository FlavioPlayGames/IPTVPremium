# Versão 1.12 — menu de canais e retomada

- Define versionName 1.12 e versionCode mínimo 14, respeitando APP_VERSION_CODE se for maior. Não altera assinatura ou applicationId.
- Em tela cheia, o cursor percorre as linhas visíveis sem deslocar a lista. A rolagem só acontece ao ultrapassar a borda, revelando as próximas linhas. A mesma regra vale para a coluna de categorias; mantém a volta circular do último para o primeiro item.
- Ao abrir Canais, restaura automaticamente o último canal confirmado da lista IPTV ativa. A seleção é gravada ao confirmar, inclusive antes de terminar a conexão, e persiste ao fechar o aplicativo. Usa o histórico existente para migrar ou se um canal salvo não existir mais.
- A restauração ocorre uma vez por entrada, sem reiniciar o canal durante atualizações de metadados. A reprodução retoma na prévia e o foco inicial permanece nas categorias.
- Mantém confirmação por OK ou 3 segundos parado no menu em tela cheia, favoritos, busca de transmissão alternativa e os ajustes anteriores de Home e carregamento.

Aplicar sobre canais-voltar-uma-vez-20261023. A alteração posterior da margem da engrenagem é preservada.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/canais-lista-estavel-retomada-20261023'
$script = Join-Path $env:TEMP 'Preparar-Lista-Canais.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Lista-Canais.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Instalador valida fontes antes de escrever e guarda backup fora de app/src. Não adiciona testes ao aplicativo. Conferir no controle físico a rolagem nas bordas e a retomada após sair e voltar, fechar o app e alternar listas. Não houve teste na TV.
