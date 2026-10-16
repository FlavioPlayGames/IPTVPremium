> Acabamento atualizado: [cores dos seis temas unificadas aos logos](Cores-unificadas-IPTV.md). O instalador Aplicar-temas-completos.ps1 inclui esta correção.

> Correção mais recente: [Aparência compacta e ícones originais](Aparencia-e-icones-IPTV.md). O instalador Aplicar-temas-completos.ps1 já inclui essa correção.

> Atualização: o instalador agora inclui os seis fundos exclusivos da segunda opção. Consulte [Fundos-exclusivos-IPTV.md](Fundos-exclusivos-IPTV.md) para imagens, medidas e validação atuais.

# Temas completos do IPTV Premium

Sete opções em Configurações → Aparência: Dourado (padrão), Azul, Roxo, Vermelho, Rosa, Verde esmeralda e Ciano/Turquesa. A escolha é aplicada imediatamente e permanece salva ao reiniciar o aplicativo.

As cores de bordas, ícones desenhados, seleção, títulos destacados, gradientes e controles acompanham o tema nas telas de canais, filmes, séries, detalhes, listas, teclado, configurações, carregamento, assinatura, acesso, atualização e menus do player. O vídeo, as capas e a cor de legenda escolhida pelo usuário não recebem recoloração.

## Artes próprias

Cada cor adicional tem fundo, logo e atlas de seis ícones desenhados especificamente para ela. O atlas separa canais, filmes, séries, listas, atualizar e configurações. São arquivos WebP próprios; não se usa filtro de cor ou ColorFilter para transformar as imagens douradas.

As artes novas conservam a resolução gerada: fundos de 1664 × 941 e logos/atlas de 1536 × 1024, com transparência nos logos e ícones. Não são arquivos 4K nativos. O aplicativo desenha cada ícone a partir da sua região do atlas, compartilhando o bitmap, sem seis cópias decodificadas. O cache novo é limitado a 20 MiB e a decodificação ocorre fora da interface.

As oito artes douradas originais foram compactadas em WebP sem perda; a comparação RGBA confirmou pixels idênticos. Isso reduziu cerca de 5,9 MB nos arquivos-fonte. Os seis conjuntos adicionais somam aproximadamente 6,2 MB. No APK, o acréscimo real medido é de aproximadamente 5,5 MB, porque a compilação já otimizava os PNGs anteriores.

Tamanhos aproximados dos APKs gerados: ARM64 136 MB, ARM32 121 MB, x86 132 MB, x86_64 144 MB e universal 291 MB. Esses valores são dos instaladores; não incluem banco, cache e outros dados de uso. O plano de otimização dos dados do aparelho permanece separado.

A personalização é das telas internas do aplicativo; o ícone/banner do launcher do Android e o site mantêm a identidade padrão.

## Aplicar

1. Abra o terminal na raiz do projeto Android.
2. Execute `Aplicar-temas-completos.ps1`.
3. Execute `Gerar-APKs.ps1`.
4. Instale o APK compatível sobre a instalação atual, sem desinstalar e com a assinatura original.
5. Abra Configurações → Aparência e escolha a cor.

O instalador também inclui a grade aprovada de configurações, caso ela ainda não tenha sido aplicada. Aceita a entrada original, a tela anterior de dois cartões ou a grade atual. Verifica as versões completas dos arquivos, preserva CRLF/BOM e marcadores anteriores e não sobrescreve fontes desconhecidas. O pacote de artes é fixado em commit e conferido por SHA-256, assim como cada imagem. Todas as verificações ocorrem antes de alterar o projeto. Backups ficam em `.iptv-backups`, fora das pastas de recursos do Android. Os PNGs dourados só são removidos após instalar os WebP correspondentes.

`-SomenteVerificar` permite conferir a compatibilidade sem alterar arquivos do projeto. `-ArtesLocais` aceita o ZIP original para uso offline, mantendo a verificação de SHA-256.

Não muda identificador, assinatura, versão, Android mínimo, configurações privadas, Firebase, licença ou cadastros de listas. O script pode ser executado novamente sem duplicar alterações.

## Verificações realizadas

- Compilação assembleDebug concluída para as cinco saídas.
- Arquiteturas, assinaturas e igualdade de código/recursos dos APKs verificadas.
- Política real dos sete temas: persistência, fallback, troca e retorno, dourado original e alfa dos 69 tons preservados.
- Auditoria das referências de cor dourada e dos caminhos de logo/arte, incluindo o player.
- Comparação exata dos pixels das oito artes douradas antes/depois da compactação.
- Instalador executado em PowerShell 7 portátil, com os layouts original, de dois cartões e em grade: verificação sem escrita, aplicação, segunda execução idempotente, CRLF/BOM/marcadores e configurações privadas preservados.
- Código modificado, ZIP adulterado e arte original alterada bloqueados antes de qualquer escrita.
- Download real do pacote publicado e conferência de SHA-256 executados.

O visual, a legibilidade das cores e a navegação ainda precisam ser conferidos em TV e celular físicos. Não distribuir os APKs de teste do ambiente: a compilação no computador do usuário preserva sua assinatura e suas configurações.
