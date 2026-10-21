# Urso na cor dos ícones e capas em ciclos de 30

Atualização cumulativa compatível com **IPTVPremium (11).zip** e com o atualizador Kids `fb94c04`. Inclui os mesmos recursos Kids/Legendados/PIN de seis dígitos da entrega anterior, caso ainda não estejam aplicados.

- O urso usa a mesma matiz do tema com acento metálico mais saturado, preservando sombras e reflexos. Corrige o azul claro/prateado da imagem enviada. Dourado original preservado; ajuste válido nos outros seis temas. O filtro de fundos e da sala de séries permanece suave, sem essa mudança de intensidade.
- Início da categoria: **30 capas antecipadas**, em seis grupos de cinco.
- Ao restarem **25 ou menos**, acrescenta **outras 30**, também de cinco em cinco. No gatilho de 25, termina com **55 à frente**. Não apenas completa novamente até 30. Só inicia outro ciclo depois de concluir o anterior e voltar ao gatilho.
- Exemplo: janela visível termina no item 10; baixa 10–39. A janela avança cinco posições; restam 25 e baixa 40–69. Agora há 55 futuras. Ao consumir outras 30, repete o ciclo. Usa a mesma regra para quatro ou cinco colunas, Filmes, Séries e Kids.
- Paginação incompleta aguarda mais registros sem aumentar o ciclo a cada grupo; cancelamento não avança um grupo incompleto. Fim da categoria limita os downloads ao conteúdo existente. Só a categoria ativa recebe essa antecipação.
- **Prioridade à navegação e às capas:** remove as preparações opcionais de detalhes, fundos e logos de filmes/séries e de episódios por foco enquanto o catálogo está aberto. Não dispara consultas de detalhes só por deixar uma capa focada. Dados e imagens de detalhes são preparados no clique, reutilizando o cache já existente; uma abertura sem cache pode demorar mais.
- O clique cancela a coroutine da fila antes de navegar para a abertura. Impede novos grupos ao fundo. Uma leitura HTTP que já esteja bloqueada pode aguardar o limite de rede para encerrar; não há garantia de interrupção instantânea do socket. Essa limitação é do carregador existente, que não foi substituído por uma nova biblioteca nesta alteração.

As capas distantes ficam em disco; só as próximas cinco são preparadas na RAM pela rotina existente. A contagem de 30/55 é a reserva à frente da janela visível, não um limite de arquivos de todo o cache.

Preservados: JPEG **90**, máximo **300 × 450**, RGB_565 opaco, cache único e rotativo com imagens **100 MB** dentro do armazenamento gerenciado de **350 MB**, Xtream **250**/M3U **2.000**, desenho/tamanho da grade e animação. Não modifica Gradle, credenciais nem assinatura. Os arquivos da navegação e da animação têm guardas de integridade. Política de pressão de memória continua podendo pausar a antecipação.

## Aplicação

Baixe `Atualizar-UrsoCapas.ps1` da revisão informada na entrega e execute no PowerShell, na raiz do projeto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:TEMP\Atualizar-UrsoCapas.ps1" -Projeto (Get-Location).Path
```

O script verifica hashes antes de escrever, aceita LF/CRLF/BOM, verifica o PNG por bytes, cria backup em `.navigation-backups/urso-capas-ciclos30-*` e restaura arquivos após falha. Pode ser reaplicado. Recusa alterações desconhecidas sem sobrescrevê-las.

## Validação

**60 testes locais passaram, sem falhas ou testes ignorados.** `assembleDebug` e `assembleDebugAndroidTest` concluídos. Instalador validado sobre ZIP (11) e Kids anterior, com LF e CRLF/BOM, reaplicação, integridade do PNG, preservação de Gradle privado, rejeição de base alterada e recuperação após falha de gravação.

Testes locais cobrem ciclos completos e parciais, gatilho de 25 e adição exata de 30, layouts com quatro/cinco colunas, cancelamento, leitura do mesmo cache no Kids com RAM vazia, PNG do urso renderizado nos sete temas e azul saturado, PIN legado/novo, Kids/Legendados e retorno de categorias. Mantidos testes de resolução, RAM e armazenamento. APKs de aplicativo e de instrumentação compilados; testes de dispositivo compilados não equivalem a execução em TV física.

Testes de revisão em `testes/kotlin`, com configuração externa opcional:

```powershell
.\gradlew.bat -I CAMINHO\urso-capas-ciclos30\testes\validar-ciclos30.gradle :app:testDebugUnitTest
```
