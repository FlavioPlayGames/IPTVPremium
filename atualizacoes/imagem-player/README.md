# Nitidez adaptativa e FSR 1 no player

Atualização para o projeto **IPTVPremium (8).zip**, com Media3 **1.11.1**. Adiciona o botão **Nitidez** imediatamente antes da engrenagem nos controles do player, tanto no layout largo como no compacto.

## Opções

- **Desativada:** mantém o processamento original do reprodutor, sem criar a cadeia de efeitos GL.
- **Leve · nitidez adaptativa:** um passe RCAS, mantendo o escalonamento padrão do player.
- **FSR 1 + RCAS · experimental:** ampliação EASU em um passe e nitidez RCAS em outro.

Os modos usam shaders, sem rede neural ou interpolação de quadros. A nitidez tem três intensidades: **Suave**, **Média** e **Forte**. O padrão inicial é desativado, com intensidade suave. A escolha é salva nas preferências.

Ao trocar o modo ou a intensidade, o mecanismo atual é recriado para inicializar ou remover corretamente a cadeia de efeitos do Media3. A posição de filmes e séries, o estado de pausa, a velocidade e as preferências de faixas são preservados. Pode ocorrer uma breve recarga; canais ao vivo retomam na posição ao vivo da fonte.

## Compatibilidade e limites

Disponível no **Exo Player/Media3**. O VLC mantém a imagem original e o painel informa que é necessário usar o Exo Player. Entrada SDR até 1080p; HDR, Dolby Vision, DRM e resoluções superiores desativam o efeito automaticamente. O FSR 1 exige OpenGL ES 3 e aceita vídeos de até 30 FPS quando essa informação está disponível. O modo Leve exige OpenGL ES 2 e suporte aos shaders no driver.

O EASU amplia até o tamanho de tela informado pelo Android, limitado a 1920 × 1080 e a 2× por dimensão, mantendo a proporção. Quando a entrada já atinge esse limite, aplica apenas RCAS. Não produz saída 4K com FSR nesta versão.

Falhas de processamento ou relatos significativos de quadros perdidos desativam a melhoria na reprodução atual e restauram o caminho original do Media3. A preferência salva permanece disponível para o próximo vídeo.

**Não foi medido o desempenho em Mali ou em uma TV Box física.** A opção FSR 1 é experimental: comece pelo modo Leve e compare no aparelho. O desempenho também depende do decoder, da resolução, do driver e da composição de vídeo do Android. A contagem de quadros perdidos é uma proteção, não uma medição completa do custo da GPU.

Os shaders adaptam as equações EASU/RCAS da AMD, com leituras explícitas de textura e domínio gama reversível para o SDR linear do Media3. A licença MIT e a referência estão no arquivo `app/src/main/assets/FSR1-NOTICE.txt` incluído na atualização.

## Atualizar pelo PowerShell

Abra o terminal PowerShell na raiz do projeto no Android Studio, onde está `gradlew.bat`:

```powershell
$arquivo = Join-Path $env:TEMP 'Atualizar-Imagem-Player.ps1'
Invoke-WebRequest 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/imagem-player/Atualizar-Imagem-Player.ps1' -OutFile $arquivo
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo -Projeto (Get-Location).Path
if ($LASTEXITCODE -ne 0) { throw 'Atualização não aplicada. Veja a mensagem acima.' }
.\gradlew.bat assembleDebug
```

O script confere todos os arquivos antes de alterar o projeto, aceita diferenças de CRLF/BOM e preserva alterações desconhecidas abortando a atualização. Cria backup dos arquivos existentes em `.navigation-backups/imagem-player-*`, verifica a gravação e restaura os originais se ocorrer falha durante a aplicação. Pode ser executado novamente sem duplicar a atualização.

O script acrescenta somente a dependência `media3-effect` ao `app/build.gradle.kts` local. O restante desse arquivo e a configuração privada do projeto são preservados; o arquivo completo não está no pacote publicado. O patch `alteracoes.patch` permite revisar as mudanças.

## Validação realizada

- Compilação de `assembleDebug` e `assembleDebugAndroidTest` concluída.
- Oito testes executados: preferências, limites, preservação da reprodução, rejeição no VLC, recuperação automática e botão nos layouts largo e compacto.
- Shaders compilados e executados com OpenGL ES por software: cores constantes, bordas, ampliação, composição dos dois passes e orientação da imagem.
- Atualizador PowerShell executado sobre cópias da versão enviada: aplicação, backup exato, CRLF, reaplicação, preservação do Gradle local, rejeição de versões diferentes e restauração após falha de gravação.

A reprodução e o desempenho em hardware físico ainda precisam ser verificados.
