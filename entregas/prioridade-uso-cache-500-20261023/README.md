# Prioridade ao uso, capas compartilhadas e lotes de 500

Versão mantida: **1.11 (13)**. Aplicar sobre a entrega `canais-menu-slim-20261023`.

- Abertura e atualização usam uma tela fixa, centralizada e dimensionada, sem arte assíncrona ou pulso de logo. Remove a passagem pela tela intermediária de conclusão antes da Home.
- Histórico não é uma etapa da abertura, da importação ou da troca de lista. Um aviso breve aparece na Home, sem painel sobreposto, informando que será importado em segundo plano.
- Trabalho opcional de histórico só recebe execução após **10 segundos sem interação**, com o app em primeiro plano e sem um conteúdo selecionado tomando prioridade. Qualquer comando, toque ou saída do app revoga essa execução; o mesmo trabalho pendente retoma no próximo intervalo ocioso. A contagem reinicia quando o catálogo libera a Home, para não iniciar junto do primeiro frame depois de uma importação longa.
- Comparação de histórico e consulta de favoritos compartilhados passam fora da thread da interface. Favoritos da própria lista são lidos separadamente, sem esperar pela transferência entre listas.
- Antecipação de capas fora da tela e preparo opcional das capas recentes também aguardam ociosidade. O carregamento visível continua ativo. A interrupção de conexões opcionais acontece fora da thread do controle.
- Trocar de lista cancela o histórico, sincronização e importação anteriores, sem aguardar `join` de limpeza de rede para iniciar o fluxo escolhido. A limpeza de dados temporários continua segura; o catálogo válido anterior não é apagado. Resultado de histórico de uma lista anterior não é publicado na atual.
- **Quatro capas genéricas compartilhadas**: as imagens comprimidas permanecem nos recursos do APK, inclusive após fechar o app. Cada uma é decodificada uma única vez por processo em 256×384; juntas usam aproximadamente **1,5 MiB** de bitmap. São conservadas ao sair/retornar a uma tela e ao reduzir o cache de outras capas. Reiniciar o processo exige somente essa pequena decodificação local, em segundo plano, sem rede.
- Nenhuma consulta extra de categoria no Room nem coroutine de decodificação por filme/série para exibir uma capa genérica. A classificação usa os metadados já presentes no cartão; sem categoria conhecida, usa uma das opções gerais. Enquanto os quatro bitmaps iniciais são preparados, um desenho estático simples mantém o cartão visível sem bloquear o foco.
- Capa real continua tendo prioridade. URLs que falharam têm uma pausa de **5 minutos** antes de nova tentativa, guardada entre aberturas, para não repetir os mesmos downloads e esperas a cada retorno. Essa lista é limitada a 512 URLs em hash; não guarda endereços nem credenciais. Arquivo real já disponível no cache ainda pode ser usado. Falha não gera nova sequência de três tentativas ao navegar.
- **Xtream e M3U: lotes de até 500 itens**, mantendo limites de caracteres para conter memória. Perfis de diagnóstico antigos continuam reconhecidos, mas seus valores passam a 500 nos dois formatos. Não altera formato das listas, bancos, versão ou identidade de canais.

## Aplicar no PowerShell

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/prioridade-uso-cache-500-20261023'
$script = Join-Path $env:TEMP 'Preparar-Prioridade-Uso-500.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Prioridade-Uso-500.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Valida todos os arquivos antes de escrever e cria backup em `.usage-priority-backups`, fora de `app/src`. Alterações locais incompatíveis fazem o instalador interromper sem sobrescrever. `-Testar` compila o APK, sem instalação automática. Depois instale o APK gerado sobre o aplicativo existente.

As verificações são externas ao aplicativo: nenhum teste foi incluído no APK. Resultados em `VERIFICACAO.txt`. A apresentação da abertura e o tempo de resposta na TV física ainda precisam ser conferidos no aparelho; não foram medidos nesta entrega.
