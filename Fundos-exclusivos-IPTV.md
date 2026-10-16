# Fundos exclusivos IPTV Premium

Seis cenários criados individualmente para os temas aprovados na segunda opção. O centro fica escuro para os ícones aparecerem com contraste, sem filtros de cor nas imagens.

| Tema | Cenário |
| --- | --- |
| Azul | Horizonte: montanhas noturnas e horizonte azul |
| Roxo | Órbita: espaço e arcos orbitais |
| Vermelho | Velocidade: painéis de metal e detalhes rubi |
| Rosa | Ateliê: arquitetura escura e iluminação rosa |
| Verde esmeralda | Obsidiana: rochas negras e luz verde nas fendas |
| Ciano/turquesa | Luz Polar: aurora e paisagem polar noturna |

O dourado original, os seis conjuntos de logos e ícones e as cores dos menus, incluindo o player, foram preservados. A atualização substitui os seis arquivos anteriores; não mantém cópias deles nos recursos do app.

## Qualidade e tamanho

Imagens em WebP, qualidade 94, na resolução nativa de 1672 × 941 pixels, sem reduzir dimensões ou ampliar artificialmente. As seis imagens passam de 1.883.678 para 1.342.126 bytes: redução de 28,75%. A qualidade e a leitura em uma TV grande ainda devem ser conferidas no aparelho.

Na compilação incremental local, os cinco APKs mantiveram o tamanho total anterior, apesar da redução dos recursos; a organização e o alinhamento do pacote também influenciam o tamanho final. O universal ficou em 290.951.313 bytes e o ARM64 em 135.994.259 bytes. Esta mudança não resolve o crescimento dos dados do usuário, que é uma tarefa separada.

## Aplicar

Execute Aplicar-fundos-exclusivos.ps1 no PowerShell na raiz do projeto Android. Ele é cumulativo e também instala a versão anterior dos temas se necessário. Aplicar-temas-completos.ps1 contém a mesma atualização.

O script baixa o pacote de artes por uma URL fixa, verifica o SHA-256 do ZIP e de cada arquivo antes de alterar o projeto, confere as versões de código compatíveis e recusa arquivos desconhecidos. Ele permite os seis fundos da versão anterior e os substitui pelos novos. O backup fica em `.iptv-backups/temas-DATA-HORA`, fora dos recursos Android. Não apague esse backup até conferir o resultado.

A verificação opcional, sem escrita no projeto, é `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Aplicar-fundos-exclusivos.ps1 -SomenteVerificar`.

Depois execute `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Gerar-APKs.ps1`. Instale o APK adequado sobre a versão atual, com a assinatura original, sem desinstalar. Escolha a cor em Configurações > Aparência.

Não são alterados: local.properties, google-services.json, versão do aplicativo ou assinatura. Um APK compilado aqui não substitui a compilação no computador original.

## Verificado

- Compilação assembleDebug concluída para ARM64, ARM32, x86, x86_64 e universal.
- Imagens dentro dos cinco APKs correspondem aos arquivos aprovados e compactados.
- Conjuntos de bibliotecas nativas correspondem às arquiteturas de cada APK; DEX igual ao build anterior.
- No projeto já com os temas, apenas seis recursos foram modificados.
- Instalador executado com PowerShell: instalação inicial compatível, atualização dos seis fundos, backup, verificação sem escrita, segunda execução sem mudanças, preservação de configurações privadas e rejeição de pacote/código/arte adulterados antes de escrever.
- Ainda não houve teste visual em uma TV ou celular físico.
