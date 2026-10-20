# Base restaurada: categorias, favorito, trailer e cache

Atualização feita **somente para o último `IPTVPremium.zip` restaurado enviado pelo usuário** (SHA-256 do ZIP: 2b30ccd1d8313dd51d6726329c9deb1522e3c51b9480a54b561b84fff59b1a0b). Este pacote não reaplica as mudanças dos antigos pacotes de rolagem/capas.

## Navegação e importação preservadas

Os arquivos de navegação, movimento, quadro de duas fileiras, lotes/conexões das capas, perfis de importação e tempo de abertura são validados pelo atualizador e mantidos byte a byte quando já correspondem à base. O algoritmo de decodificação das capas também foi preservado.

- Primeira entrada em Filmes/Séries coloca o foco na lista de categorias, mantendo Todos como prévia. Confirmar a categoria entra na grade; voltar dos detalhes preserva a restauração do item visitado.
- O coração sai do canto da capa e fica pequeno junto da nota, abaixo dela. Fica vazado quando não é favorito e preenchido quando é. Clique e manter OK pressionado continuam disponíveis.
- Importação padrão preservada: **Xtream em lotes de 250 e M3U em 2.000**. Perfis de diagnóstico de debug continuam disponíveis; o pacote não altera preferências salvas no dispositivo.

## Trailer dentro do aplicativo

Filmes, séries e prévias da filmografia usam uma Activity interna. O cabeçalho tem **somente o nome do título**, sem botão Voltar ou texto de preferência por português. Voltar no controle e ESC fecham o trailer; mensagens de erro/repetição ficam dentro da área do vídeo.

O resolver aproveita o trailer do provedor e busca alternativas no TMDB sob demanda, preferindo português. YouTube usa incorporação oficial, controles do player e solicitação de legendas em português quando disponíveis; vídeos diretos usam Media3. Não extrai streams do YouTube nem cria tradução/dublagem.

O WebView usa composição normal com aceleração da Activity, suporta a superfície nativa de vídeo e não força uma camada gráfica extra. Está configurado para não manter cache HTTP e limpar esse cache ao abrir/fechar. A reprodução real ainda precisa ser conferida na TV; restrições de incorporação e suporte do WebView podem impedir um vídeo específico.

## Armazenamento do cache — MB decimais

| Grupo gerenciado | Limite |
|---|---:|
| Capas, logos, fundos e fotos, compartilhados | 100 MB |
| Dados dos episódios em JSON | 25 MB |
| Respostas persistidas dos serviços de arte | 25 MB |
| Legendas salvas | 5 MB |
| Outros arquivos de cache, incluindo APK de atualização | 195 MB |
| **Teto combinado** | **350 MB** |

Não são reservas de RAM. Os limites da RAM da base permanecem iguais.

O controle cobre `cacheDir` e a pasta privada de legendas salvas. Não inclui APK instalado, bancos permanentes das listas, favoritos, preferências, credenciais nem arquivos internos que o Android/provedor WebView mantém fora dessas pastas. Portanto, não limita a linha de armazenamento total do aplicativo mostrada pelo Android a 350 MB; o teto é para os caches gerenciados acima.

Antes de gravar, cada escritor reserva espaço para o novo arquivo e seu temporário, considerando as gravações concorrentes. O orçamento inclui a substituição de um arquivo sem apagar a cópia antiga antes de reservar. Se não houver espaço recuperável, recusa essa gravação de cache em vez de ultrapassar sua cota. Arquivos em leitura/decodificação e em gravação são protegidos; os demais giram por LRU. Downloads sem tamanho conhecido reservam o máximo permitido e não podem escrever além da reserva.

O índice evita varrer todas as pastas a cada nova capa. Há reconciliação/limpeza quando a manutenção fica ociosa, migração das cotas antigas na primeira operação e remoção de temporários abandonados. Marcadores de sessão/tema são preservados na rotação. APKs de atualização também precisam caber na cota de outros caches; um APK maior que essa disponibilidade é recusado com mensagem.

## Aplicar no PowerShell

Abra o terminal na raiz do projeto restaurado, junto de `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Base-Restaurada.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/base-restaurada-categorias-cache/Atualizar-Base-Restaurada.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Confira a mensagem acima.' }
.\gradlew.bat assembleDebug
```

O atualizador valida todos os arquivos antes de escrever, aceita LF/CRLF/BOM, rejeita outra base ou modificações desconhecidas e preserva os arquivos de Gradle/configurações locais. Cria backup exato em `.navigation-backups/base-restaurada-*`, restaura as escritas se houver falha e não reaplica arquivos já atualizados. Os sete arquivos de navegação/importação incluídos como guardas são conferidos, não substituídos na instalação normal. O patch mostra somente alterações reais.

## Validação

27 testes passaram em rodadas focadas com Robolectric/Compose e JUnit: entrada nas categorias, coração abaixo do título/preenchimento, retorno por ESC, resolver/HTML/intent internos do trailer, cabeçalho sem avisos/botão Voltar, cotas por grupo e global, temporários, leitores protegidos, substituição cancelada, gravações simultâneas, migração das cotas antigas, preservação de favoritos/banco/marcadores e importação 250/2.000. Um servidor HTTP local confirmou download de uma imagem com reserva, commit sem temporário residual e nova leitura do disco sem outro download.

APK de depuração e APK de testes Android compilados. Os hashes dos arquivos de navegação/importação e o corpo do decodificador foram comparados com o ZIP restaurado. O PowerShell foi executado em instalações LF/CRLF, idempotência, backup exato, guarda contra alteração de navegação, rejeição da outra versão enviada e rollback após erro de escrita. Reprodução na TV, consumo real do dispositivo e FPS não foram medidos aqui.
