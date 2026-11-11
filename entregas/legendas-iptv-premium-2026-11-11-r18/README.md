# IPTV Premium r18 — redirecionamento da pesquisa OpenSubtitles

## Diagnóstico confirmado na TV

A coleta r17 contém três consultas de legendas com HTTP 301, em 157, 346 e 331 ms. O transporte recebia o redirecionamento, mas o cliente recusava qualquer resposta 3xx autenticada e exibia “OpenSubtitles indisponível”. O log não contém o destino do redirecionamento nem comprova a existência de legendas para o filme pesquisado. Não mostra erro de cota ou senha nessas três consultas.

A mesma coleta registra 93 transferências bem-sucedidas de endereços classificados como provedor e uma do CDN TMDB. As 48 transferências visíveis bem-sucedidas tiveram média 129 ms, mediana 118 ms e máximo 338 ms. As 52 tentativas bem-sucedidas da fonte da lista, incluindo preparo, tiveram média 168 ms e máximo 373 ms. Não houve 502 ou timeout nesta amostra. Duas antecipações foram interrompidas por prioridade, comportamento esperado. São amostras diferentes da r16, sem controle dos mesmos filmes/servidores; não é possível atribuir toda a diferença à atualização.

## Correção

Somente buscas GET do endpoint de legendas podem seguir 301, 302, 307 ou 308, com no máximo três requisições no total. O endereço deve continuar HTTPS, no mesmo host oficial original, sem credenciais na URL, porta diferente de 443 ou fragmento. O caminho permanece `/api/v1/subtitles` ou sua variante com barra final.

Todos os parâmetros da consulta são preservados: pequenas diferenças de codificação e ordem são aceitas; troca de ID, idiomas, ano ou qualquer outro parâmetro é recusada. Se o redirecionamento omitir a query, os parâmetros originais são mantidos. Ciclos e destinos fora dessas regras são bloqueados antes de qualquer envio da chave/token ao destino.

Login e download POST não são repetidos por redirecionamento. O fluxo de download continua com um único pedido que consome cota, somente após a escolha do usuário e falta do arquivo no cache. Redirecionamentos de arquivos públicos continuam sem chave ou token, conforme o comportamento anterior.

O coletor acrescenta `stage=redirect status=FOLLOWED|BLOCKED|LIMIT hop=...`, sem exportar destinos, parâmetros, token ou chave. `BLOCKED` pode indicar uma mudança real do serviço fora das regras aceitas; isso exigirá nova avaliação, não seguir qualquer URL automaticamente.

A entrega altera dois fontes sobre a r17 e o coletor. Capas, cache/rotatividade, armazenamento, histórico, Kids, lote r15 de 2.000, versão, assinatura e configurações privadas permanecem. Não modifica o site ou segredos Cloudflare. Não contém APK/credenciais. A conexão salva é preservada; sessão expirada exige reconectar pelo portal.

## Instalar e testar

Extraia em `Downloads\legendas-iptv-premium-r18`. No PowerShell do Android Studio, com a TV autorizada:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\legendas-iptv-premium-r18\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O instalador valida os hashes da r17, guarda recuperação externa, compila usando sua chave privada local e instala sem desinstalar/limpar dados.

Saia do filme, abra novamente e entre no menu **Legendas**, não apenas em Informações técnicas. Aguarde a pesquisa. Teste Resident Evil e uma obra que tenha legendas conhecidas no serviço. Se aparecerem opções, escolha uma para validar download e texto; não faça downloads repetidos para gerar logs.

Se falhar ou para registrar o teste:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\legendas-iptv-premium-r18\Coletar-Diagnostico.ps1" -AoVivo
```

Deixe rodando, saia/reabra o filme e pesquise pelo menu Legendas, depois encerre com Ctrl+C. Envie `Downloads\IPTV-capas-legendas-r18.txt`. A pesquisa só acontece uma vez por abertura do player e não é disparada pelo painel de informações técnicas. Não limpa dados ou logs da TV.

Esperado para redirecionamento compatível: HTTP 301 → `FOLLOWED` → HTTP 200 → contadores de resultados recebidos/aceitos. HTTP 200 com zero resultados pode ser ausência real de legendas ou uma identificação que precise de avaliação; corrigir o redirecionamento não garante resultados para todo filme.

## Restaurar

Use `Desfazer.ps1 -Backup "PASTA_DA_RESTAURACAO" -Instalar`, com o caminho impresso ao aplicar. Restaura somente os dois fontes desta etapa, mantendo a r17 de capas, conta e configuração privada.
