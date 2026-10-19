# Opção 1 — rolagem contínua — 1.07

A grade segue o destino dos comandos em uma animação contínua. Novas setas atualizam o destino sem reiniciar o movimento. Mantém a coluna; o layout existente preserva duas fileiras completas ao parar: oito capas com quatro colunas ou dez com cinco, quando houver itens suficientes. Durante o movimento há capas parciais nas bordas.

A trajetória virtual agora avança pelo movimento calculado, independente do arredondamento e da limitação de pixels consumidos pela grade. Isso evita prender a evolução da animação no limite superior. Ao concluir, alinha exatamente a fileira antes de devolver foco ao card. Mantida a tolerância de 1,5 pixel físico em baixa velocidade. Não altera curva, velocidade máxima, geometria, neon, metadados ou downloads.

App e APK de testes compilados; os mesmos 14 testes de navegação permanecem, incluindo quatro inversões e retorno às telas reais. Não há emulador nesta nuvem: confirmar no aparelho com -TestarNavegacao, sem aumentar o prazo de 900 ms nem remover verificações. Depois da aprovação automática, testar também segurando as setas e invertendo direção no controle da TV.

Instala por cima sem limpar dados, valida hashes e faz backup. Aceita as versões de navegação anteriores desta sequência. Diagnóstico: diagnostico-rolagem-continua.txt. A única alteração efetiva desta entrega é na integração da trajetória em CatalogGridNavigation.kt; o arquivo de testes incluído é preservado.
