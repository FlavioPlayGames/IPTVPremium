# Controle parental — opção 3 aprovada

Atualização específica sobre o projeto ZIP28 com Android 12 e o visual parental já aplicado.

- Categorias de filmes e séries em grade: três colunas em telas largas, duas ou uma em telas menores.
- Busca e controle “Bloquear tudo” no topo; seleção de lista compacta quando há mais de uma.
- Rolagem contínua da grade, sem páginas Anterior/Próxima. A grade compõe apenas os itens necessários por meio de LazyVerticalGrid.
- Cartões com nome e ícone vetorial, sem repetir “Livre” em cada opção.
- Cadeado identifica bloqueio; contorno e fundo destacados identificam somente o foco.
- Rodapé mostra a quantidade de categorias bloqueadas e o salvamento automático.
- Regras existentes, PIN, assinatura, configurações e Android 12 preservados. Desativar “Bloquear tudo” restaura as escolhas individuais já existentes.

O atualizador altera somente ParentalControlsUi.kt, após validar a versão conhecida, e cria backup em .iptv-backups/grade-parental-*. Pode ser repetido; -SomenteVerificar não altera arquivos.

## Aplicar na raiz do projeto

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Aplicar-grade-parental.ps1
```

```powershell
.\gradlew.bat :app:assembleDebug
```

APKs: app\build\outputs\apk\debug. Instale com a assinatura original sobre o aplicativo atual, sem desinstalar.

## Verificações

Compilação debug das cinco variantes concluída. Instalador testado com simulação, aplicação, backup, repetição, BOM/CRLF, configuração privada preservada e rejeição de fonte desconhecida sem alterações. A navegação com controle e a apresentação em TV/celular precisam de validação no aparelho; não houve execução em TV/emulador neste ambiente.
