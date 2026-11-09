# Capas de temporadas em RAM no catálogo do player

Esta revisão muda somente o menu de temporadas dentro do player. Ao carregar uma capa, mantém uma referência à miniatura em RAM durante a navegação da série selecionada. Ao voltar para uma temporada já carregada, a imagem aparece diretamente, sem zerar o bitmap nem passar novamente pelo carregador.

O cache pertence ao catálogo aberto e à série selecionada. Ao fechar o catálogo ou selecionar outra série, libera suas referências. Entrar em episódios e voltar às temporadas da mesma série preserva as imagens. A próxima abertura usa normalmente os caches existentes do aplicativo; não força outro download.

São guardadas somente miniaturas efetivamente carregadas, com limite de 8 MB e 256 entradas. Não baixa antecipadamente todas as temporadas. Se uma série ultrapassar o limite, as miniaturas menos usadas podem voltar ao carregador. Não altera importação, regra de capas do provedor/TMDB, episódios, catálogo externo ou qualidade de reprodução.

**A imagem repetida no M3U ainda precisa de diagnóstico da lista atual. Este pacote não é uma correção desse problema.**

## Instalar

Base esperada: projeto com `capas-provedor-temporada` aplicado. O instalador valida a base e guarda os arquivos anteriores. Extraia para `C:\Users\flavi\Downloads\player-temporadas-ram`.

```powershell
Set-Location "C:\Users\flavi\Downloads\player-temporadas-ram"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Não precisa atualizar a lista para testar este ajuste.

## Conferir

1. Durante a reprodução, abra o catálogo do player.
2. Selecione uma série com várias temporadas e aguarde algumas capas.
3. Navegue para baixo e volte às temporadas já vistas: devem reaparecer sem limpar a imagem.
4. Entre em episódios e volte às temporadas: as miniaturas já carregadas devem permanecer.
5. Feche o catálogo: o cache temporário é liberado.

## Desfazer

```powershell
powershell -ExecutionPolicy Bypass -File .\Desfazer.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
```

Restaura os fontes anteriores se não tiverem sido modificados depois. Não apaga os dados da TV.
