# Controle parental — visual e navegação

Base: projeto ZIP28 com a compatibilidade Android 12 já aplicada. Atualização de três arquivos de interface; nenhum recurso de imagem novo.

## Interface

- Página inicial com quatro cartões: bloqueio geral, canais adultos, filmes e séries; duas colunas em telas largas e uma em telas estreitas.
- Ícones vetoriais e contornos no tema ativo; estado de cada filtro visível.
- Ações menores e separadas para alterar PIN, bloquear novamente e desativar a proteção.
- Criação/alteração do PIN em formulário central com largura limitada; os campos exibem indicadores vazios e números mascarados, sem “OK para digitar”.
- Teclado numérico com fundo preto/cinza, contorno metálico, título na fonte do aplicativo e destaque no tema ativo. Toque, setas, OK/Enter e teclas numéricas continuam suportados.
- Confirmação de desativação em área própria; botão Voltar do controle retorna das opções internas aos controles.
- Foco retorna ao cartão anterior e não se perde quando o bloqueio geral/adulto ou uma categoria está sendo salvo. Salvamentos simultâneos são ignorados enquanto há uma gravação em andamento.
- Categorias em páginas de 12 opções, com identificação de livre ou bloqueada. Busca e bloqueio da lista inteira continuam disponíveis.

## Preservado

PIN existente, hash/limitação de tentativas, regras, liberação por sessão, identificação/licença, assinatura, Firebase, listas, player, temas e Android mínimo 12. O bloqueio por categoria continua mostrando o conteúdo com cadeado e exigindo PIN.

## Aplicar

Na raiz do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Melhorar-visual-parental.ps1
```

Depois compile:

```powershell
.\gradlew.bat :app:assembleDebug
```

Instale sobre o app existente com a mesma assinatura, sem desinstalar. O script valida os três arquivos antes de escrever, cria backup em `.iptv-backups/controle-parental-*` e é idempotente. `-SomenteVerificar` não altera arquivos. Arquivos privados e configurações de compilação não são modificados.

## Verificações

- Compilação debug das cinco variantes concluída.
- Android Lint NewApi/InlinedApi concluído sem erros para mínimo API 31.
- Testes do código real de entrada numérica: tamanho e caracteres do PIN, teclas físicas e numpad, apagar, consumo dos eventos e preservação de OK/setas/Voltar.
- Testes reais do instalador PowerShell: simulação, três arquivos alterados, backup, repetição, BOM/CRLF, arquivos privados preservados e rejeição de fonte desconhecida antes de escrever.
- A apresentação e a navegação com controle físico ainda precisam de validação no aparelho do usuário; não houve teste em TV/emulador neste ambiente.
