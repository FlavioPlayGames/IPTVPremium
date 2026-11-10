# IPTV Premium — r6: restauração da antecipação antiga

Atualização incremental sobre a r5, conforme a regra validada a partir do java.zip antigo. Restaura em Filmes a reserva inicial de 15 capas adiante, crescendo até 25, em grupos de cinco, com dois trabalhadores e conservação das capas pelo LRU normal de memória. O carregador de imagens e o cálculo da reserva coincidem com os fontes antigos enviados.

Séries Xtream e M3U usam o mesmo plano e a mesma fila, acrescentando a identificação verificada quando faltar uma capa válida do provedor. Mantém as correções de capas/histórico, o cache rotativo e as cotas existentes (100 MB de cache descartável, até 75 MB de imagens e 125 capas por tipo). Não cria um cache adicional ou amplia cotas. Não mede o tamanho completo instalado contra a meta de 400 MB.

A limpeza de código não utilizado fica para a próxima etapa.

[Baixar continuacao-iptv-premium-r6.zip](https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/continuacao-iptv-premium-2026-11-10-r6/continuacao-iptv-premium-r6.zip)

Salve em Downloads e execute no PowerShell:

```powershell
$projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
$pasta = "$env:USERPROFILE\Downloads\continuacao-iptv-premium-r6"
Unblock-File -LiteralPath "$pasta.zip"
Expand-Archive -LiteralPath "$pasta.zip" -DestinationPath $pasta -Force
Set-Location $pasta
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "$projeto"
if ($LASTEXITCODE -eq 0) {
    powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "$projeto"
} else {
    throw "A aplicacao falhou. Confira a mensagem antes de compilar/instalar."
}
```

São seis arquivos Kotlin. O aplicador confere hashes antes de copiar e cria backup. O instalador está na pasta extraída em Downloads; compila com os arquivos e assinatura locais e instala com adb install -r, sem desinstalar ou limpar dados. Não precisa reimportar a lista.

Para registrar a navegação na TV, na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Coletar-Diagnostico.ps1 -AoVivo
```

Ctrl+C encerra. Saída em Downloads\IPTV-tempos-capas-r6.txt. O filtro exporta apenas contadores, tempos e IDs públicos. Consulte LEIA-ME.md e VALIDACAO.md no ZIP para escopo, testes e limitações. Os testes de rede/bitmap usam adaptadores simulados; não foi publicado APK nem medido ganho na TV.

Validação: compileDesempenhoKotlin e lintDesempenho passaram, sem erros de Lint. Passaram os testes JVM de reserva/grupos/cache/histórico/rotação e os testes de aplicação/reversão/coleta em PowerShell 7, com comportamento de arrays JSON do Windows PowerShell 5.1 também simulado. Conferência de navegação/imagens no aparelho ainda necessária.
