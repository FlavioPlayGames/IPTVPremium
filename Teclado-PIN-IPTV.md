# Teclado numérico próprio e botão Continuar

Todos os campos de PIN do controle parental passam a usar o mesmo teclado numérico do aplicativo: criação, confirmação, alteração, acesso aos controles, bloqueio geral e desbloqueio das categorias. O teclado nativo da TV não é solicitado por esses campos.

## Usar o teclado

Selecione o campo e pressione OK, Enter ou toque nele. Use as setas e OK para escolher os números, ou toque diretamente nas teclas. Há Apagar, Limpar, Pronto e Cancelar. Pronto fica disponível com 4 a 8 números e preenche o campo; depois confirme na ação correspondente: Entrar, Desbloquear ou Salvar PIN. Cancelar/Voltar descarta a edição em andamento.

As teclas numéricas físicas, inclusive o teclado numérico externo, funcionam tanto no campo quanto no teclado aberto. Backspace/Delete apagam o último número. A ação de cada tecla é consumida uma única vez e manter uma tecla pressionada não repete os números do PIN.

O PIN aparece mascarado, tem limite de oito números e não é salvo no estado restaurável da tela. Ao sair do aplicativo, o teclado fecha e seu rascunho é descartado. Os campos e teclas acompanham a cor do tema, com foco visível e rotas explícitas para as setas do controle. Em telas pequenas, o painel permite rolagem.

O armazenamento, a validação e as regras do PIN existente não foram alterados. Não é necessário recriar o PIN; o controle parental por categorias da entrega anterior continua incluído.

## Cadastro manual

Continuar conserva a borda e o fundo escuro no mesmo padrão de Voltar, inclusive antes da seleção. O preenchimento cinza do estado desabilitado foi removido; a indicação de indisponibilidade fica no texto atenuado, sem permitir foco ou clique antes de escolher a lista.

Voltar continua à esquerda, na mesma linha. Selecionar Xtream Codes ou M3U continua transferindo o foco para Continuar; o próximo OK avança ao formulário.

## Aplicar

Execute `Aplicar-teclado-PIN.ps1` na raiz do projeto Android e depois `Gerar-APKs.ps1`. Instale o APK apropriado sobre a versão atual, sem desinstalar e usando a assinatura original. O instalador é cumulativo, verifica compatibilidade antes de escrever e faz backup em `.iptv-backups/teclado-pin-DATA-HORA`.

`local.properties`, `google-services.json`, assinatura, versão e os recursos de imagem não foram alterados por esta correção.

## Validação

- AssembleDebug dos cinco APKs concluído.
- Helpers de entrada e o tratador real de eventos Kotlin exercitados em JVM com adaptadores mínimos de KeyEvent: números de 0 a 9, teclado numérico externo, zero inicial, limite de oito dígitos, confirmação com quatro a oito, exclusão/vazio, uma entrada por pressionamento e preservação dos eventos de OK, setas e Voltar para seus controles.
- Instalador executado para versões compatíveis e para a entrega anterior: verificação sem escrita, instalação, backup, repetição sem alterações e preservação dos arquivos privados.
- A interação física com o controle da TV e o toque ainda precisa ser conferida no aparelho do usuário. A compilação e os testes de lógica não substituem essa conferência.
