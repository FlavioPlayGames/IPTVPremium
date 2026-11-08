# Regra estável do Xtream em listas automáticas

Aplicar sobre o projeto atual, após m3u-capas-series. Não é substituição integral pelo java.zip antigo: preserva M3U oficial, capas, ordenação, histórico/Kids e banco versão 6.

## Aplicar

Extraia o ZIP e execute na pasta extraída:

```powershell
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Após BUILD SUCCESSFUL, com a TV conectada:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

## O que retorna à regra antiga

A lista informada foi cadastrada por link M3U com reconhecimento automático. Quando a API é escolhida, uma falha de importação volta a ter até quatro tentativas Xtream, sem disparar uma importação M3U completa após duas tentativas. A função de retentativa foi restaurada textualmente da versão antiga enviada.

Um erro na verificação da conta também encerra com erro em vez de escolher M3U silenciosamente. Uma resposta negativa da verificação ainda permite o importador nativo, assim como a opção M3U direta do administrador. O transporte só é gravado depois do sucesso da importação; catálogo ativo, cancelamento e falha local de armazenamento continuam protegidos.

O leitor HTTP, parser e writer Xtream já eram idênticos aos da versão antiga comparada. Não foram trocados. Xtream cadastrado diretamente com servidor/usuário/senha também já usava o mesmo fluxo de importação. Não se promete redução de tempo do servidor nem velocidade de uma lista que já estava importando pela API sem falhas.

## Testar e medir

Não apague dados nem recrie a lista. Inicie a captura:

```powershell
powershell -ExecutionPolicy Bypass -File .\Medir-Importacao.ps1 -Formato XTREAM -PastaSaida "C:\Users\flavi\Downloads"
```

Quando aparecer Captura iniciada, no site edite a MESMA lista existente e envie novamente com reconhecimento automatico (nao M3U direta). Isso solicita uma nova verificacao Xtream sem excluir nem recriar o cadastro. Aguarde a TV receber a atualizacao. Após terminar, confira o catálogo e pressione Enter no PowerShell. Envie o TXT com xtream-restaurado no nome.

A captura inclui rede, parsing, gravação por endpoint, tentativas e formato utilizado, sem links ou credenciais. Se a API falhar, o erro será mostrado e o catálogo anterior permanecerá disponível. Isso permite distinguir erro de API de uma atualização M3U longa. Se houver vários aparelhos, informe -Dispositivo com o serial do adb devices. -SomenteImportacao dispensa a pausa final de conferência.

Uma lista que já tenha sido gravada como M3U nativa não é convertida automaticamente por esta correção. Ela conserva a identidade e o histórico. Para renegociar o formato, use a edição explícita já existente no site; não exclua a lista. O relatório identificará o transporte efetivamente usado.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

O pacote altera somente o sincronizador. Não há migração de banco, alteração de qualidade de vídeo ou limpeza de dados.
