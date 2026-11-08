# M3U oficial e ordem do provedor

Aplicar sobre o java.zip enviado nesta conversa, após m3u-episodios-demanda. O pacote substitui o importador M3U antigo, elimina os parsers alternativos e a atividade do protótipo. O site continua funcionando com o mesmo envio de listas. A seleção automática de transporte e M3U direta do administrador continuam respeitadas.

## Aplicar

Extraia o ZIP e abra o PowerShell na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

O script verifica os arquivos antes de alterar, cria backup e compila. Se apontar arquivo divergente, envie a saída; não force a troca.

Somente após BUILD SUCCESSFUL, com a TV conectada:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

## Atualizar e conferir

1. Inicie o coletor abaixo antes da atualização.
2. Na TV, atualize uma vez a mesma lista M3U direta. Não apague dados nem recrie a lista.
3. Confira a ordem das categorias, canais e séries. A ordem padrão segue o provedor; A–Z e Z–A continuam disponíveis quando escolhidos.
4. Abra uma série, confira temporadas/episódios, reproduza e depois abra novamente a mesma série. Teste também outra série e o Continuar.
5. Pressione Enter no PowerShell após os testes e envie o TXT gerado.

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -PastaSaida "C:\Users\flavi\Downloads"
```

O diagnóstico contém m3u-oficial no nome e separa download, preparação, gravação e fonte de episódios. Agora também preserva o tempo de carregamento dos episódios e eventos de cache. Se houver vários aparelhos, acrescente -Dispositivo com o serial mostrado por adb devices. -SomenteImportacao termina sem esperar pelos testes manuais.

## O que mudou

A importação oficial baixa a M3U para um arquivo temporário, prepara o catálogo localmente e grava seus metadados com statements reutilizados numa única transação Room. Não troca nem remove o banco inteiro, não apaga índices compartilhados e não interfere nas outras listas. A geração anterior permanece ativa até a publicação da nova.

Episódios permanecem numa fonte privada indexada por série; somente a série solicitada é materializada no Room. Reabrir usa cache, sem novo download da M3U. A fonte permanente continua em files/m3u_episode_sources; os arquivos temporários da importação são removidos ao terminar ou cancelar normalmente.

A ordem das categorias M3U é persistida pela primeira ocorrência na lista, inclusive no Kids e na tela de canais. Canais usam a sequência de entrada; séries usam a primeira ocorrência; filmes mantêm a regra de última ocorrência para entradas repetidas. A classificação entre canal/filme/série e os IDs permanecem compatíveis com o protótipo aprovado.

Os arquivos de sincronização, resolução de transporte, parser e episódios Xtream foram preservados. As categorias Xtream continuam com a ordenação anterior. Não houve alteração de qualidade de vídeo ou dependências.

## Banco e compatibilidade

O banco passa da versão 5 para a 6 por migração que acrescenta somente uma coluna opcional de posição às categorias. Não apaga favoritos, histórico, listas ou catálogos. As categorias antigas continuam disponíveis; é necessário atualizar a M3U uma vez para registrar as posições que antes não eram guardadas.

Não reinstale um APK antigo que conheça apenas o banco 5 sobre dados já migrados. O Desfazer deste pacote mantém a compatibilidade com o banco 6.

## Desfazer a mudança funcional

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Isso restaura o importador anterior e a apresentação anterior, mantendo a migração e a entidade de categorias na versão 6. A atividade de protótipo permanece removida. Nenhum dado da TV é apagado.

A nova velocidade precisa ser medida na TV; os 61,5 segundos pertencem à implementação anterior sob demanda, não a esta revisão oficial.
