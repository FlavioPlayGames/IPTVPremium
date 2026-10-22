# Importação por acesso · 1.10

Pacote baseado exclusivamente no java.zip enviado em 22/10/2026. Modifica 10 arquivos Kotlin, adiciona 4 arquivos de produção e 2 arquivos de teste. Não contém APK, imagens, configurações privadas ou substituições do visual.

## Comportamento

- Padrão confirmado: Xtream 250 itens por lote e M3U 2.000. Os perfis de diagnóstico debug existentes continuam disponíveis; um perfil escolhido manualmente pode ter valores diferentes.
- Xtream: autentica e importa o índice de categorias, liberando a Home sem aguardar todos os títulos.
- Entrar em Filmes, Séries ou Canais prioriza a seção. Escolher uma categoria a coloca primeiro. Cada categoria é preparada numa geração temporária e publicada numa transação depois de concluída.
- Fila de até 7 categorias, com uma requisição de conteúdo por vez. A prioridade é reavaliada entre categorias e interrompe uma etapa em andamento quando muda a seção/categoria.
- Conteúdo já concluído é reutilizado; um checkpoint atômico, sem credenciais ou URLs, permite retomar após fechar o processo. Uma etapa incompleta pode precisar ser baixada outra vez.
- Após 2,5 segundos sem teclas, permite trabalho ocioso em Filmes → Séries → Canais. Pausa ao sair do primeiro plano ou abrir detalhes/reprodução. Toques e movimentos do controle não ficam bloqueados esperando rede.
- Ao fim de cada seção há uma conferência Xtream em segundo plano para incluir itens sem categoria e IDs que não aparecem no índice. Essa conferência lê o endpoint completo uma vez; os itens já salvos usam INSERT IGNORE. Ela não refaz downloads de capas/detalhes. Um provedor que ignora category_id usa o endpoint da própria seção como compatibilidade, permanecendo Xtream.
- Atualização mantém o catálogo ACTIVE anterior e só troca para a nova geração após concluir todas as seções. Uma resposta vazia/falha não apaga a biblioteca anterior. Uma alteração/exclusão da fonte impede publicar a resposta antiga.
- Preserva a correção enviada: a origem cadastrada decide o importador. Xtream convertido pelo site continua Xtream, com até quatro tentativas da preparação inicial; não cai silenciosamente em M3U.
- M3U nativa mantém o download/leitura integral do arquivo para descobrir as categorias e a gravação em lotes de 2.000. Não promete importação remota por categoria num formato que não oferece esse endpoint.

## Aplicar

Feche o Android Studio durante a compilação para evitar novamente o bloqueio de R.jar. Mantenha o emulador aberto. Execute em PowerShell:

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script = Join-Path $env:TEMP 'Preparar-Importacao-Por-Acesso-1.10.ps1'
Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/importacao-por-acesso-1.10-20261022/Preparar-Importacao-Por-Acesso-1.10.ps1' -OutFile $script
if ((Get-FileHash $script -Algorithm SHA256).Hash -ne '5adb1555c7fa92a6a7fddd49134fe108ff6c097c1c9b4e7797cc56e0017b048c') { throw 'Arquivo diferente do esperado.' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Emulador emulator-5554 -Testar
```

O instalador confere o pacote e todos os arquivos antes de alterar qualquer um. Se detectar outra edição, interrompe sem aplicar. Cria backup em `.navigation-backups/importacao-acesso-110-*`. Instala com `adb install -r`; não desinstala, não limpa dados/cache e não muda a versão 1.10 existente. Não restaura imagens removidas.

`-Verificar` apenas verifica compatibilidade. `-SomenteAplicar` aplica o código sem compilar/instalar. `-Testar` compila e executa os 17 testes incluídos no emulador informado. Sem `-Emulador`, apenas aplica e compila.

Diagnósticos: `diagnostico-importacao-acesso-110-compilacao.txt` e `diagnostico-importacao-acesso-110-testes.txt` na raiz do projeto.

## Validação e limites

- 9 testes JVM da fila/perfil executados.
- 9 verificações de transporte executadas com o repositório e parser reais: índice sem títulos, ordem, filtro/escape, lotes 250/250/1, 501 filmes preservados, URLs de reprodução, canais, filtro ignorado e falha sem fallback M3U.
- Compilação debug, AndroidTest e Kotlin release verificada. Os 8 testes de integração usam Room/DataStore reais no Android e verificam prioridade, interrupção, checkpoint, fonte alterada, atualização atômica e catálogo vazio. Estão compilados; não foram executados num emulador nesta sessão cloud.
- O arquivo recebido contém somente Java/Kotlin. Para compilar em cloud foram usados aliases locais dos 10 atlas Home e dos recursos Kids ausentes nessa cópia; esses aliases não entram no pacote e o projeto local deve conservar seus próprios recursos. Nenhuma alteração visual foi avaliada com esses substitutos.
- Ainda é necessário medir o tempo na TV e validar o comportamento do provedor real. Não há promessa de um número de segundos nem de menor tempo para importar o catálogo completo.
