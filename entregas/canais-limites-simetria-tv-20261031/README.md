# Canais: simetria, limites de foco e tela ativa

Categorias e canais com larguras iguais. Cima no primeiro item leva a Organizar; datas respeitam limites superior/inferior; Esquerda nas datas vai ao video antes dos canais. Tela permanece ativa enquanto MainActivity esta em primeiro plano, com liberacao ao pausar.

Player ao vivo agora adquire prioridade propria para abertura e primeiros 2s de reproducao, adiando somente tarefas de fundo cooperantes (XMLTV etc.). Sem aumentar espera do video nem alterar buffer/codec. Pausa de 2s ainda exige validacao no aparelho: inclui LiveLoadPerf e Capturar-Canais.ps1 para medir primeiro quadro/buffering se persistir.

Pacote cumulativo, base e1f2e47 e anteriores compativeis. Baixe ZIP, execute Aplicar.ps1 -Projeto <pasta> -Testar e Instalar-TV.ps1. Backup e Desfazer incluidos; nao limpa dados.

[Detalhes](LEIA-ME.txt), [validacao](VALIDACAO.txt). Compilacao, politicas, conexao e instaladores passaram; foco, descanso e pausa inicial requerem teste na TV real.
