# IPTV Premium — revisão r3: capas e Continuar

[Baixar continuacao-iptv-premium-r3.zip](./continuacao-iptv-premium-r3.zip?raw=true)

Base: fontes com a primeira entrega/r2 aplicada. Esta revisão modifica 12 arquivos Kotlin; mantém versão/assinatura configuradas no projeto e inclui aplicador, reversão, instalador -Projeto e diagnóstico filtrado.

## Correções

- Capas M3U: rejeita logos variáveis de episódios, placeholders compartilhados com filmes e paisagens. Mantém capas estáveis do provedor e consulta posters verificados apenas para cards visíveis com capa ausente/rejeitada, com cache, prioridade ao vídeo/importação e até duas consultas simultâneas. Nenhuma imagem arbitrária de episódio é escolhida como alternativa.
- Continuar nos dois formatos: progresso local não depende de vínculos já concluídos; o catálogo ainda valida presença e identidade. Metadados confirmados no checkpoint completam identidades antigas sem sobrescrever IDs contraditórios. O limite de 400 registros foi removido, com compactação indexada preservando a regra de identidade.
- Capa da temporada: separa a identidade usada na arte dos metadados exibidos, preservando os mesmos campos nulos da preparação na reprodução.
- Atualizar da Home: aguarda gravações pendentes antes do reinício e abre a Home explicitamente. Não reimporta listas.

A versão anterior podia selecionar outra foto de episódio como capa. Aqui a importação rejeita essa associação; o poster correto depende de identificação segura e disponibilidade do TMDB. Se isso falhar, a capa genérica permanece. Registros que já tenham sido descartados pelo limite antigo não são inventados/recriados automaticamente.

## Validação

Compilação Kotlin/Java e Lint desempenho passaram: zero erros, 178 avisos e seis sugestões. Cinco regressões reproduzidas no SharedLibraryStore real da r2 passaram na r3. Testes cobriram 450 fatos preservados, 2.200 fatos na comparação de compactação, perfis, sucessor de episódio, identidade de arte, cache em disco e limites de consultas. A lista completa preservou os 329.991 itens e os arquivos de episódios ficaram byte a byte iguais; só campos de capa de séries mudaram.

Rede/bitmaps, Android SharedPreferences/DAO e TV são etapas distintas: os adaptadores JVM são descritos no relatório do ZIP. A API real TMDB foi bloqueada no CONNECT do proxy e a validação física na TV continua pendente. Não houve APK assinado remotamente, promoção para oficial ou publicação do site.

## Baixar, aplicar e instalar

Execute no PowerShell; o instalador está na pasta extraída em Downloads:

```powershell
& {
    $ErrorActionPreference = "Stop"
    $projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
    $pasta = "$env:USERPROFILE\Downloads\continuacao-iptv-premium-r3"
    $zip = "$pasta.zip"
    Invoke-WebRequest -UseBasicParsing -Uri "https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/continuacao-iptv-premium-2026-11-10-r3/continuacao-iptv-premium-r3.zip" -OutFile $zip
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne "71145cf37be13ca7274cb455d2527e964246f7ec716a489ccc6b14d966346d65") { throw "ZIP diferente do publicado." }
    Unblock-File -LiteralPath $zip
    Expand-Archive -LiteralPath $zip -DestinationPath $pasta -Force
    Set-Location $pasta
    powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "$projeto"
    if ($LASTEXITCODE -ne 0) { throw "A aplicacao falhou. Confira a mensagem." }
    powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "$projeto"
}
```

O aplicador confere a base e cria backup. O instalador compila com sua assinatura local, identifica a TV e instala o APK por ABI com adb install -r, sem desinstalar ou limpar dados.

## Na TV

**M3U: Minhas Listas → selecionar a lista → Atualizar lista.** O botão Atualizar da Home apenas limpa caches e não regrava as capas importadas.

Depois confira os cards visíveis e teste um filme e uma série parados antes do fim, nos dois formatos e no mesmo perfil em que foram assistidos. Se faltar algum item/capa, execute na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Coletar-Diagnostico.ps1
```

O arquivo em Downloads/IPTV-diagnostico.txt contém apenas contagens e estados públicos de API/arte filtrados. Não contém logs completos, títulos assistidos, URLs de reprodução, senhas, chaves ou tokens. As instruções completas, limitações e testes reproduzíveis estão no ZIP.
