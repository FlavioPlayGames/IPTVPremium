# Sinopse no catálogo do player

Mantém a lista lateral atual. Somente o bloco do filme com foco se expande, mostrando a sinopse abaixo da capa e do título. Os outros filmes continuam compactos; o vídeo continua visível. Mesma cor, transparência e seleção anteriores.

Aplica-se às categorias, Todos, Favoritos e Continuar de filmes. Séries, temporadas e episódios não mudam. A sinopse utiliza os detalhes e o cache já usados pela ficha do filme. Consulta apenas o filme focado após 300ms, fora da interface, com limite de espera de 8s; sem busca de toda a lista. Ao mudar de foco, a descrição anterior é escondida e o carregamento cancelado. Mantém até 48 resultados por abertura do catálogo/playlist/perfil. Texto limitado a cinco linhas; ausência ou falha mostra Sinopse indisponível. Não abre a ficha nem inicia reprodução para consultar.

Correção incremental sobre a versão atual com histórico e retomada de sessão. Não altera PlaybackScreen, manifesto, pausa/retomada, favoritos, regras do histórico ou ajuste automático. Dois fontes Kotlin alterados.

Extraia e execute dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Instalador valida os fontes, cria backup e fornece Desfazer.ps1. Não sobrescreve versões desconhecidas. Sem APK ou configurações privadas.

Conferir na TV: foco expandindo apenas um filme, rolagem da linha expandida, texto longo/ausente, troca rápida de foco, Favoritos e Continuar, pressão de 2s para favoritar e reprodução ao confirmar. A imagem aprovada é uma proposta visual, não captura desta compilação.
