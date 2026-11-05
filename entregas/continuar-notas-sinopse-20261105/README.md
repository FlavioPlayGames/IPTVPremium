# Notas no Continuar e OK na sinopse

Corrige os cartões de Continuar dos catálogos normal e Kids para receber a nota e o ano da edição realmente disponível na lista atual. Usa os registros locais já consultados para conferir disponibilidade, sem baixar dados de toda a lista ou exigir assistir novamente. Os valores são de apresentação e não ficam gravados no histórico; ao trocar de lista usa a nota da edição correspondente. Nota ausente aparece como —, sem inventar zero. Inclui capas de séries.

Dentro da sinopse do catálogo do player, OK/Enter/A reproduz o filme; em Continuar preserva a posição de retomada. Para séries, abre as temporadas, inclusive quando a capa veio de Continuar. A confirmação acontece ao soltar a tecla, uma vez por pressão, e não favorita dentro da leitura. ←/Voltar continuam recolhendo a gaveta e retornando à capa, sem saltar níveis. Nas capas, segurar por 2s continua favoritando.

Mantém a versão 1.13, altura/entrada Kids, perfis separados, posição do vídeo, cores, estrela amarela, opacidade, gaveta e rolagem otimizada. Não limpa histórico nem altera banco ou preferências antigas. Incremental sobre kids-posicao-entrada dc17965. Cinco fontes Kotlin alterados.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Há backup e Desfazer.ps1, com proteção de edições posteriores. Fontes desconhecidos são recusados. Sem APK ou configurações privadas.

Conferir na TV: notas no Continuar normal/Kids, títulos com/sem avaliação, edição de outra lista, OK dentro da sinopse de filme/filme em Continuar/série, repetições da tecla, Voltar e pressão longa na capa.
