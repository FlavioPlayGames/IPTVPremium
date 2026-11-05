# Abertura do player sem reduzir qualidade

Atualização incremental sobre o java.zip enviado em 05/11/2026 (292 arquivos Kotlin). Altera somente AppNavigation.kt. Versão 1.13 mantida.

Nos episódios, retira uma consulta externa TMDB que podia atrasar a abertura em até 5 segundos quando faltava o ID da série. Mantém o ID informado pelo provedor e os dados de título da série, ano, temporada e episódio. A pesquisa existente de legendas continua resolvendo a identidade quando o menu de legendas é aberto, sem disputar a abertura do vídeo.

A URL do episódio passa a ser localizada por consulta direta na geração ativa da lista, em vez de paginar até 5 mil episódios. Mantém a prioridade da URL original importada e o fallback Xtream com ID e extensão reais. Funciona também para episódios além daquele limite. Não altera o banco nem exige reimportar a lista.

Qualidade preservada: nenhum ajuste de resolução, bitrate, seleção de faixas, decodificador, tamanho de buffer, zoom ou imagem. Exo e VLC permanecem iguais aos enviados. O Exo desta versão já usa 1 segundo de buffer inicial; não foi reduzido para evitar piorar a estabilidade. A abertura dos filmes já consulta diretamente o catálogo local e permanece igual: esta correção não promete acelerar uma fonte lenta do provedor. Sem TV/provedor não foi medida a redução real de tempo.

Preserva histórico, Continuar normal/Kids, favoritos, retomada, sinopse, temas e demais recursos. Sem limpeza ou migração nova.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Inclui backup e Desfazer.ps1. Recusa fonte diferente do ZIP enviado e pacote corrompido. Não inclui APK nem configurações privadas.

Conferir na TV: abertura de série com e sem ID TMDB, filme normal e retomado, controle de qualidade/áudio/legendas, episódios de M3U e Xtream, Exo e VLC. Caso filmes ainda demorem, informe o mecanismo usado e o tempo aproximado até a primeira imagem para distinguir inicialização do player e resposta do provedor.
