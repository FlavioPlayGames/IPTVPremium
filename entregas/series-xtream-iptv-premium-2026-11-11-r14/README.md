# IPTV Premium r14 — séries e prioridade da importação

Atualização sobre a **r13**. Inclui a correção de capas sazonais em Xtream/M3U, reutilização do cache de séries e a comparação da importação Xtream com o `java.zip` enviado.

## Resultado da comparação Xtream

Os lotes, perfis de banco, cliente HTTP, fluxo progressivo e ativação do catálogo já coincidem com a referência enviada. Foram preservados, assim como os novos campos de capas e IDs externos. Consulte `COMPARACAO-XTREAM.md` para as diferenças e os hashes de igualdade.

A r14 pausa/cancela consultas de capas e seus metadados que estejam em andamento quando começa uma importação, retomando ao finalizar. Isso estende a prioridade já existente e evita essa disputa por recursos; o ganho real na TV ainda precisa ser medido. Não se afirma que configurações antigas foram restauradas quando elas já eram iguais.

## Correções de séries

- Reutiliza capa TMDB geral já guardada antes de repetir uma URL de provedor indisponível.
- Quando existe temporada assistida, uma capa válida do provedor pode aparecer imediatamente, mas o fluxo continua a busca da capa da temporada. Antes, podia retornar cedo e nunca concluir essa etapa.
- Capa sazonal já conhecida é recuperada diretamente; se o bitmap foi removido da RAM, pode baixar a URL sazonal conhecida sem esperar nova tentativa na URL ruim do provedor.
- A fila de antecipação também completa a escolha de temporada, em vez de parar na capa geral válida do provedor.
- Uma associação provisória da série a outra lista pode informar a temporada conhecida mesmo antes de mapear o ID do episódio. Isso muda somente a apresentação da capa, sem liberar retomada por um ID não confirmado.
- A seleção visual considera o último registro assistido conhecido, inclusive concluído; um próximo episódio somente enfileirado, ainda não assistido, não escolhe sua temporada.
- Se não houver temporada conhecida ou capa sazonal disponível, permanece a alternativa geral válida. Não usa capa de outra temporada por aproximação.

Os dois formatos e os catálogos normal/Kids usam esse fluxo. Mantidas as correções de filmes da r13, os limites de cache/rotação e as imagens de até 300 × 450. Não houve alteração de versão, assinatura, importador de dados, esquema do banco ou orçamento de armazenamento.

## Instalar na TV

Extraia em `Downloads\series-xtream-iptv-premium-r14`. No PowerShell do Android Studio:

```powershell
$pasta = "$env:USERPROFILE\Downloads\series-xtream-iptv-premium-r14"
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O aplicador verifica a base r13 antes de alterar os quatro fontes e guarda recuperação externa em `Downloads\IPTV-restauracao-r14-...`. O instalador compila com as configurações/assinatura do seu projeto e instala com `adb install -r`. Não inclui APK, credenciais ou listas.

## Medir a importação/atualização Xtream

Antes de atualizar a lista na TV:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\series-xtream-iptv-premium-r14\Coletar-Importacao.ps1" -AoVivo
```

Com a coleta ativa, atualize a lista Xtream uma vez, espere terminar e encerre com Ctrl+C. Envie `Downloads\IPTV-importacao-r14.txt`.

O coletor é somente leitura e também reconhece os registros de importação que já existiam na r13. Exporta somente campos previstos, contadores, estados e tempos. Não exporta nomes de listas/conteúdos, URLs, credenciais ou corpos de resposta. Em modo ao vivo, guarda o perfil de banco mais recente disponível e acompanha os eventos seguintes.

## Verificar as capas

Abra séries com histórico nas duas listas, confira se a capa corresponde à temporada do último episódio assistido e repita a saída/retorno ao catálogo. A seleção da temporada pode atualizar depois que o histórico da lista ativa for publicado.

Se continuar lento ou usar capa geral indevidamente, execute `Coletar-Diagnostico.ps1 -AoVivo` antes de reproduzir o caso e envie `Downloads\IPTV-capas-r14.txt`, informando a série e a temporada esperada. O resultado visual ainda precisa ser confirmado na TV.

Para restaurar os fontes anteriores e reinstalar: `Desfazer.ps1 -Backup "PASTA_DA_RESTAURACAO" -Instalar`, usando o caminho impresso ao aplicar.
