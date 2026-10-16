# Aparência compacta e ícones originais

A tela de Aparência agora usa uma grade de quatro cores por linha e coloca os três tamanhos de texto lado a lado. Em telas com pelo menos 660 dp de largura disponível, a prévia aparece na coluna da direita; em telas estreitas, abaixo dos controles. A tela continua rolável para acomodar textos ampliados e formatos menores.

A prévia usa os recursos reais selecionados: fundo exclusivo, logo e seis ícones. Os botões têm indicação de seleção, cor de amostra, destaque de foco e semântica de botão/seleção para acessibilidade. As escolhas continuam sendo aplicadas e salvas imediatamente.

Os seis desenhos dourados originais são a base de todos os temas, incluindo atualizar e configurações. Apenas o acabamento RGB foi alterado nos arquivos prontos de cada cor, preservando o desenho, a transparência e a proporção. Não há filtro durante a exibição. Os quatro ícones principais conservam o formato 3:2 do original; os dois pequenos conservam o formato quadrado. As posições, dimensões dos componentes e animação de foco da Home dourada são usadas também nas outras cores.

Cada atlas tem 2304 × 1536 pixels: seis células de 768 × 768, com a tela 768 × 512 dos quatro ícones principais posicionada no início de sua célula. Cada recorte respeita seu próprio formato. Os intervalos restantes ficam completamente transparentes. Não há redesenho, recorte automático do contorno ou fragmentos das células vizinhas.

[Ver os seis ícones azuis corrigidos](previas/icones-originais-azul.webp). Essa imagem mostra os arquivos de arte, não uma captura da tela do aplicativo.

Os fundos exclusivos, logos, paletas, player e recursos dourados não foram alterados nesta correção. O aplicativo continua com os mesmos dados de acesso e configurações.

## Instalação

Baixe `Corrigir-aparencia-e-icones.ps1` e execute no PowerShell na raiz do projeto Android. O instalador é cumulativo e reconhece as versões anteriores compatíveis, inclusive a atualização dos fundos exclusivos. Ele verifica o pacote e todos os arquivos antes de escrever, faz backup dos arquivos substituídos em `.iptv-backups/temas-DATA-HORA` e não altera `local.properties`, `google-services.json`, versão, assinatura ou dados do usuário.

Para verificar sem modificar o projeto, acrescente `-SomenteVerificar` ao comando do instalador.

Depois execute `Gerar-APKs.ps1` e instale o APK adequado sobre o atual, sem desinstalar. Abra Configurações > Aparência e confira também a Home em cada cor.

## Validação

- `assembleDebug`: os cinco APKs compilaram.
- Os APKs incluem os novos atlas e as bibliotecas nativas correspondentes às suas arquiteturas.
- As 36 máscaras de transparência dos ícones coincidem pixel a pixel com as dos originais dourados redimensionados proporcionalmente para as células. Os 24 intervalos sob os ícones principais são totalmente transparentes.
- Na atualização a partir da entrega anterior, só três fontes Kotlin e seis atlas foram alterados. Logos, fundos e arquivos dourados foram comparados e permaneceram iguais.
- Instalador executado em PowerShell: instalação e atualização, backup, preservação dos arquivos privados, verificação sem escrita, execução repetida sem mudanças e rejeição de arquivos incompatíveis antes de alterar o projeto.
- A avaliação visual e a navegação com controle em uma TV física ainda precisam ser conferidas no aparelho do usuário.
