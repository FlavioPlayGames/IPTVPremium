# Listas, Configurações, retomada e blocos — versão 1.8

- Modo Claro suavizado: fundo cinza claro e painéis menos brancos.
- Cada categoria de canais, filmes e séries passa a ter preenchimento e borda discretos mesmo sem seleção. O foco mantém o contorno metálico mais forte. Tamanho dos ícones e capas preservado.
- Listas e cadastro manual usam o mármore estático já preparado, painéis neutros e textos adaptados a Claro/Escuro. Os campos, teclado, validação e botões mantêm as ações existentes.
- Configurações de legendas e controle parental, incluindo teclado numérico, seguem as cores e fonte sem serifa do novo layout.
- Janela Continuar assistindo/Retomar episódio usa o vidro escuro e controles brancos do player aprovado nos dois modos; a posição, duração e ações Continuar/Do início são preservadas.
- Kids mantém sua própria paleta. Não altera importação, histórico salvo, autenticação, regra de PIN, reprodução ou tamanho dos elementos da Home.

## Verificação

Compilados assembleDebug, assembleDebugAndroidTest e compileReleaseKotlin. Instalador verifica hashes antes de qualquer escrita, cria backup, preserva arquivos locais do Kids e configurações privadas e rejeita edições desconhecidas. Sem emulador na máquina cloud: a conferência visual e os testes Android precisam ser executados no emulador do usuário. Nenhum APK publicado.
