# IPTV Premium — r7: pesquisa geral e limpeza em um toque

Atualização incremental sobre a r6. Em Filmes ou Séries, pesquisa em todas as categorias permitidas da lista ativa, tanto Xtream quanto M3U e nos modos normal/Kids. Durante a pesquisa, ignora o filtro de categoria/Favoritos; ao limpar, retoma a seleção guardada. Pesquisar a partir de Continuar também usa o catálogo completo daquele tipo de conteúdo.

Adiciona Limpar na barra de pesquisa e LIMPAR no teclado virtual dos catálogos. Apaga somente o texto, zera o cursor e permite pesquisar novamente; preserva histórico, favoritos, listas e caches. Escolher explicitamente uma categoria encerra a pesquisa.

A reserva de capas da r6, as cotas e a rotação de armazenamento não foram modificadas. A busca continua local no SQLite com a pausa de 350 ms; não faz uma nova busca de catálogo no servidor do provedor. Os filtros de lista/perfil/controle parental do repositório permanecem no caminho existente.

[Baixar continuacao-iptv-premium-r7.zip](https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/continuacao-iptv-premium-2026-11-10-r7/continuacao-iptv-premium-r7.zip)

Salve em Downloads e execute no PowerShell:

```powershell
$projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
$pasta = "$env:USERPROFILE\Downloads\continuacao-iptv-premium-r7"
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

São cinco arquivos Kotlin modificados e um auxiliar novo. O aplicador confere hashes e cria backup para desfazer. O instalador fica na pasta extraída em Downloads, compila com os arquivos e assinatura locais e instala com adb install -r. Não precisa reimportar a lista nem limpar o cache.

Após instalar, teste um título de outra categoria e use os dois atalhos de limpeza. Confira também Favoritos, Continuar e Kids. Veja LEIA-ME.md e VALIDACAO.md no ZIP para testes e limitações. Aparência e foco com o controle remoto exigem conferência na TV; não foi publicado APK ou feita instalação remota.

Validação: compilação final e Lint passaram, sem erros e com a mesma contagem de avisos da r6. Passaram testes dos fluxos reais de opções de pesquisa, dos handlers de limpeza extraídos, regressões de histórico/capas/reserva/rotação e aplicação/reversão/coletor em PowerShell 7, com comportamento JSON do Windows PowerShell 5.1 também simulado.
