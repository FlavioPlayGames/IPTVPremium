# Compatibilidade Android 12 ou superior

Base: projeto enviado como IPTVPremium (28).zip em 16/10/2026. Esta atualização é específica para essa versão, sem substituir mudanças recentes do player, histórico, temas ou controle parental.

## Alterações

- `app/build.gradle.kts`: apenas `minSdk` de 34 para 31 (Android 12). `compileSdk` e `targetSdk` continuam 37.
- `PackageSigningCompat.kt`: consulta de assinaturas com API antiga em Android 12/12L e API tipada a partir do Android 13.
- `DevicePortalSync.kt`: usa a consulta compatível, mantendo exatamente o algoritmo da identificação do aparelho e a seleção dos certificados.
- `AppUpdateManager.kt`: usa consultas compatíveis e leitura limitada a 16.385 bytes sem `InputStream.readNBytes` (API 33). Respostas maiores que 16.384 bytes continuam rejeitadas. A validação de tamanho, hash, pacote, versão e assinatura do APK continua obrigatória.

## Aplicação

Abra o PowerShell na raiz do projeto, baixe `Compatibilizar-Android12.ps1` deste commit e execute:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Compatibilizar-Android12.ps1
```

O script valida todos os arquivos antes de escrever, aceita somente os arquivos conhecidos desta versão, preserva BOM e quebras de linha, cria backup em `.iptv-backups/android12-*` e pode ser repetido. `-SomenteVerificar` valida sem alterar.

Compile no seu computador com a configuração de assinatura original:

```powershell
.\gradlew.bat :app:assembleDebug
```

Os cinco APKs ficam em `app\build\outputs\apk\debug`. Instale apenas um, sobre a instalação atual, sem desinstalar. O universal atende às quatro arquiteturas. Se o aparelho já usa uma assinatura de release, selecione essa mesma configuração; um APK debug assinado com outra chave não substitui uma instalação release.

`local.properties`, `google-services.json`, chaves de assinatura, números de versão e preferências do aparelho não são alterados pelo atualizador. Nenhuma credencial do ZIP é incluída na entrega pública.

## Validação

- Compilação debug das quatro arquiteturas e universal concluída.
- Análise Android Lint `NewApi` e `InlinedApi` concluída sem erros com mínimo API 31.
- Assinaturas dos cinco APKs de validação verificadas com `apksigner --min-sdk-version 31`. Esses APKs de nuvem não são distribuídos; o APK final deve ser compilado com a assinatura original no computador do usuário.
- Manifesto dos cinco APKs: pacote original, mínimo API 31, destino API 37.
- Testes do código real do adaptador com simuladores de PackageManager em API 31, 32, 33, 34, 35, 36 e 37: seleção da API correta, mesmos objetos de certificado e flags, arquivo ausente e propagação de erro de permissão.
- Leitura limitada testada com respostas vazias, completas, no limite, maiores que o limite, fragmentadas e leitura sem progresso.
- Atualizador PowerShell testado: simulação, aplicação, backup, repetição, LF/CRLF/BOM, preservação das configurações locais e rejeição sem escrita de fonte alterada ou minSdk inesperado/duplicado.
- Não houve teste de instalação/reprodução em aparelho ou emulador Android 12 neste ambiente. Após compilar, testar abertura, listas M3U/Xtream, reprodução, sincronização Firebase, controle parental e atualização por APK no aparelho.

A alteração de compatibilidade não é uma redução de armazenamento nem uma otimização de desempenho. Esses trabalhos continuam separados.
