# Capa da série acompanha a temporada

Para séries de listas Xtream e M3U, a capa principal acompanha a temporada do episódio salvo. Aplica-se ao catálogo normal, Kids, Favoritos, Continuar e às capas principais do catálogo dentro do player.

- Sem episódio salvo: capa geral da série.
- Episódio salvo: capa da temporada correspondente.
- Mudou de temporada ou voltou para uma anterior: acompanha o episódio mais recente.
- Episódio concluído: mantém a temporada dele; não retorna a um registro antigo só porque ainda está incompleto.
- Temporada sem capa válida: usa a capa geral da série.
- Dentro do player, a série em reprodução acompanha imediatamente a temporada do episódio atual.
- Kids e normal usam seus próprios históricos. As imagens públicas podem compartilhar cache, mas a escolha da temporada não é compartilhada entre os dois.

As imagens de temporada vêm dos metadados da obra identificada no TMDB. Não são fotos dos episódios. A identificação continua validada por nome/aliases e ano/IDs quando disponíveis. Se não for possível identificar a série com segurança, fica a imagem padrão.

## Instalar

Compatível com os fontes das revisões `m3u-capas-automaticas` ou `series-capas-tmdb`. Inclui a etapa anterior de capas verificadas; não é necessário aplicar dois ZIPs. O instalador valida a base e guarda os originais antes de alterar arquivos.

Extraia para `C:\Users\flavi\Downloads\series-capas-temporada` e execute no PowerShell:

```powershell
Set-Location "C:\Users\flavi\Downloads\series-capas-temporada"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Abra o catálogo. **Não precisa reimportar nem atualizar a lista.** Usa o histórico já salvo e a integração TMDB existente. As primeiras capas carregam progressivamente com internet. A mudança de temporada reaproveita os metadados da mesma série; não faz uma busca nova por episódio.

O cache de capas desta revisão usa um namespace novo, para incluir as temporadas mesmo se a versão anterior já tinha guardado só a capa geral. Metadados públicos mínimos têm validade de 30 dias dentro da cota existente; os arquivos de imagem mantêm o cache e limites atuais. Se o sistema limpar o cache, será necessário buscar novamente.

## Conferir na TV

1. Abra uma série sem progresso: deve mostrar a capa geral.
2. Reproduza um episódio de outra temporada por alguns segundos e volte ao catálogo: deve mostrar a capa dessa temporada.
3. Confira a mesma série em Favoritos, Continuar e no catálogo do player.
4. Se assistir novamente uma temporada anterior, a capa deve acompanhá-la.
5. Confira o Kids separadamente. Não deve herdar a temporada vista no normal.

Pode haver temporadas com a mesma imagem no TMDB. Nesse caso, a troca de temporada não causa uma diferença visual, embora a escolha corresponda à temporada correta. Uma temporada sem imagem usa a capa geral; se nenhuma das duas estiver disponível, fica a imagem padrão.

## Diagnóstico de capas

Com a TV conectada ao ADB, execute antes de abrir o catálogo:

```powershell
powershell -ExecutionPolicy Bypass -File .\Coletar-Capas.ps1
```

Abra as séries, aguarde, saia e volte ao catálogo. Pressione Enter no PowerShell para salvar `diagnostico-capas-series-*.txt`. O relatório contém somente métricas permitidas de identificação e carregamento, sem títulos, URLs ou credenciais.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Restaura os fontes encontrados antes desta instalação, se não tiverem sido alterados depois. Não apaga os dados da TV. Não há mudança de versão do banco, reprodução ou qualidade do vídeo. O pacote contém fontes e scripts, sem APK ou chaves particulares.
