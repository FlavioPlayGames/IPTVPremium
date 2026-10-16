# Controle parental — estrutura compacta da referência e teclado do aplicativo

Base: ZIP28, Android 12, visual parental e grade da opção 3 já aplicados.

## Correção de estrutura

A tela de categorias deixa de ser conteúdo dentro do HubPane antigo. Ela passa a ocupar a área inteira, com cabeçalho e rodapé fixos e LazyVerticalGrid usando o espaço restante por weight(1f). Foram removidos o título externo duplicado, o segundo botão de retorno, a faixa inteira para escolher lista e o cálculo de altura fixa por tamanho da tela.

Em telas largas (a partir de 780 dp úteis), o cabeçalho tem uma única linha: logo existente do tema, título/lista, busca, bloquear tudo e Voltar. Em telas estreitas, as ferramentas passam para uma segunda linha. A lista é escolhida pelo nome no cabeçalho e abre um seletor sobreposto quando há mais de uma, sem ocupar uma faixa permanente.

Os cartões têm altura mínima 42 dp, ícones vetoriais e nomes em branco; o foco recebe fundo e contorno destacados, e o bloqueio recebe cadeado. Há ícones reconhecidos por nome para documentários, esportes, 4K, comédia, terror/suspense, adulto e animação, com símbolo genérico de filme/série para demais categorias. Os nomes e a ordem continuam vindo da lista do cliente; não são substituídos pelos exemplos da referência.

## Teclado

O campo de busca não é mais BasicTextField e não abre o IME do Android. Toque ou OK abre o AppKeyboard já usado no aplicativo, com letras, números, acentos, apagar, edição do cursor e Concluir. A consulta é atualizada durante a digitação. Concluir ou Voltar fecha somente o teclado e devolve o foco à busca. A troca de lista fecha o seletor e carrega as respectivas categorias.

## Preservação e aplicação

PIN, regras, liberação por sessão, limite de tentativas, Android mínimo 12, player, listas, Firebase, chaves e versão são preservados. Somente ParentalControlsUi.kt e SettingsHubScreen.kt são atualizados, após validação de ambos; backup em .iptv-backups/parental-referencia-*. Não há novas imagens ou dependências.

Na raiz do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Ajustar-parental-referencia.ps1
```

```powershell
.\gradlew.bat :app:assembleDebug
```

Instale com a assinatura original sobre o aplicativo existente, sem desinstalar. -SomenteVerificar valida sem alterações.

## Validação

Compilação debug das cinco variantes concluída. Android Lint NewApi/InlinedApi concluído sem erros para mínimo API 31. Instalador PowerShell testado com simulação, aplicação, backup, repetição, BOM/CRLF, arquivos privados preservados e rejeição de fonte desconhecida antes de qualquer escrita.

Não houve execução em TV/emulador neste ambiente. A aparência e os percursos de foco do controle precisam ser conferidos no aparelho, incluindo busca com o teclado do aplicativo, troca de lista, bloqueio individual, bloqueio geral e retorno aos controles.
