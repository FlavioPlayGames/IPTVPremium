# Foco reforçado — 1.06

Proposta 3 aprovada: capas de Filmes e Séries recebem contorno metálico de 4 dp, reflexos localizados, halo curto e filete interno escuro. Linha de 3 dp abaixo do título em foco; espaço constante para evitar saltos. Categorias selecionadas recebem o mesmo contorno reforçado. Todas as sete paletas usam suas cores, com textos brancos e sem filtro nas imagens.

Desenho estático com drawWithCache, sem blur de GPU, bitmap extra ou animação permanente. Compatível com API 28. Mantém a navegação, busca, limites de cache e player da entrega anterior. O alvo de 350 MB para o processo completo ainda exige medição na TV.

Compilação e lint em nuvem; testes Android compilados. Sem emulador/KVM para validar a aparência real ou o PSS aqui. Instale por cima e confira o foco nas sete paletas. -CapturarLog mantém o diagnóstico de fluidez/memória.

Instalador cumulativo, com hashes, backup e rollback. Não limpa dados. -Verificar apenas confere compatibilidade.
