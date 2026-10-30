# Diagnostico de filmes e imagens

Captura somente tags de navegacao, imagens/metadados e player. Remove URLs HTTP/RTSP/RTMP e campos comuns de credenciais antes de gravar no TXT da Area de Trabalho. Nao altera fontes, APK, dados, lista ou cache. Sem compilacao/instalacao.

Execute Capturar-Filmes.ps1 com TV conectada via ADB. Abra o filme afetado desde a capa, aguarde a tela de detalhes, tente Assistir e espere o erro. Encerre com Ctrl+C e envie o TXT e o titulo do filme.

Validado parser PowerShell e casos de redacao de URLs/Authorization/password/token, preservando status HTTP. ADB e problema real ainda precisam ser avaliados na TV.
