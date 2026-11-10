# IPTV Premium — r4: capas e fila de identificação

Atualização incremental sobre a r3: reconhece `e`/`&` na identificação de capas alternativas de séries, com consulta alternativa limitada e validação de subtítulos/anos/IDs; libera a vaga de metadados antes do download; inclui medição de consulta e coletor ADB ao vivo com filtro estrito. Preserva chaves de cache existentes.

[Baixar continuacao-iptv-premium-r4.zip](https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/continuacao-iptv-premium-2026-11-10-r4/continuacao-iptv-premium-r4.zip)

Salve em Downloads e execute no PowerShell:

```powershell
$projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
$pasta = "$env:USERPROFILE\Downloads\continuacao-iptv-premium-r4"
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

São quatro arquivos Kotlin; o aplicador valida hashes, cria backup e permite desfazer. O instalador está na pasta extraída em Downloads. Compila e instala a versão desempenho usando os arquivos e a assinatura locais, sem desinstalar ou limpar dados. Esta revisão não exige nova importação da lista já atualizada na r3.

Para coletar tempos enquanto navega na TV, execute na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Coletar-Diagnostico.ps1 -AoVivo
```

Ctrl+C encerra. Saída em `Downloads\IPTV-tempos-capas-r4.txt`.

Validação: compileDesempenhoKotlin e lintDesempenho passaram; Lint com zero erros e a mesma contagem de avisos da r3. Passaram testes JVM de identidade/cache/histórico, separação das filas com imagens lentas simuladas e testes de aplicação/reversão/coletor em PowerShell 7 com comportamento de JSON do Windows PowerShell 5.1 simulado. Respostas de metadados e bitmaps nos testes são simulados; a r4 ainda precisa de validação na TV. Não foi medido ganho em segundos no aparelho, nem publicado APK. Detalhes e limitações em LEIA-ME.md e VALIDACAO.md dentro do ZIP.
