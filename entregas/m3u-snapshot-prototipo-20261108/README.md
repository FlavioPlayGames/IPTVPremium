# Protótipo de importação M3U em catálogo separado

Este pacote cria uma entrada de teste exclusiva de M3U. Ainda não substitui o importador usado pelos menus normais. O objetivo é medir a nova arquitetura na TV antes de integrá-la. A meta de 25 segundos ainda não foi verificada no aparelho.

O pacote acrescenta somente arquivos novos: não altera o sincronizador, o importador ou os dados do Xtream. O catálogo normal e o histórico continuam no banco atual. O protótipo usa outro diretório e publica um arquivo próprio, validado pelo Room.

## Aplicar e instalar

Na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Somente após BUILD SUCCESSFUL:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

## Executar o protótipo

Abra o aplicativo normalmente, selecione a mesma lista M3U que usamos nos testes e aguarde o catálogo normal estar pronto. Deixe a TV sem reproduzir vídeo ou fazer outra importação. O teste abaixo fecha o aplicativo e abre a tela dedicada para medir o protótipo isoladamente; não apaga dados.

```powershell
powershell -ExecutionPolicy Bypass -File .\Testar-Prototipo-M3U.ps1 -PastaSaida "C:\Users\flavi\Downloads"
```

O download começa sozinho usando a URL já cadastrada na TV. Não digite usuário, senha ou link no PowerShell. Mantenha a TV na tela do protótipo. Voltar cancela. Se a lista ativa for Xtream, o teste pede para selecionar M3U; ele não converte ou testa o Xtream.

Ao terminar, envie o TXT de Downloads com nome diagnostico-prototipo-m3u-r1-....txt. Para repetir, acrescente -Rodada 2 ou -Rodada 3. Primeiro vamos conferir uma execução e, depois, comparar três rodadas nas mesmas condições. Se houver vários aparelhos, informe -Dispositivo com o serial de adb devices.

A tela mostra contagens e tempo do protótipo. Ao voltar ao aplicativo, os menus ainda usam o catálogo normal; essa primeira etapa testa a construção independente, não a integração de navegação. Use somente a variante desempenho instalada por este pacote. A atividade de teste não aparece no launcher e exige a permissão privilegiada DUMP do chamador ADB.

## O que medir

stage=complete totalMs é o tempo total, incluindo a leitura da seleção, espera de prioridade e execução do protótipo. stage=summary totalMs mede o pipeline dentro do construtor. O coletor aguarda complete em caso de sucesso; não considera pronto apenas por terminar a carga.

As etapas incluem limpeza de testes interrompidos, download para arquivo, preparação binária, criação de banco privado, carga, criação de índices, verificação, validação pelo Room, checkpoint final e publicação. commit é uma parcela de load: não some os dois. sourceHash permite verificar se duas execuções receberam a mesma fonte normalizada; o relatório não contém a URL.

Os arquivos ficam no diretório privado files/m3u_snapshot_prototype. São necessários espaço para fonte, registros preparados, banco e WAL. Há uma verificação conservadora de espaço, mas falhas de armazenamento ainda podem ocorrer; nesses casos o catálogo normal permanece preservado. Os registros de episódios são processados incrementalmente. Só categorias e metadados finais das séries são mantidos em mapas.

## Remover os arquivos do experimento

Antes de desfazer o código, você pode apagar somente os arquivos do protótipo:

```powershell
powershell -ExecutionPolicy Bypass -File .\Testar-Prototipo-M3U.ps1 -Limpar -PastaSaida "C:\Users\flavi\Downloads"
```

Esse comando fecha o aplicativo e limpa somente m3u_snapshot_prototype. Não limpa listas, favoritos, histórico ou bancos normais. Depois, se desejar restaurar o código:

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Há backup e conferência de hashes. Arquivos existentes com conteúdo diferente não são sobrescritos. RELATORIO.md descreve a arquitetura, os testes e o que ainda depende da TV.
