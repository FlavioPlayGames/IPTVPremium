# IPTV Premium — r5: janela móvel de até 30 capas

Atualização incremental sobre a r4 para Filmes e Séries, Xtream e M3U, normal e Kids. Inclui categorias, Favoritos, pesquisa e Continuar. Prioriza os visíveis e até 15 adiante/5 atrás, inverte a direção ao subir e prepara os cinco próximos. Cancela trabalho obsoleto ao navegar rápido e retoma na posição final. A antecipação recebe os dados de identidade necessários às séries M3U sem capa válida do provedor.

As cotas e a rotação existentes foram preservadas: cache descartável de 100 MB, até 75 MB para imagens e LRU de 125 capas por tipo. A janela ativa de 30 não cria um cache adicional nem amplia os limites; os arquivos antigos continuam sujeitos à limpeza rotativa existente. A meta de 400 MB para o aplicativo completo depende também de recursos, bancos e dados persistentes; não foi medida remotamente nesta revisão.

[Baixar continuacao-iptv-premium-r5.zip](https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/continuacao-iptv-premium-2026-11-10-r5/continuacao-iptv-premium-r5.zip)

Salve em Downloads e execute no PowerShell:

```powershell
$projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
$pasta = "$env:USERPROFILE\Downloads\continuacao-iptv-premium-r5"
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

O aplicador verifica hashes e cria backup para oito arquivos Kotlin, sendo um novo. O instalador está na pasta extraída em Downloads e usa a compilação e a assinatura locais com `adb install -r`. Não desinstala nem limpa dados. Não precisa reimportar a lista para ativar a regra.

Para medir a janela durante a navegação na TV, execute na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Coletar-Diagnostico.ps1 -AoVivo
```

Ctrl+C encerra. O resultado fica em Downloads\IPTV-tempos-capas-r5.txt. O coletor exporta somente contadores, tempos e IDs públicos em formatos permitidos, sem URLs ou credenciais. Consulte LEIA-ME.md e VALIDACAO.md dentro do ZIP para os detalhes da janela, testes e limitações. Os testes portáveis usam respostas/bitmaps simulados; a velocidade e aparência na TV exigem conferência no aparelho. Não foi publicado APK.

Validação final: compileDesempenhoKotlin e lintDesempenho passaram, zero erros de Lint e mesma contagem de avisos da r4. Passaram os testes JVM de janela/fila/cache/armazenamento/histórico e os testes de aplicação/reversão/coleta em PowerShell 7, com comportamento JSON do Windows PowerShell 5.1 também simulado.
