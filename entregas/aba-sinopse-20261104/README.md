# Aba externa de sinopse e Voltar por níveis

Aplica a opção 1 aprovada: uma pequena aba Sinopse › junto à borda externa do catálogo, alinhada à capa selecionada. A lista permanece compacta. A aba aparece somente nas capas de filmes e séries, incluindo Favoritos e Continuar; nunca em temporadas ou episódios.

- → na capa ou clique na aba abre a sinopse completa em painel translúcido lateral. ↑/↓ rolam textos longos. Não há corte em cinco linhas.
- Voltar/← dentro da sinopse fecha somente a leitura e devolve o foco à mesma capa.
- Voltar na lista de episódios retorna às temporadas; nas temporadas retorna às capas da série; nas capas retorna às categorias; nas categorias fecha o catálogo. O botão da tela e o controle seguem a mesma regra. Repetição da mesma tecla não atravessa vários níveis.
- OK continua selecionando/reproduzindo; segurar 2s continua favoritando. Favoritos individuais de episódios não ganham aba de sinopse.

Consulta os detalhes somente ao abrir a leitura, reutilizando os carregadores/cache da ficha e da série. Sem varredura de catálogo. Sinopse ausente/falha mostra Sinopse indisponível. Cache limitado à abertura do catálogo/playlist/perfil, com no máximo 48 resultados.

Incremental sobre sinopse-player. Quatro fontes Kotlin alterados. Não altera manifesto, regras de histórico/favoritos ou a pausa/retomada de vídeos.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Há backup e Desfazer.ps1, com proteção de edições posteriores. O instalador recusa fontes desconhecidos. Sem APK ou configurações privadas.

Validar na TV: aba acompanhando seleção/rolagem, leitura completa e retorno à mesma capa, episódio → temporada → capa → categorias, uma tecla por nível, favoritos de 2s e seleção de reprodução.
