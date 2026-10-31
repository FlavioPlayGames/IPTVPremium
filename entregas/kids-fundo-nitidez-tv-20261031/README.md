# Kids: fundo visivel, sem estrela e nitidez

Fundo azul/rosa mais presente, nuvens maiores e centro legivel. Remove estrela de selecao da Home e das capas. Mascotes usam pixels nativos e sao desenhados na escala final, mantendo expansao 8%/140ms. PNGs/logo/personagens continuam separados e nao sao redesenhados.

Nao altera velocidade/motor de scroll ou animacao das capas. Retira as artes da Home do cache ao sair e precalcula as formas do fundo. A perda de fluidez ainda exige verificacao na TV: inclui Capturar-Fluidez.ps1 para diagnostico do processo se persistir.

Requer visual Kids dc26f5f aplicado. Pacote pequeno, tres fontes. Baixe ZIP, execute Aplicar.ps1 -Projeto <pasta> -Testar e Instalar-TV.ps1. Backup e Desfazer incluidos, sem apagar dados.

[Detalhes](LEIA-ME.txt), [validacao](VALIDACAO.txt). Compilacao/cache/instaladores passaram; aparencia/nitidez e fluidez precisam de teste na TV.
