# Validação r34 — 15/11/2026

- Compilação final :app:assembleDesempenho aprovada, com Kotlin, R8, lint vital e geração dos APKs. Os hashes do manifesto correspondem aos fontes finais compilados.
- Aplicação, repetição sem duplicar alterações, restauração byte a byte, proteção contra base divergente e contra sobrescrever edições posteriores aprovadas em PowerShell 7 e simulação da forma de arrays do Windows PowerShell 5.1. Parser dos scripts aprovado. Execução Windows real ainda depende do PC.
- Onze fontes Kotlin alterados ou adicionados. Nenhuma alteração de Gradle, versão mínima, assinatura, API, player, importação, persistência de dados ou site.
- Dez dependências de paleta/modo/arte, componentes existentes e envio manual são verificadas pelo aplicador. Todos os recursos/imagens existentes foram comparados byte a byte ao snapshot r33 e permaneceram idênticos. Não há imagem/recurso novo no pacote.
- As cores vêm dos componentes existentes (SettingsLook, CatalogStyle e SubtitleVisual). Imagens de fundo via SettingsBackdrop/CatalogThemeBackdrop já existentes; nenhum cenário desenhado, nova animação, blur ou biblioteca. Exceções explícitas: lixeira vermelha e status ativo verde.
- Inspeção dos fluxos de foco: entrada de Legendas com setas horizontais na TV e lista vertical em tela estreita; opções de Aparência com foco ao abrir e retorno ao menu; prévias abaixo acessíveis por foco e rolagem; lista ativa identificada abaixo do nome, independentemente da seleção navegada.
- Prévias reais existentes da Home, Kids e grade de capas preservadas, com tema/modo/opções selecionados. Não são simuladas por novas imagens.

Limite: não houve emulador nem TV física para uma captura real ou navegação interativa. A compilação e a inspeção do fluxo de foco não substituem esse teste; conferir foco, textos maiores e legibilidade nos temas/modos da TV após instalar. As pranchas de aprovação são propostas visuais.

O problema das capas genéricas recarregando não faz parte desta entrega. Também não há alteração no menu de sincronia do player nesta etapa de configurações.
