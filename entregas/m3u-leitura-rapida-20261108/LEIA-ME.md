# M3U direta — processamento dos atributos mais leve

Atualização do aplicativo, versão 1.13 mantida. Não é necessário publicar o site por causa desta otimização.

## Trabalho realizado

O importador fazia consultas separadas para group-title, tvg-logo, tvg-id e a indicação xui-id de cada entrada, repetindo a leitura do mesmo texto e criando resultados intermediários. A nova implementação extrai os atributos juntos e mantém os mesmos critérios de classificação, primeiras ocorrências, atributos vazios e indicação XUI, inclusive os casos legados preservados pelos testes.

Não remove canais, filmes ou episódios, não troca a ordem do provedor, não muda IDs nem converte M3U direta em Xtream. Mantém os limites dos lotes e filas, a leitura em fluxo, gzip, gravação sobreposta à leitura e confirmação do catálogo somente após concluir todas as gravações. Falhas/cancelamentos continuam preservando o catálogo ativo. Recursos privados e configurações de contas não são tocados.

## Por que ainda pode levar mais tempo que Xtream

Uma M3U direta exige ler e processar o arquivo inteiro, inclusive todos os episódios que o provedor colocou nele. A via Xtream pode deixar os episódios para consulta sob demanda. O ganho abaixo é do processamento local; não promete que a importação M3U passará a ter o mesmo tempo total de Xtream.

## Medições locais

Comparação isolada dos atributos: 180.000 entradas por execução, mediana de cinco execuções alternadas após aquecimento, aproximadamente 922 ms antes e 271 ms depois (3,4 vezes na etapa testada). Preservação de campos verificada em nove exemplos e 6.000 linhas geradas. Importador completo: 16 cenários de paridade/falha/cancelamento com armazenamento e rede simulados, incluindo 20.000 filmes com conteúdo, IDs e providerOrder idênticos. Uma comparação sintética de importador teve 502 ms antes e 226 ms depois; é uma amostra local, não tempo de TV.

Os testes com latência simulada de leitura e banco não mostraram ganho relevante, como esperado quando essas esperas dominam. Nenhuma medição foi feita na sua TV ou no provedor real. O tempo total relatado de menos de dois minutos ainda precisa ser medido por etapas para sabermos quanto está em download, processamento e armazenamento.

## Aplicar e instalar

Extraia o ZIP em C:\Users\flavi\Downloads\m3u-leitura-rapida. Os scripts ficam na raiz dessa pasta.

```powershell
Set-Location "C:\Users\flavi\Downloads\m3u-leitura-rapida"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Depois de BUILD SUCCESSFUL:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

O instalador aceita o importador conhecido das últimas entregas, verifica os arquivos antes de alterar, cria backup e recusa versão desconhecida. Desfazer.ps1 restaura os originais com proteção de conflito.

## Medir uma importação real

Com a TV conectada ao ADB, ainda na pasta do pacote, execute ANTES de iniciar uma nova importação M3U direta:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao-M3U.ps1 -Formato M3U -PastaSaida "C:\Users\flavi\Downloads"
```

Quando aparecer Captura iniciada, atualize a lista M3U direta no aplicativo. Não exclua os dados nem cadastre outra entrada. O script encerra após o resumo da importação ou o prazo de dez minutos e grava diagnostico-importacao-m3u-atual.txt em Downloads. Envie esse relatório para analisarmos os tempos reais.

A captura só lê logs e reconstrói uma lista permitida de métricas numéricas/estados; não altera perfil, configurações ou dados, não limpa o logcat e não inclui URLs, senhas ou mensagens livres de exceção no relatório. Havendo mais de um aparelho conectado, informe -Dispositivo com o serial mostrado por adb devices.

As fases podem ocorrer em paralelo: os tempos de leitura, parser e banco não devem ser somados como se fossem etapas sequenciais. A leitura inclui rede/decodificação; os bytes e métricas HTTP ajudam a separar essas esperas.
