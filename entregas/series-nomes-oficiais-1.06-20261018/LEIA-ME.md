# Identificação por nomes oficiais e apresentação pt-BR — 1.06 / interno 8

Prioridade: ID TMDB ou IMDb quando disponível. Sem ID, descobre a produção pelo nome da lista e confirma nomes originais, traduções oficiais e títulos alternativos retornados pelo TMDB. Nunca cadastra uma exceção fixa para Armadilha, nem altera o ID de reprodução do provedor.

Depois de identificada, todas as fichas e temporadas são pedidas com language=pt-BR. Título textual, sinopse, gêneros e nomes de episódios usam os dados localizados disponíveis; na ausência deles, o conteúdo da lista é preservado. Mantém o layout dos cards como EPISÓDIO 01, etc. Não renomeia os itens persistidos da playlist nem o histórico.

Os nomes oferecidos às buscas complementares ficam na ordem original -> inglês dos EUA -> português brasileiro. Nomes idênticos não geram tentativas repetidas. Quando a lista só informa português e não tem ID, é necessário primeiro descobrir a obra para conhecer o original/inglês. IDs conhecidos evitam buscas por texto. O idioma de retorno da API é independente do idioma do termo pesquisado.

## Segurança da correspondência

Busca o nome normalizado da lista; considera resultados não adultos e ano exato quando fornecido. Correspondências diretas precisam ser únicas. Para traduções, exige ano, confere até quatro candidatos plausíveis e aceita somente correspondência única com um nome oficial da obra. Dois shows com o mesmo alias/ano são rejeitados; não escolhe o primeiro. Sem ano, não adivinha por tradução. Consultas de candidatos podem ocorrer em paralelo e reutilizam o cache/coalescência existente; não pesquisa todos os detalhes do catálogo.

## Caso real

TMDB devolve name=Armadilha, original_name=Die Falle, tradução en-US=The Trap e tradução pt-PT=A Armadilha. O buscador anterior rejeitava o resultado por não considerar essa tradução. O novo identificador foi testado diretamente contra a API real com A Armadilha (2026), sem ID fixo no algoritmo; reconheceu 283371 e retornou pt-BR. O ID aparece apenas nas provas e documentação do caso real.

## Instalação

Preparar-Series-Nomes-Oficiais-1.06.ps1 -Projeto CAMINHO -CapturarLog -Emulador emulator-5554

Instala por cima no emulador e salva diagnostico-series-nomes-oficiais-logcat.txt. Abra A Armadilha. Não é necessário excluir lista ou limpar dados. As chaves TMDB/Fanart existentes são preservadas. Compartilhamento ativo, player, temas, progressos e favoritos mantidos. Não publica atualização no servidor.

Cumulativo com 32 entradas (23 arquivos e nove exclusões antigas). Sobre a última entrega, modifica somente SeriesDetailsScreen.kt e acrescenta SeriesArtworkIdentityResolver.kt. Mantém versão 1.06/interno 8. Verificação completa de hashes, backup, rollback e idempotência. Rejeita versões desconhecidas dos arquivos em vez de sobrescrever mudanças do usuário. -Verificar não altera nada; -NaoCompilar aplica sem compilar.

## Validação

Cenários controlados de identificação de nome original, inglês, pt-PT, pt-BR, ano, ambiguidade, IDs, falhas e cancelamento. Prova contra API real do caso Armadilha. Compilação Android, APKs e lint; o emulador do usuário ainda precisa confirmar a apresentação visual e a reprodução. Logos: português -> neutro -> inglês; imagens válidas da lista continuam prioritárias, sem extrator de vídeo.
