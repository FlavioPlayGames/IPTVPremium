# Categorias sem conteudo e teste de rolagem a 50%

Oculta categorias zeradas em Filmes/Series, inclusive Kids, com as contagens existentes. Nenhum dado e excluido. Categorias reaparecem quando recebem conteudo; atalhos Todos, Favoritos e Continuar assistindo permanecem.

Perfil de teste: movimento das capas pelo controle a metade da velocidade anterior, sem reduzir a taxa de quadros nem mudar destinos/soltura das teclas. Usa o mesmo motor compartilhado por Filmes, Series e Kids.

Baixe `categorias-vazias-tv.zip`, execute `Aplicar.ps1 -Projeto <pasta> -Testar` e depois `Instalar-TV.ps1`. Base verificada por hash; compativel com as correcoes recentes. Inclui backup e Desfazer. [Validacao](VALIDACAO.txt); teste final na TV pendente.
