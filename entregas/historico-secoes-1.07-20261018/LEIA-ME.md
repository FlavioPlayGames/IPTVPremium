# Histórico por seção — 1.07 / interno 9

Filmes reconcilia somente filmes; Séries, somente séries. O trabalho começa ao entrar na respectiva seção e é cancelado ao sair. A recuperação do histórico antigo de versões anteriores também segue essa divisão, inclusive as consultas de episódios que antes começavam na abertura. A abertura da Home não aguarda reconciliação ou pesquisas de nomes alternativos. Os dados locais próprios da lista continuam disponíveis.

O conjunto de trabalho parte dos Favoritos e Continuar assistindo. Fatos relacionados de remoção/conclusão são preservados para não recuperar favoritos removidos nem progresso antigo. Histórico concluído não relacionado não gera busca. Mantém IDs reais, nomes originais/inglês/espanhol/português, preferência pela primeira variante não legendada e proteção contra títulos ambíguos.

O banco retorna candidatos por nome/aliases ou IDs, em páginas de até 256, em vez de materializar todos os filmes e séries para comparação em Kotlin. Padrões com classes de letras toleram acentos, caixa e marcas de qualidade sem transformar nomes chineses ou cirílicos em buscas genéricas; a identificação estrita continua validando nome, ano e IDs antes de vincular. Cada seção conserva seu cache independente na mesma geração; trocar lista/geração invalida o vínculo antigo.

As pesquisas remotas começam depois da etapa local e de 750 ms para priorizar a primeira tela. Filmes não carrega episódios. Séries busca episódios somente das séries em Continuar assistindo que precisam confirmar temporada/número no provedor de destino. Uma série sem episódio correspondente continua como favorito de recuperação conforme a regra existente. Cancelar ao sair da seção não aplica o bloqueio de nova tentativa de um minuto.

Após adicionar/trocar lista, a tela de carregamento mostra Carregando seu histórico e explica que o aplicativo pode ser usado normalmente. O aviso dura dois segundos; depois libera a Home sem aguardar o histórico. A reconciliação segue por seção ao entrar em Filmes/Séries, em segundo plano. Abertura normal não acrescenta essa espera. A importação inicial do catálogo e a sincronização de licença da central seguem necessárias; esta correção reduz o trabalho adicional dos vínculos, não promete tempo total abaixo de dez segundos.

Cumulativo: mantém acesso local validado, cache de catálogo, quatro/cinco capas, foco neon, teclado e regra de miniaturas existentes. Mantém 1.07/interno 9 e aceita a saída x86 única. Instalador com hashes, backup, rollback e -Verificar. Não apaga listas, chaves, histórico ou favoritos.

Validação: regras JVM, consultas SQLite, instalador, APKs, compilação e lint. A nuvem não dispõe de TV/emulador; os nove testes nativos são compilados e podem ser executados pelo instalador. Latência e consumo real precisam ser medidos no aparelho.
