# Teste de rolagem por fileira

Perfil experimental inspirado no video: aproximadamente 400ms por fileira, inicio suave, continuidade durante repeticao da tecla e parada alinhada. Usado em Filmes, Series e Kids. Nao altera o layout, importacao, dados ou player.

Base e9431fe (perfil anterior de 25%). Baixe `rolagem-referencia-tv.zip`, execute `Aplicar.ps1 -Projeto <pasta> -Testar` e depois `Instalar-TV.ps1`.

## Voltar ao movimento anterior

`Reverter-Rolagem.ps1 -Projeto <pasta>` restaura do backup somente `CatalogScrollMotion.kt`, recompila e reinstala a versao com o movimento anterior. Categorias vazias ocultas, amarelo vivo e demais correcoes permanecem. Sem limpar dados. TV conectada necessaria para reinstalar.

[Validacao](VALIDACAO.txt). O comportamento visual final precisa ser avaliado no aparelho.
