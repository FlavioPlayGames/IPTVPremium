# IPTV Premium r17 — capas com falha e diagnóstico OpenSubtitles

## Resultado da análise da TV

O arquivo `IPTV-capas-r16.txt` contém quatro respostas HTTP 502 e três timeouts de imagens. As quatro conclusões de fallback TMDB levaram 8.238–8.915 ms (média 8.574 ms). As três transferências de imagem bem-sucedidas registradas levaram 246–817 ms. As filas de downloads ficaram entre 0 e 53 ms; as filas de metadados registradas foram zero. Esses eventos não têm correlação individual suficiente para atribuir cada 502 ao provedor ou ao TMDB, nem identificar o filme. Não demonstram um gargalo geral da fila, de decodificação ou de memória.

A mensagem técnica “OpenSubtitles indisponível” informa falha de consulta/serviço, mesmo quando o menu mostra ausência de resultados. O log recebido não contém chamadas desse provedor; login bem-sucedido não comprova que a pesquisa foi autorizada.

## Alterações

- A capa fornecida pela lista continua sendo a primeira tentativa em filmes/séries, Xtream/M3U. Cache local continua antes da rede. O carregamento remoto dessa fonte ganha prazo nominal de 1,5 segundo por URL, com cancelamento da conexão; não repete imediatamente a mesma tentativa antes da alternativa verificada. Limites de conexão/leitura de fontes externas passam a dois segundos por etapa; o CDN oficial TMDB mantém os limites anteriores. A duração real do cancelamento depende da implementação de rede do Android.
- A mesma URL com falha recebe pausa de 20 segundos em RAM, limitada a 512 registros; sucesso libera a pausa. Não bloqueia todo o servidor, não cria arquivos de falha permanentes e não aumenta cache. Cache existente ainda pode ser usado durante a pausa. Os downloads antecipados permanecem sob as mesmas cotas e filas.
- O fallback continua usando identificação verificada no TMDB. Uma capa lenta pode ser substituída por essa alternativa; não se promete que toda capa virá da lista ou aparecerá em 1,5 segundo. Esse prazo se refere à tentativa da fonte, não ao processo inteiro.
- SubDL e OpenSubtitles começam suas consultas independentemente. O menu ainda aguarda o conjunto de resultados. A busca OpenSubtitles por ID TMDB não exige consulta adicional de nome inglês nem combina o ID com um ano potencialmente incorreto do provedor. Busca apenas por nome continua exigindo ano coerente; outro ID, filme, episódio ou idioma continua rejeitado. Isso não comprova a existência de uma legenda para Resident Evil (2026).
- Erro HTTP 403 passa a ser mostrado como acesso negado; 400/404/422 como consulta recusada. Falhas nunca são armazenadas como pesquisa vazia. A geração de cache de buscas muda, descartando o efeito de resultados vazios antigos sem apagar arquivos de legendas já baixados.
- Diagnóstico informa status HTTP, tipo fixo de falha de rede, quantidade recebida/aceita de resultados e classificação do endereço original da imagem como PROVIDER/TMDB (um link da própria lista também pode apontar para o CDN TMDB). Não exporta URLs, títulos, chave, token, senha, corpo de resposta ou texto de exceção.

R15 mantém lote 2.000; regras de histórico, temporadas, normal/Kids, versão, assinatura, resolução de capas e limites/rotatividade de armazenamento permanecem. O objetivo global de 400 MB não foi novamente medido nem garantido por esta entrega. Arquivo privado de chave, conta salva e configuração Cloudflare não são alterados. O pacote aplica sete fontes sobre a r16, preservando backup externo e verificando hashes. Não contém APK ou credenciais.

## Instalar

Extraia em `Downloads\capas-legendas-iptv-premium-r17`. No PowerShell do Android Studio com a TV autorizada:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\capas-legendas-iptv-premium-r17\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

Compila localmente usando sua configuração privada e instala com `adb install -r`; não desinstala nem limpa dados. Não é necessário cadastrar outra chave ou atualizar os segredos do site. Se a sessão tiver expirado, conecte novamente sua conta pelo portal.

## Validar na TV

1. Abra os filmes novos e observe a primeira carga das capas em Xtream e M3U. Reabra o catálogo; imagens salvas devem ser reutilizadas.
2. Abra Resident Evil, menu Legendas. Aguarde a pesquisa e confira a mensagem técnica. Teste também uma obra conhecida que já tenha legendas no serviço. Não faça downloads repetidos apenas para gerar diagnóstico.
3. Depois de reproduzir ambos os problemas, execute:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\capas-legendas-iptv-premium-r17\Coletar-Diagnostico.ps1"
```

Envie `Downloads\IPTV-capas-legendas-r17.txt`. Para coleta ao vivo, acrescente `-AoVivo`, reproduza os problemas e encerre com Ctrl+C. A coleta não limpa o log ou dados da TV. Registros antigos podem coexistir; os novos incluem `origin=...`, `stage=provider_attempt` e campos de pesquisa.

A falha de OpenSubtitles ainda precisa ser identificada no serviço real. Se vier HTTP 403, confira se o API Consumer está autorizado para a pesquisa; não troque chave ou compre plano com base apenas nessa hipótese. Os testes locais usam respostas simuladas.

## Restaurar

Use o caminho impresso ao aplicar:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\capas-legendas-iptv-premium-r17\Desfazer.ps1" -Backup "PASTA_DA_RESTAURACAO" -Instalar
```

Restaura estes sete fontes, sem apagar a conta, a chave privada ou os dados da TV.
