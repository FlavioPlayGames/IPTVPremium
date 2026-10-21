# IPTV Premium 1.09 — seleção de capas e categorias translúcidas

Em Filmes e Séries, a capa em foco ganha contorno metálico de 2,4 dp, separador escuro fino, sombra curta e ampliação de 3%. A ampliação fica na imagem da capa, mantendo título e metadados nítidos. Capas sem seleção continuam sem contorno decorativo.

O painel de categorias e o fundo das linhas deixam o mármore aparecer nos modos Claro e Escuro. Letras, contagens, logo e ícones mantêm a opacidade original. O modo Claro mantém o branco suavizado e os pequenos ícones em grafite. O Kids mantém sua própria aparência.

A versão passa a ser exatamente **1.09**, com versionCode mínimo 11; códigos superiores definidos localmente são respeitados. O nome antigo em APP_VERSION_NAME não substitui o número desta entrega. local.properties não é modificado.

## Aplicar

Execute `Preparar-Capas-Vidro-1.09.ps1` na raiz do projeto. Para compilar, instalar e testar no emulador aberto, acrescente `-Emulador emulator-5554 -Testar`. O script verifica os arquivos antes de escrever, cria backup e usa instalação sobre o aplicativo existente, sem limpar seus dados.

`-Verificar` apenas confere a compatibilidade; `-SomenteAplicar` aplica sem compilar. Arquivos locais diferentes das versões conhecidas interrompem a aplicação antes de qualquer alteração.

## Verificação

Compilação: assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin. Testes do instalador: verificação sem escrita, backup exato, reaplicação, rejeição atômica de edição desconhecida e preservação de arquivos privados/testes locais, incluindo UTF-8 com BOM e CRLF.

O ambiente de compilação não tem emulador: os testes instrumentados foram compilados, mas sua execução e a revisão visual na TV dependem do emulador/aparelho local. O script -Testar executa a suíte existente de 90 testes no emulador.

Nenhum APK, segredo, arquivo de configuração privada ou recurso auxiliar exclusivo do ambiente de compilação está incluído nesta entrega. O player conserva o visual escuro translúcido aprovado para ambos os modos.
