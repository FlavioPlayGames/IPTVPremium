# Capas, Aparência e tecla Esc

Atualização cumulativa para `IPTVPremium (5).zip`, com ou sem o atualizador anterior de navegação.

## Alterações

- Remove da grade de filmes e séries o título, contador e divisor decorativo marcados na referência. As categorias e seus contadores na barra lateral continuam disponíveis.
- Mostra duas fileiras completas: 4 por fileira (8 capas grandes) ou 5 por fileira (10 capas). O tamanho considera a área disponível e a escala dos textos; títulos e informações ficam dentro das duas fileiras.
- Preserva a proporção 2:3 da arte e o cache/carregamento de capas.
- Reduz os cantos das capas e das bordas de foco para 4 dp.
- Faz Esc seguir o botão voltar em filmes, séries e canais: teclado primeiro, categorias depois, saída da seção quando já está nas categorias. A categoria anterior recupera o foco.
- Adiciona na Aparência uma prévia local da grade que troca imediatamente entre 8 e 10 exemplos, acompanhando o tema. Não exige playlist nem conexão.

## Aplicação pelo PowerShell do Android Studio

Execute, na raiz do projeto, o comando do chat para baixar e executar `Atualizar-Aparencia-Capas.ps1` desta pasta. O atualizador verifica todos os arquivos antes de escrever, preserva mudanças incompatíveis e cria backups em `.navigation-backups`. Inclui as correções anteriores de navegação e aceita arquivos com finais de linha LF ou CRLF. Não substitui configurações locais, tokens ou assinatura.

O patch registra as alterações em relação à atualização anterior. A origem completa dos arquivos atualizados está embutida no script.

## Validação

- `:app:assembleDebug` e `:app:assembleDebugAndroidTest`: concluídos.
- Cálculo de layout executado em 300 cenários de dimensões, colunas e escalas de texto: aprovado.
- Atualizador PowerShell: aplicação sobre o ZIP original e sobre a atualização anterior, backup, CRLF, repetição e recusa de conflitos verificados.
- Testes instrumentados de Esc e prévia compilados; execução e avaliação visual no controle/TV ainda pendentes.
