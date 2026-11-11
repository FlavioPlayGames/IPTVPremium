# Histórico local e limpeza r9

Base: fontes Kotlin do java.zip atualizado, após r8. Versão, assinatura, configuração Gradle, recursos, pesquisa e política de capas preservados.

## Instalar na TV

Feche o Android Studio. A TV deve estar conectada e autorizada no ADB como antes. No PowerShell:

```powershell
$pasta = "$env:USERPROFILE\Downloads\historico-iptv-premium-r9"
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

Atualizar-TV encontra o Java do Android Studio quando JAVA_HOME não estiver configurado, confere os hashes de todos os fontes, arquiva os originais em Downloads\IPTV-restauracao-r9-HORARIO, aplica as alterações, compila desempenho e instala o APK da ABI da TV com `adb install -r`. Não desinstala, apaga dados ou executa a limpeza física novamente. Se a compilação falhar, os fontes novos permanecem para diagnóstico e a instalação não é iniciada.

Para aplicar somente os fontes: execute Aplicar.ps1 -Projeto CAMINHO. Para desfazer: execute Desfazer.ps1 -Backup PASTA_DA_RESTAURACAO e recompile. Desfazer restaura código, não desfaz ações realizadas no aplicativo.

## Funcionamento

- A próxima lista recebe primeiro Continuar (filmes e séries), depois Favoritos.
- Somente correspondências reais do catálogo ativo aparecem. Os itens ausentes permanecem guardados para outra lista, sem criar cartões de conteúdos indisponíveis.
- Os registros encontrados avançam para os IDs da lista ativa; não se cria uma cópia de progresso para cada lista antiga. A última transferência concluída fica como referência.
- Excluir todas as listas não apaga o histórico portátil. Uma nova lista procura os registros preservados.
- Favoritos removidos e conteúdos ocultados do Continuar não devem reaparecer por herança de uma lista antiga.
- Normal e Kids têm estados separados; os filtros parentais continuam no catálogo.
- O fluxo antigo de consultas repetidas e enriquecimento de histórico por TMDB foi substituído por leitura paginada local e cache das correspondências da geração ativa. O TMDB das capas permanece funcionando como antes.
- Títulos ambíguos, anos e IDs conflitantes não são ligados. Títulos em idiomas diferentes, sem IDs ou aliases confirmados em comum, podem permanecer guardados sem aparecer.
- Para séries, a correspondência do título permite abrir os detalhes enquanto os episódios do provedor de destino são resolvidos. O progresso só é aplicado ao ID real da mesma temporada/episódio; um atalho de série não inventa um ID de episódio nem mostra porcentagem falsa. Se os episódios consultados não contiverem a coordenada salva, esse progresso fica guardado.
- O registro antigo de identidades é lido como fonte de migração e mantido no armazenamento privado para recuperação. Não é o executor do fluxo novo. O progresso existente não é zerado.

## Limpeza e armazenamento

Foram removidas 12 funções privadas sem chamadas, quatro arquivos antigos de prefetch/download de capas e o resolvedor de aliases exclusivo da rotina antiga. Em SeriesAutoNext.kt somente nextSeriesEpisode foi removida; adjacentSeriesEpisode, usada no player, permanece.

A remoção de uma série do Continuar grava o histórico em lote. Os detalhes reutilizam uma projeção calculada fora da UI, e o JSON do histórico só é decodificado novamente quando muda.

Os limites de cache e sua rotação permanecem iguais: 100.000.000 bytes no cache gerenciado, 75.000.000 bytes para imagens e 125 capas por tipo. A meta de 400 MB para o aplicativo instalado continua sendo uma meta a medir na TV; nenhum aumento desses limites foi feito. Dados de usuário não são descartados para caber nela.

## Validar na TV

1. Reproduza um filme e um episódio na lista A, pare antes do fim e marque favoritos.
2. Ative uma lista B de outro formato e confira primeiro Continuar, depois Favoritos. Os IDs numéricos não precisam ser iguais entre listas.
3. Confira que itens ausentes de B não aparecem. Ative uma lista C que os contenha e confirme que retornam.
4. Avance a reprodução em B/C e confira que a próxima lista recebe a posição mais recente.
5. Remova um item do Continuar e desmarque um favorito; confirme que não voltam na troca seguinte.
6. Teste o perfil Kids separadamente. A exclusão de todas as listas também é coberta por testes JVM; faça na TV apenas se desejar esse fluxo, sem apagar dados do aplicativo.

Para medir, execute depois de reproduzir uma troca:

```powershell
$pasta = "$env:USERPROFILE\Downloads\historico-iptv-premium-r9"
powershell -ExecutionPolicy Bypass -File "$pasta\Coletar-Diagnostico.ps1"
```

O arquivo filtrado fica em Downloads\IPTV-historico-r9.txt. Se necessário, acrescente -AoVivo antes de trocar a lista e encerre com Ctrl+C. Só contadores/tempos com formato permitido são exportados. Não envie pastas de recuperação, listas completas, credenciais ou chaves de assinatura.

Veja VALIDACAO.md para os testes realizados e suas limitações.
