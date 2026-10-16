# Cores dos temas alinhadas aos logos

Correção aprovada a partir da referência rubi: azul, roxo, vermelho, rosa, verde esmeralda e ciano/turquesa agora usam o respectivo logo como referência para o acabamento dos seis ícones e a matiz dos controles da interface. O dourado original permanece intacto.

A alteração anterior deslocava as cores dos originais dourados, permitindo que diferentes áreas terminassem com matizes distintas. Esta correção alinha a matiz do material e ajusta sua saturação à do logo. Os reflexos e sombras continuam derivados do desenho original; a transparência, o formato, o recorte, as dimensões e o posicionamento dos ícones não foram alterados.

As imagens coloridas são arquivos WebP prontos, sem filtro durante sua exibição. Logos e fundos exclusivos não foram modificados. Não há alteração na tela de Aparência compacta, na reprodução ou nos dados do aparelho.

## Comparações das artes reais

Estas imagens reúnem o logo e os seis ícones de cada tema para conferir o acabamento. Não são capturas da interface e não representam mudanças no layout da Home.

| Tema | Logo e ícones |
| --- | --- |
| Azul | [Ver comparação](previas/cores-unificadas/blue.webp) |
| Roxo | [Ver comparação](previas/cores-unificadas/purple.webp) |
| Vermelho | [Ver comparação](previas/cores-unificadas/red.webp) |
| Rosa | [Ver comparação](previas/cores-unificadas/pink.webp) |
| Verde esmeralda | [Ver comparação](previas/cores-unificadas/emerald.webp) |
| Ciano / Turquesa | [Ver comparação](previas/cores-unificadas/cyan.webp) |

## Aplicar no computador original

Execute `Unificar-cores-dos-temas.ps1` na raiz do projeto Android. O instalador é cumulativo: inclui os ajustes anteriores de Aparência, ícones e fundos exclusivos e aceita as versões compatíveis das entregas anteriores.

O script confere os hashes do pacote e dos arquivos antes de alterar o projeto, preserva arquivos privados e guarda backup em `.iptv-backups/temas-DATA-HORA`. Para apenas verificar compatibilidade sem escrever, use `-SomenteVerificar`.

Depois execute `Gerar-APKs.ps1` e instale o APK adequado sobre a versão atual, com sua assinatura original, sem desinstalar. A escolha de tema continua em Configurações > Aparência.

## Validação

- Compilação assembleDebug dos cinco APKs concluída; bibliotecas nativas e atlas presentes em cada APK verificados.
- As 36 máscaras de transparência coincidem com os originais dourados na mesma escala. Os 24 intervalos dos ícones principais permanecem transparentes, sem fragmentos de células vizinhas.
- Antes da compressão WebP, o valor máximo RGB (componente de brilho HSV) de cada pixel visível foi preservado, mantendo a estrutura de reflexos e sombras. A compressão usa qualidade 94 e transparência sem perda.
- Paletas: sete temas, persistência, troca/retorno, preservação de alfa e identidade dos 69 tons originais no dourado verificados com a implementação Kotlin.
- Apenas seis atlas e a definição das seis matizes foram alterados em relação à entrega anterior. Logos, fundos, recursos dourados e geometria da interface permaneceram iguais.
- Instalador: instalação inicial e atualização, backup, verificação sem escrita, repetição sem alterações, arquivos privados preservados e rejeição de conteúdo incompatível antes de escrever.
- A conferência visual em uma TV ou celular físico ainda deve ser feita no aparelho do usuário.
