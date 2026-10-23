# Teste 1 — Catálogo nativo progressivo (1.12-T1)

Primeiro experimento da comparação, inspirado na arquitetura de OwnTV/OwnTV_Core. Implementação própria, sem incorporar o código ou as dependências desses projetos. O foco deste teste é **Xtream**. O teste na TV é visual e não exige ADB nem conexão da TV com o computador.

## O que muda de método

O catálogo Xtream passa a ter um ciclo de vida independente das telas: autenticação e categorias liberam a Home; os itens de Canais, Filmes e Séries são sincronizados em seguida, com até duas fases de rede e um escritor. Ao abrir uma seção, dados salvos são usados imediatamente. Na primeira carga, basta o primeiro lote confirmado ou a confirmação de que a seção está vazia; não se espera a seção inteira.

Fluxo novo: Reader HTTP → parser de tokens → registro do banco → consulta paginada da interface. Não passa pelo enquadramento textual, JSONArray, PlaylistContent e XtreamCatalogWriter antigos para importar Xtream. Detalhes e episódios continuam sob demanda.

Uma atualização reutiliza a identidade estável dos registros. A comparação é exata e limitada aos itens do lote, sem manter um mapa de todos os títulos na RAM. Insere novos, altera os que mudaram e não regrava o conteúdo dos inalterados. Se somente a ordem muda, atualiza apenas a posição. Um registro compacto de visita é escrito separadamente para permitir remoções seguras.

Usa lotes limitados a 800 registros e a uma estimativa de texto, não a regra antiga de 500. O teto de 800 também respeita o limite de parâmetros de versões antigas do SQLite. Há uma fila de um lote entre leitura e escrita. Um erro ou cancelamento encerra ambos os lados.

Não impõe Filmes primeiro nem espera pela abertura de uma aba para começar a importar as outras seções. O temporizador de ociosidade/carência do fluxo anterior não inicia atualizações Xtream neste teste. Uma seção interrompida pode ser retomada por nova leitura e comparação; seções já concluídas não são baixadas novamente nessa retomada. Atualizar lista solicita uma nova passagem por todas as seções.

Se a resposta completa falhar, tenta categorias. As remoções são limitadas ao escopo validado; a recuperação por categoria preserva itens sem categoria que ela não pode verificar. Resposta inesperadamente vazia não apaga um catálogo anterior. Uma falha em um tipo não descarta os outros. O estado parcial/falha aparece na interface.

As telas consultam o banco enquanto a sincronização avança. Contadores são atualizados de forma espaçada e sem acionar novamente o bloqueio de carregamento. Capas visíveis podem baixar durante a sincronização; antecipação fora da tela e trabalho opcional de histórico aguardam. Cache das capas em RAM/disco permanece disponível.

## Isolamento para comparar e voltar

- Mantém applicationId e assinatura do projeto. Versão visível: **1.12-T1**, código mínimo 14, respeitando APP_VERSION_CODE maior.
- Cria **playlist_cache_native_test1.db** para o catálogo experimental. Não migra nem apaga playlist_cache.db, usado pela versão anterior.
- Usa uma pasta separada para fragmentos M3U do teste, evitando que a manutenção alcance os fragmentos do catálogo anterior.
- Fontes cadastradas, conta, favoritos, histórico, avaliações e cache de imagens continuam compartilhados. O isolamento é do catálogo, não uma segunda instalação do aplicativo.
- Não desinstale o app e não use Limpar dados. Isso apagaria os dados de ambas as versões.
- M3U mantém seu importador anterior, usando o banco de teste. Não deve ser usada para julgar este experimento Xtream.

## Aplicar e gerar o APK

Aguarde uma compilação anterior terminar e execute no PowerShell do computador:

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/teste-1-catalogo-nativo-20261023'
$script = Join-Path $env:TEMP 'Preparar-Teste-1.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Teste-1.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O script valida o ZIP e todos os arquivos antes de escrever, aceita as fontes conhecidas dos pacotes anteriores e recusa alterações desconhecidas. Guarda backup dos arquivos alterados em .native-test1-backups, fora de app/src. Reaplicar não cria um backup novo quando nada mudou. Não toca nas configurações privadas do projeto.

A opção -Testar gera o APK debug no projeto local. Instale como costuma fazer, por cima do app atual. Não há necessidade de conectar a TV ao computador. Confirme a versão **1.12-T1** antes da comparação.

## Roteiro visual na TV

Use a mesma lista Xtream e a mesma conexão dos testes anteriores. Ao instalar, o primeiro acesso prepara uma vez o banco de teste; não precisa recadastrar a lista.

1. **Primeira abertura:** observe quanto demora para aparecer a Home e se o controle responde. A Home pode abrir antes de todo o catálogo estar pronto.
2. **Primeiro uso:** abra Filmes, Séries e Canais. Observe quanto demora até haver conteúdo utilizável e se a navegação engasga. Capas e itens podem continuar aparecendo durante a preparação.
3. **Fim do catálogo:** na Home ou em Filmes/Séries, um aviso discreto mostra a preparação e as quantidades recebidas. Quando desaparecer sem aviso de falha, as três seções terminaram. Isso mede uma coisa diferente de apenas abrir a Home rápido.
4. **Reabertura:** depois de terminar, feche e abra novamente. Depois repita reiniciando a TV. O catálogo salvo deve continuar disponível, sem repetir a primeira importação completa.
5. **Troca de lista:** se tiver duas listas Xtream, espere cada uma terminar sua primeira sincronização e alterne usando Usar lista. Verifique se reaproveita o catálogo da lista escolhida.
6. **Atualização:** use **Atualizar lista em Minhas listas**. A navegação deve continuar possível com o catálogo anterior enquanto os dados são atualizados. O ícone de atualização da Home tem outra função: ele limpa o cache de experiência e reinicia o app; não o use para este passo.

Se aparecer “Parte do catálogo não foi concluída…”, anote em qual seção faltou conteúdo. Não considere um catálogo parcial como importação completa e rápida.

Basta responder algo como:

| Observação | Resultado na TV |
|---|---|
| Home | abriu mais rápido / igual / mais lento |
| Filmes | espera aproximada e se engasgou |
| Séries | espera aproximada e se engasgou |
| Canais | espera aproximada e se engasgou |
| Navegação enquanto prepara | fluida / com travadas |
| Fechar e reabrir | manteve o catálogo / carregou de novo |
| Atualizar lista | manteve o uso / bloqueou |

Não precisa medir milissegundos nem enviar logs. Se for conveniente, pode usar o relógio do celular para comparar a espera. A decisão entre os métodos será tomada pelo resultado na sua TV; testes no computador não substituem essa comparação.

## Desfazer

O backup de código permite voltar exatamente aos arquivos anteriores. Baixe o restaurador e gere outro APK:

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/teste-1-catalogo-nativo-20261023'
$undo = Join-Path $env:TEMP 'Desfazer-Teste-1.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Desfazer-Teste-1.ps1" -OutFile $undo
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $undo -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Instale o APK novamente, por cima. Ele volta a ler o banco anterior. O restaurador confere os arquivos antes de sobrescrever, valida os backups e remove somente os fontes adicionados pelo teste. Se você editar arquivos depois de aplicar o teste, ele recusa restaurar automaticamente esses arquivos para não perder seu trabalho. O banco experimental permanece no armazenamento, sem ser usado pelo código anterior.

## Referências e validação

Referências examinadas: https://github.com/ahXN00/OwnTV e https://github.com/ahXN00/OwnTV_Core. Esta entrega não é uma compilação do OwnTV nem sua reprodução integral: é o primeiro experimento de substituição do núcleo do IPTV Premium.

Verificações externas, compilação e limitações estão em VERIFICACAO.txt. Nenhum teste ou adaptador de JVM acompanha o APK. Nenhuma promessa de tempo de importação em segundos é feita antes do teste visual na TV.
