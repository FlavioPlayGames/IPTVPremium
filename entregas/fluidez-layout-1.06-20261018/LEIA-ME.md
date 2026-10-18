# Fluidez, títulos completos e layout aprovado — 1.06 / interno 8

Minhas Listas: mantém o fundo real desfocado, transparência e contornos de cada tema; amplia e espaça as linhas; troca o grande ícone repetido à direita por LISTA SELECIONADA, com hierarquia semelhante à proposta aprovada. Usar lista mantém aparência neutra fora do foco. Não usa a imagem gerada como fundo do app. Exclusão continua confirmada.

Busca de Filmes/Séries: texto/cursor respondem imediatamente; a consulta ao Room só muda após 350 ms sem digitar. Limpar responde imediatamente. A grade não é enviada ao topo a cada letra; a navegação pendente é cancelada ao abrir o teclado. Capas visíveis têm prioridade; antecipação de até 15 capas continua sob demanda. O antigo aquecimento de 15 fichas/fundos/logos foi reduzido a três fichas ociosas e é cancelado enquanto há navegação ou teclado aberto.

DPAD: destinos verticais mantêm a mesma coluna mesmo em várias teclas rápidas, linhas ainda não compostas e ida/volta; esperam o item real e o layout antes de focar. Não desviam para primeira coluna em última linha incompleta. Comandos horizontais cancelam a intenção anterior. Pedidos antigos de restauração são cancelados quando há navegação manual. A primeira linha ainda permite sair para a barra superior. Filmes e séries, incluindo Continuar assistindo, usam o controlador.

Nomes longos: rolagem contínua após pequena pausa somente no card focado, sem animar todos os títulos. Nomes curtos não se movem.

Memória: alvo de 350 MB para o processo inteiro, sem aumentar caches. Capas 20 MiB, detalhes até 24 MiB, carrossel 12 MiB; tema ativo mantém orçamento existente de 32+8 MiB e histórico de episódios segue limitado. Player Media3 recebe alvo de buffer comprimido de 24 MiB, priorizando tamanho. Paging de filmes/séries limita a janela a 180 itens, com placeholders de altura real para preservar posição global; carregamento inicial 30 e antecipação 15. O catálogo inteiro continua no Room. Nenhum favorito, histórico, lista ou tema é apagado.

A cada cinco segundos, fora da thread principal, mede PSS total do processo e pico. A partir de 280 MB pausa antecipações e libera caches descartáveis com intervalo de 30 s, preservando recursos fixos do tema. Caches/buffers controlados não garantem sozinhos que qualquer TV/codec fique abaixo de 350 MB: o consumo real inclui objetos visíveis, runtime, gráficos e decodificador nativo e precisa ser medido no aparelho. O diagnóstico CatalogMemory registra pssMb, picoMb e alvoMb durante grade, busca e reprodução. Não há emulador/KVM neste ambiente para afirmar FPS ou pico de memória real.

Validação: testes determinísticos de digitação em sequência, pausa, limpeza, intenção de navegação rápida, reversão, limites e margem de orçamento. Compilação app/testes Android e lint. Quatro testes Compose de foco/teclado estão compilados para execução no emulador: -TestarFluidez. -CapturarLog executa esses testes, instala por cima e captura diagnóstico de navegação, memória, histórico e séries; navegue rapidamente e reproduza conteúdo para obter medições reais. Não limpa dados nem altera listas reais durante os testes de foco.

Instalador cumulativo com hashes, backup, rollback e proteção de alterações desconhecidas. -Verificar não modifica o projeto. Mantém 1.06/interno 8. Correções anteriores de identidade, favoritos, episódios e artes permanecem incluídas.
