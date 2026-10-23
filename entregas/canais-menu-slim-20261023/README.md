# Canais — menu Slim, busca e carregamento

Mantém a versão 1.11 (13). Aplicar sobre a entrega anterior de capas/categorias/vídeo. O instalador aceita também a versão imediatamente anterior ao ajuste de foco. Inclui somente os fontes alterados e não adiciona testes ao aplicativo.

## Comportamento

- Menu na tela cheia seguindo a referência: fundo quase preto translúcido, tipografia sans-serif fina, linhas compactas, ícones de TV e cantos discretos. A coluna de Canais ocupa 30% da largura e 86% da altura útil, com limite de 220–360 dp para manter leitura em telas de tamanhos diferentes. O desenho mantém o mesmo estilo nos modos Claro e Escuro; a seleção usa a cor do tema.
- Aba lateral com o indicativo ≡ abre a coluna **Categoria** à esquerda. Esquerda também abre; selecionar uma categoria ou apertar Direita recolhe essa coluna em direção ao painel de **Canais**.
- No vídeo em tela cheia: OK ou setas Cima/Baixo/Esquerda abrem o menu. No menu, Cima/Baixo navegam. OK confirma; parar 3 segundos sobre um canal confirma na tela cheia. Sair da linha, continuar navegando, abrir categorias ou fechar o menu cancela essa espera. Direita no painel de canais retorna ao vídeo.
- Na prévia, navegar ou entrar numa categoria não altera a reprodução: apenas OK confirma um canal. Esquerda na lista retorna às categorias; Direita na lista vai à prévia. Voltar conserva os níveis de navegação.
- Apenas CH+/CH− fazem troca direta. As setas deixam de fazer troca imediata.
- A busca consulta o catálogo inteiro da categoria no SQLite, incluindo canais fora das páginas já carregadas; preserva favoritos, bloqueios parentais e ordenação. O teclado não perde foco para listas que recompõem durante a digitação. Continuar assistindo mantém sua ordem recente.
- Ícone e controles de formato de tela reutilizados do player de filmes: **Normal**, **Tela cheia** e **Ajuste manual**, com Zoom, Largura, Altura, Horizontal, Vertical e Restaurar. Mantém o player e a transmissão. Ajustes persistem entre entradas.
- Mensagem central na prévia preta enquanto o catálogo de canais é preparado. Em caso de erro, OK tenta novamente.

## Importação e cache

No Xtream, inclusive listas convertidas e cadastradas como Xtream, trocar/importar/atualizar uma lista prepara Filmes e Séries sem baixar os endpoints de canais. A conclusão de Canais é solicitada exclusivamente pela aba Canais. Categorias e canais são gravados numa etapa temporária e publicados juntos no catálogo ativo, somente após sucesso; uma falha ou cancelamento conserva a pendência para nova tentativa. A marca de conclusão fica no banco, vinculada à geração: voltar à Home, trocar de lista ou reiniciar o app reaproveita o que já terminou. A atualização de uma lista cria uma nova geração e deixa seus canais para o próximo acesso. Não muda o formato da lista nem usa M3U como fallback do Xtream.

Na M3U nativa, o provedor entrega um arquivo único com as seções misturadas: permanece uma única leitura compartilhada para evitar baixar o mesmo arquivo duas vezes. A interface e o Paging de Canais são abertos somente ao acessar a aba, aproveitando o banco já existente.

## Instalação pelo PowerShell

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/canais-menu-slim-20261023'
$script = Join-Path $env:TEMP 'Preparar-Canais-Slim.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Canais-Slim.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

O instalador valida todos os fontes antes de qualquer alteração e cria backup em `.live-slim-backups`, fora de app/src. Se houver alterações locais incompatíveis, interrompe sem sobrescrever. `-Testar` executa assembleDebug; não instala automaticamente. Instale depois o APK gerado sobre o app existente.

Verificações de compilação, busca SQL e importação estão em VERIFICACAO.txt. Não foi possível verificar o desenho e o controle remoto em TV física; a reprodução da referência deve ser conferida no aparelho.
