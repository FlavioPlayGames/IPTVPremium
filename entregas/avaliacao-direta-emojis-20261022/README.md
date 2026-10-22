# Avaliação direta e novas reações

Implementação para Filmes e Séries sobre o projeto enviado, mantendo a versão 1.11 (13). São dois arquivos Kotlin; não há novas imagens, fontes ou bibliotecas.

## O que muda

O menu “Sua avaliação” deixa de abrir. Na ficha, selecione a avaliação e pressione OK: os desenhos das estrelas passam ao contorno amarelo, sem o retângulo branco. Esquerda/direita escolhe de 1 a 5; outro OK confirma. No toque, o primeiro toque entra na edição e o segundo escolhe a nota. Voltar cancela a edição sem fechar a ficha.

Ao confirmar, as estrelas escolhidas se juntam e crescem numa camada temporária sobre a própria ficha:

- **1–2 estrelas:** rosto sorridente que derrete, inspirado em 😊 → 🫠, cerca de **4,6 segundos**.
- **3–4 estrelas:** sorriso, biquinho e coração vermelho que flutua, inspirado em 😊 → 😘 → ❤️, cerca de **4,6 segundos**.
- **5 estrelas:** foguete decola para cima e para a direita, com partículas e **OBRIGADO!** surgindo em diagonal no rastro, cerca de **5,8 segundos**.

São desenhos vetoriais próprios, evitando diferenças de emojis entre aparelhos. A camada não abre uma janela de menu nem assume foco. Durante a reação, novos comandos de nota são ignorados; Voltar encerra somente a reação. Ao terminar, a ficha permanece aberta com a nota salva. O salvamento continua em segundo plano se a pessoa sair antes; erros revertem a nota e mostram um aviso.

“Limpar” aparece ao editar uma avaliação existente. No controle, chegue à quinta estrela e pressione direita para focar “Limpar”; OK remove a nota.

## Aplicar

Execute no PowerShell:

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/avaliacao-direta-emojis-20261022'
$script = Join-Path $env:TEMP 'Preparar-Avaliacao-Direta.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Avaliacao-Direta.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O script confere hashes, aceita a versão original enviada ou a entrega anterior das reações e cria backup em `.inline-rating-backups`. Se os arquivos tiverem outras alterações locais, ele interrompe antes de copiar. Não apaga imagens ou dados. O script compila, mas não instala automaticamente: gere/instale o novo APK na TV depois.

Para usar o pacote baixado manualmente, acrescente `-PacoteLocal 'C:\caminho\avaliacao-direta-emojis.zip'`.

## Conferir na TV

Em um filme e uma série, teste as notas 1/2, 3/4 e 5. Confira o tamanho, a velocidade e a trajetória do foguete. Após cada reação, teste o controle na ficha, saia e retorne para conferir a nota salva. Teste também Voltar durante a edição e durante a reação, e a opção “Limpar”.

Compilação local validada. O foco físico, a fluidez e a aparência final precisam ser conferidos na TV; não há aparelho conectado neste ambiente. Nenhum teste automatizado foi adicionado ao aplicativo.
