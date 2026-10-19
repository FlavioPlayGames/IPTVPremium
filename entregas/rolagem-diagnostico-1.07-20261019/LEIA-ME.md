# Diagnóstico das etapas da rolagem — 1.07

O último resultado mantém 10 aprovações e quatro falhas de inversão. As correções anteriores não resolveram. Não é possível concluir apenas pelo deslocamento de 1 pixel que o problema esteja na integração da trajetória.

Esta entrega NÃO anuncia nova correção de comportamento. Acrescenta estado de diagnóstico consultado somente se o teste falhar: marcador de versão continuous-trace-1, etapa, quadros, tempo virtual, distância, velocidade, destino, atividade do trabalho e presença do solicitante de foco. Nenhuma informação de lista ou credencial é coletada.

Mantidos os mesmos 14 testes, prazos, verificações, curva, velocidade e layout. App e APK de testes compilados. Sem emulador na nuvem; o teste no emulador do usuário é necessário para localizar a suspensão real e evitar novas alterações por hipótese.

Executar com -TestarNavegacao. Arquivo de saída diagnostico-rolagem-etapas.txt. Instalador valida hashes, faz backup e instala por cima sem limpar dados. Apenas dois arquivos alterados: navegador e mensagem de falha do teste.
