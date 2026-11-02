# Revisão de fontes — 02/11/2026

Base: `java.zip` SHA-256 `3f9da8530971da6f9518c882dbe5be5703e8e7b6be177a5f1a864048e6de2c84`. Os 267 fontes Kotlin enviados coincidiam com a última base de trabalho; a revisão entrega 270 fontes ativos, após cinco adições e exclusão de dois marcadores vazios.

## Cobertura e evidência

A revisão inclui inventário de todos os fontes, compilação integrada, análise Android Lint do projeto de validação e inspeção dos caminhos de navegação, tema, Paging, cache, importação, repositórios, histórico, reprodução e cancelamento. O projeto de compilação usa os recursos e dependências já existentes; o upload novo contém somente a pasta Java. Os vídeos foram examinados como evidência do comportamento relatado. Anexos e comentários de código não foram tratados como novas instruções do usuário.

As mudanças são concentradas nos problemas encontrados. Não houve atualização em massa de bibliotecas, alteração de protocolo do fornecedor ou refatoração geral do player. Compilação e Lint cobrem referências/tipos e regras estáticas; não demonstram ausência de qualquer defeito em execução.

## Problemas corrigidos

| Área | Constatação | Correção |
|---|---|---|
| Home | Arte principal lia a paleta global temporária Kids | Paleta e contraste da Home principal independentes da paleta Kids, inclusive prévia/modo claro |
| Grade Kids | Geometria reservava metadados de uma linha e padding de 2 dp, mas o cartão tem título + nota e a grade usa 4 dp | Mesma geometria real do catálogo normal; duas linhas completas com arredondamento conjunto em pixels |
| Canais | Âncora invisível podia continuar focável; atualização de categorias podia rolar de volta à categoria confirmada e remover o item navegado | Âncora limitada à entrega de foco, reposicionamento somente na entrada solicitada, recuperação do último solicitante de foco |
| Voltar | Estado dependia do foco físico; latch podia ficar preso sem key-up ou perder o key-up ao remover a tela | Recuperação pela área lógica, nova pressão aceita, key-up consumido após transição e handlers percorridos por snapshot |
| Metadados | Cache das fichas usava ID da edição/provedor; nomes com qualidade/idioma impediam correspondência | Cache público compartilhado por identidade da obra; IDs e dados de reprodução continuam por edição |
| Capas ausentes | Carregador recebia só a URL, sem nome/ano para recuperar a imagem | Cartões nas duas interfaces passam nome, ano e IDs externos ao fallback; consulta apenas enquanto visíveis |
| Fornecedor sem ficha | Retorno vazio encerrava a abertura sem aproveitar o nome/ano disponíveis no catálogo | Semente local + enriquecimento; ficha pública existente pode ser reutilizada antes da consulta de outra edição |
| Concorrência | Locks de filmes eram divididos em 64 posições; títulos diferentes podiam esperar pela mesma posição | Reutilização do coordenador por chave exata, com liberação após o último participante |
| Compose | Duas leituras diretas de StateFlow dentro da composição | Estado observado via collectAsState |
| Memória/layout | Callback usava literal 80 e painel parental tinha BoxWithConstraints sem usar suas medidas | Constante oficial de trim e Box simples |
| Código antigo | Ramos de Kids dentro do navegador normal e funções sem chamadas permaneceram após a reconstrução | Retirados 52 ramos, 18 funções, atlas privado sem uso, efeito vazio e dois arquivos de tela vazios |
| Fontes | 138 cópias históricas de fontes estavam sob java, somando 8.610.955 bytes | Retiradas da árvore ativa e preservadas no backup local do instalador |

A análise inicial Lint apontou 10 erros e 164 avisos. Parte dos erros eram seis ocorrências do mesmo falso positivo `RestrictedApi` nos overrides públicos de `Activity.dispatchKeyEvent`: a ponte herdada do AndroidX tem anotação de escopo de biblioteca. Foi mantido o callback público necessário ao controle remoto, com supressão estreita e comentário nesses dois métodos. Não foi desativado o detector globalmente.

## Metadados e identidade

O TMDB fornece os dados públicos, com identificação pelo IMDb quando o ID estiver disponível, conforme resposta explícita do usuário. A fonte da avaliação continua correta. Qualidades/idiomas reconhecidos não fazem parte da chave da obra; ano, tipo e IDs externos continuam distinguindo filmes, séries, continuações e remakes. Números de títulos como `1917` e `Blade Runner 2049` são preservados.

Sem ano ou ID externo não se compartilha cache somente pelo nome. A resolução pode procurar um resultado inequívoco, mas não usa popularidade para escolher homônimos. Dados de catálogo e títulos duplicados não são excluídos. Episódios, temporadas do fornecedor, URLs de reprodução, favoritos e histórico não entram no cache público compartilhado.

RAM: 128 entradas/8 MiB, TTL de seis horas, misses de um minuto e coordenação de requisições sobrepostas. Cancelamento não vira falha permanente. Disco: respostas públicas de até 1 MiB por arquivo sob a cota já existente de respostas de 5 MB, com reserva/rotação e validade de seis horas. A busca de capas ausentes tem dois slots, respeita abertura/importação e é cancelada com a visibilidade do cartão.

## Avisos e limites mantidos

A análise final passou: **zero erros, 167 avisos e cinco sugestões**. Os avisos incluem 75 sugestões KTX, 31 recursos sem referência, 18 usos de medidas de Configuration, 12 sugestões de catálogo de versões, nove convenções de Modifier e alertas específicos de plataforma. Cinco recursos Kids antigos passaram a ser identificados como não referenciados após a limpeza; não fazem parte da navegação ativa. Os avisos de estilo, recursos não referenciados e compatibilidade devem ser distinguidos de defeitos funcionais. Não foram removidos recursos por nome usando apenas a lista do Lint: existem seleções dinâmicas de artes e o novo upload não inclui `res`. Também não foram alterados cadastro de dispositivos, identificadores usados no portal, orientação de vídeo ou versões de dependências por uma sugestão estática.

Os gestores que armazenam `applicationContext` foram conferidos: essa referência não retém uma Activity. O novo cache público guarda somente diretório e gestor de cota, sem Context estático. As operações de rede/decodificação continuam fora da thread de UI, com cancelamento e limites existentes. Não foram encontrados conflitos de declaração que impeçam a compilação.

A compilação não mede FPS, PSS ou foco de uma TV física. Os testes de host usam respostas públicas simuladas e não acessam o fornecedor particular. O teste de API externa não pôde ser concluído pela rede deste ambiente; a configuração TMDB existente foi detectada, sem exportar seu token. Falhas de rede, credencial ausente e obras sem correspondência permanecem tratadas pelo fallback.

## Regressão na TV

1. Entrar/sair do Kids a partir da Home em cada paleta principal e no modo claro; conferir ausência de recoloração da Home.
2. Em filmes e séries Kids, descer a partir da segunda linha e subir novamente, inclusive segurando a seta; conferir avanço de uma linha de cinco capas.
3. Em Canais normal/Kids, navegar várias categorias sem OK, aguardar, usar Voltar; conferir foco visível e saída sem confirmação extra. Repetir em Compacto/Duas colunas, busca, vídeo e tela cheia.
4. Abrir duas edições da mesma obra; comparar ficha e conferir que o link de cada edição e os episódios próprios continuam corretos.
5. Abrir categoria com capas vazias, sair rapidamente e voltar; conferir cancelamento sem travar a navegação, preenchimento das capas e persistência da ficha após reiniciar.

O instalador valida toda a base antes de modificar, conserva originais e bloqueia restauração que sobrescreveria alterações posteriores. Os resultados dos testes acompanham esta entrega.
