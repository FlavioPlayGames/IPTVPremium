# Legendas: auxiliar sem desconto de reação — r25

Base: fontes Android enviados em 12/11/2026. Apenas três arquivos de legendas são alterados. Não muda versão, Gradle, recursos, listas, histórico ou site.

O auxiliar passa a calcular a diferença entre o instante marcado no vídeo e o primeiro horário da legenda, sem subtrair um segundo. Arredonda o resultado para o segundo mais próximo; empates arredondam para longe de zero: +4,4 → +4; +4,5 → +5; −4,4 → −4; −4,5 → −5. Limite permanece em ±120 segundos. Ajustes manuais de 0,1/0,5/1/5 segundo e valores já salvos permanecem inalterados até uma nova ação do usuário.

A explicação do auxiliar foi atualizada. Os botões já tinham “+ Atrasar” e “- Adiantar”; esses sinais também foram adicionados às descrições de acessibilidade. Positivo atrasa a legenda; negativo adianta.

## Uso no Windows PowerShell

Depois de extrair este pacote, execute:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ".\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O projeto é a pasta Android com `app/build.gradle.kts`; `site-public-atual.zip` não é o destino desta alteração. Se sua pasta for diferente, ajuste somente `-Projeto`.

O script confere todos os três arquivos antes de alterar qualquer um; aceita a base enviada ou a própria correção já aplicada, incluindo LF/CRLF e BOM UTF-8. Faz backup dos arquivos alterados em `backups-codex/`, aplica e chama o `Instalar-TV.ps1` incluído. Esse instalador é a cópia do script enviado com o projeto: identifica a TV autorizada via ADB, compila `:app:assembleDesempenho`, escolhe o APK compatível e instala com `adb install -r`. Não desinstala, não altera a chave/versão e não tenta contornar recusa de assinatura.

Se houver várias TVs, informe `-Serial "identificador"`. O ADB padrão fica em `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`; pode ser substituído por `-Adb`. A TV precisa estar conectada e autorizada. `-SomenteAplicar` aplica sem compilar/instalar.

Uma base diferente é recusada para preservar alterações locais. Se o build falhar, os três arquivos corrigidos e o backup permanecem no PC; não é instalada uma saída antiga. Os fontes enviados permanecem preservados fora desta entrega.

O pacote não contém credenciais, configurações Firebase/local.properties ou APK. O build usa as configurações locais do usuário. Não foi executado build Android nem teste na TV neste ambiente; veja `validacao.txt` para os testes isolados realizados.
