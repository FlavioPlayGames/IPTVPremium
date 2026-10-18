# Tela de carregamento de séries — 1.06 / interno 8

Remove o nome específico da série abaixo de “Carregando série” no painel da TV. O anúncio de acessibilidade também passa a ser genérico. Mantém o spinner, animação, cores de cada tema, prévia posterior e regras de carregamento.

Pacote cumulativo: mantém as correções anteriores de dados, artes e nomes em espanhol. A única alteração em relação a espanhol-abertura-1.06 é ui/series/SeriesLoadingOverlay.kt. Não limpa dados e preserva listas, progresso, chaves e assinatura. Instalador verifica hashes, faz backup e rollback; -Verificar não altera o projeto.

Validação: compilação de APKs Android e lint. Sem teste visual no emulador neste ambiente.
