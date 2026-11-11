# Capas: ordem de importação e busca r11

Base: fontes da r10. A r11 retira o genérico antecipado e implementa a ordem solicitada, preservando a fila, o orçamento e o histórico.

## Instalar na TV

Pode usar o PowerShell do Android Studio, com a TV conectada/autorizada:

```powershell
$pasta = "$env:USERPROFILE\Downloads\capas-iptv-premium-r11"
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O script verifica os hashes, guarda os fontes em Downloads\IPTV-restauracao-r11-HORARIO, compila desempenho com a assinatura local e instala com adb install -r. Não desinstala, limpa dados, remove listas ou repete a limpeza física.

**Após instalar esta versão, atualize as listas uma vez em Minhas Listas → selecionar a lista → Atualizar lista.** A r11 passa a importar IDs externos que antes não chegavam completos aos filmes; uma atualização feita na r10 não recupera esses campos novos.

## Regra

1. Usar a capa utilizável fornecida pela lista/provedor.
2. No TMDB, procurar pelo ID externo informado: TMDB diretamente ou IMDb convertido pelo serviço. O ID de stream do provedor e o tvg-id não são tratados como IDs do TMDB.
3. Sem identificação externa utilizável, buscar pelo nome e ano, em pt-BR, depois en-US e es-ES. O ano disponível também é enviado como filtro da API. Os resultados são conferidos pelos nomes oficiais/alternativos, ano e IDs; não se escolhe uma obra só por popularidade.
4. Somente depois de esgotar as tentativas sem uma capa válida, exibir o genérico. Durante a busca, não há pôster genérico nem preparação antecipada dos quatro recursos genéricos.

Quando faltam tanto o ID quanto o ano, a busca usa o nome e aceita apenas uma correspondência inequívoca; não inventa um ano nem escolhe entre homônimos.

## Correções

- Filmes Xtream passam a preservar tmdb_id/tmdb e imdb_id/imdb na leitura normal e rápida, na gravação do catálogo, no modelo de tela e na antecipação.
- M3U preserva IDs externos explícitos nos atributos tmdb-id, tmdb_id, tmdb, tvg-tmdb, imdb-id, imdb_id, imdb e tvg-imdb. IDs conflitantes entre episódios não são atribuídos arbitrariamente à série.
- Para um filme visível sem capa utilizável e sem ID externo no catálogo, os detalhes do próprio provedor são consultados antes da busca por nome. Isso pode recuperar uma capa/ID que só exista em get_vod_info. Em M3U compatível, o ID de vídeo é extraído do endereço apenas para consultar o provedor, sem usá-lo como TMDB ID.
- Essas consultas extras não percorrem o catálogo: têm duas vagas, deduplicação por lista/geração/item e cache RAM limitado a 128 entradas, sem copiar vídeos ou credenciais. A antecipação continua com a fila anterior, sem chamar get_vod_info em massa.
- O cartão só confirma ausência depois da última tentativa; o genérico não aparece no intervalo da nova tentativa nem enquanto uma consulta está pendente. Normal e Kids usam a mesma regra de apresentação.
- Capas continuam em até 300x450, com proporção preservada. Fila 15→25, grupos de cinco, dois trabalhadores de antecipação e os limites/rotação existentes são mantidos.

## Dados e recuperação

O banco descartável do catálogo recebe duas colunas opcionais para IDs de filmes por migração 6→7, sem excluir linhas, listas, histórico ou favoritos. Esse dado permite consultar o TMDB pelo ID após reiniciar o aplicativo.

Para recuperar a lógica anterior com segurança, use o Desfazer.ps1 deste pacote. Ele mantém três arquivos de modelos/esquema compatíveis com o banco atualizado, restaura os demais fontes e pode compilar/instalar com -Instalar:

```powershell
powershell -ExecutionPolicy Bypass -File "$pasta\Desfazer.ps1" -Backup "PASTA_DA_RESTAURACAO" -Instalar
```

A recuperação não apaga o banco nem reverte ações feitas nos dados do aplicativo. Uma reaplicação da r11 reconhece os três arquivos mantidos.

## Validar

Confira Insaciável 4K no Xtream e Pesadelo na Cozinha no M3U após a atualização. Na lista M3U antiga enviada, Pesadelo na Cozinha tem 19 imagens diferentes entre os episódios e não declara IDs externos nesses registros; isso não fornece, por si só, uma capa geral confiável. A busca deve confirmar a série, sem escolher uma foto de episódio ou um homônimo.

Para registrar o fluxo real na TV, execute antes de reproduzir o caso:

```powershell
powershell -ExecutionPolicy Bypass -File "$pasta\Coletar-Diagnostico.ps1" -AoVivo
```

Encerre com Ctrl+C. O resultado filtrado fica em Downloads\IPTV-capas-r11.txt. Inclui fonte da capa, tempos e a consulta get_vod_info, sem URLs, títulos, senhas ou respostas do provedor. Não envie arquivos de recuperação ou assinatura.
