# Importação dinâmica, prioridade e capas

Aplicar depois da remoção do Fanart. Mantém a versão configurada e as demais alterações do projeto atualizado enviado.

- Indicador giratório leve na tela de carregamento. Mensagens correspondem à fase real; contagem somente quando o código informa um total. Sem porcentagem inventada ou atraso mínimo artificial.
- Importação de M3U e Xtream ganha prioridade sobre sincronização opcional de histórico e downloads de capas. Consultas de histórico em execução cedem por cancelamento cooperativo; conexões de capas são interrompidas fora da thread da interface. Ao terminar, falhar ou cancelar, a prioridade é liberada, inclusive com importações simultâneas.
- Mantém lotes de importação em 500. Prioridade não aumenta a velocidade do servidor e não altera prioridade de threads do Android.
- Capas visíveis começam a preparação sem a espera extra de 100 ms; até cinco por lote visível, mantendo limite global de seis conexões e de duas antecipações em segundo plano.
- Mantém RAM → disco → rede, cache de 125 por seção, antecipação de cinco em cinco apenas após ociosidade e imagens genéricas compartilhadas somente quando não há capa. Não restaura imagens removidas nem Fanart.

- Home: margem no recorte da engrenagem para não cortar sua ampliação.
- Canais: ESC/Voltar da prévia vai direto às categorias. Sair da tela cheia retorna com categorias abertas.
- Avaliação de filmes e séries: primeira estrela cresce 12% ao receber foco, como o coração. Mantém uma estrela selecionada por vez e sem animações de avaliação.

## Aplicar no PowerShell

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/importacao-dinamica-prioridade-20261023'
$script = Join-Path $env:TEMP 'Preparar-Importacao-Dinamica.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Importacao-Dinamica.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Valida todos os arquivos antes de escrever, rejeita fontes incompatíveis e cria backup fora de app/src. Não inclui chaves, configurações privadas ou testes no aplicativo. Instale o APK gerado na TV. A validação de tempo e navegação no aparelho ainda é necessária.
