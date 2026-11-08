# M3U: episódios sob demanda

Esta atualização aplica a regra de episódios sob demanda ao M3U nativo do aplicativo e ao protótipo. Os arquivos de sincronização, parser e consulta de episódios da API Xtream foram preservados.

## Comportamento

Na atualização M3U são gravados canais, filmes, categorias e os metadados finais de cada série. Os episódios ficam numa fonte local compacta e indexada, fora do Room. Quando uma série é solicitada pela abertura/foco ou pelas rotinas existentes do Continuar, somente seus episódios são preparados e gravados no cache. Abrir outra série não exige baixar a lista inteira novamente. O cache fica separado por playlist e geração.

A M3U ainda precisa ser lida inteira para reconhecer os conteúdos: ela não oferece o endpoint de episódios do Xtream. Esta mudança adia a criação de IDs/entidades e a persistência Room dos episódios. Não é promessa de importação em 25 segundos; precisamos medir na TV.

## Aplicar e instalar

O pacote deve ser aplicado sobre a base m3u-snapshot-prototipo já usada no último teste. Extraia e execute na pasta:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Somente após BUILD SUCCESSFUL:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

## Testar no catálogo normal

As listas antigas já carregadas continuam utilizáveis. Atualize UMA vez a mesma lista M3U direta para criar a nova geração com episódios sob demanda. Não apague dados nem recrie a lista. O site pode continuar enviando a lista normalmente; a seleção M3U direta continua respeitada.

Para medir, inicie a coleta ANTES de atualizar a lista na TV:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -PastaSaida "C:\Users\flavi\Downloads"
```

Depois de Captura iniciada, atualize a lista no aplicativo e aguarde concluir. O PowerShell pedirá para abrir uma série e abrir novamente, conferindo o cache. Teste também outra série, temporadas, reprodução e Continuar. Só depois pressione Enter no PowerShell para salvar o TXT. Envie o diagnóstico cujo nome contém demanda.

Se houver vários aparelhos, acrescente -Dispositivo com o serial exibido em adb devices.

## Comparar pelo protótipo separado

Opcionalmente execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\Testar-Prototipo-M3U.ps1 -PastaSaida "C:\Users\flavi\Downloads"
```

Esse teste fecha o aplicativo e abre o protótipo usando a lista M3U selecionada. O resumo deve mostrar episódios no banco inicial iguais a zero e a quantidade disponível sob demanda. Use o botão Escolher uma série e testar episódios. Abra uma série, volte pelo botão Escolher outra série e abra a mesma novamente. Você pode testar outras séries. Depois pressione Enter no PowerShell para salvar a coleta completa. O tempo inicial é separado do tempo de abrir episódios, conforme a nova regra solicitada.

Para coletar somente a importação inicial, sem esperar pelos testes manuais, acrescente -SomenteImportacao. O modo -Limpar continua apagando apenas os arquivos do protótipo, não as fontes do M3U normal nem o histórico.

## Segurança dos dados e limites

A fonte dos episódios normais fica no diretório privado files/m3u_episode_sources e deve permanecer disponível. Não é preciso manter a conexão com o provedor para montar a lista de episódios, mas reproduzir o vídeo continua dependendo do link do provedor. A primeira solicitação de uma série tem o custo de montar seu cache; as próximas reaproveitam os dados.

IDs, URLs, capas, temporada/episódio e ordenação permanecem compatíveis com o importador nativo. Duplicidades mantêm a última ocorrência. A publicação do cache de uma série é transacional e confere a geração ativa; erro ou cancelamento não deixa um cache parcial publicado. O banco permanece na versão 5, sem migração nova.

Há backup e validação de hashes antes de aplicar. Se houver arquivo divergente, envie o erro; não force a substituição.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Depois de restaurar a regra antiga, atualize as listas M3U nativas para repor no Room todos os episódios que agora estavam sob demanda. As fontes locais não são apagadas automaticamente ao desfazer. Favoritos, histórico e cadastro de listas não são apagados.
