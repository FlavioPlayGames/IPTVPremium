# Kids — ícones mais baixos e entrada direta

Desloca juntos os três ícones/mascotes e seus nomes 5% da altura da tela para baixo. Mantém tamanho, espaçamento, logo, cores e áreas de foco/clique alinhadas. Vale para azul e rosa.

Retira o fade ao entrar/retornar à tela inicial Kids. Enquanto a Home normal está pronta, prepara as artes locais em segundo plano, sem rede e sem aguardar no clique. O cache existente de 32 MiB e os pixels nativos são preservados. A preparação é evitada/interrompida sob pressão de memória. Ao passar da Home para Kids, uma tolerância de 1,5s evita descartar imagens entre a saída de uma composição e a entrada da outra; sem dono, as três artes são retiradas depois. Não retém novas coleções ilimitadas.

Incremental sobre kids-historico-1.13 (159ad5a). Três fontes Kotlin alterados. Mantém a versão 1.13, o histórico por perfil, o botão Assistir, o player e os favoritos. Não repete a limpeza do Continuar já realizada.

Extraia e execute no PowerShell dentro da pasta:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, execute Instalar-TV.ps1 com o mesmo -Projeto. Há backup e Desfazer.ps1, com proteção de edições posteriores. O instalador recusa fontes desconhecidos. Sem APK ou configurações privadas.

Conferir na TV a nova altura e a resposta ao abrir/voltar ao Kids, inclusive primeira entrada, retorno rápido e após usar filmes/séries. Não houve medição de latência/FPS no aparelho; o ajuste remove a espera visual e antecipa a decodificação identificada no fonte.
