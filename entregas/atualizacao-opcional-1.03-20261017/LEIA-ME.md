# IPTV Premium 1.03 — número interno 5

Preparado para teste de instalação por cima da 1.02/code4, sem desinstalar nem limpar dados.
O preparador Android aplica somente cinco arquivos, cria backup e preserva configurações de assinatura.
Prepara APP_VERSION_CODE=5 e APP_VERSION_NAME=1.03 e verifica os cinco APKs após compilar.
Detecta o Java do Android Studio quando instalado no caminho padrão.

## Aplicativo

Execute Preparar-app-1.03.ps1 na raiz Android (pasta com gradlew.bat e local.properties).
Opcional: -Projeto caminho. Para apenas aplicar sem compilar: -NaoCompilar.
Instale primeiro no emulador por cima da versão existente, com o APK x86 se for o emulator-5554 AOSP_TV_on_x86 informado.

O aviso permite Mais tarde ou Voltar. O adiamento é lembrado para aquela versão.
Configurações > Atualização > Verificar atualização permite retomar a instalação.
Versões posteriores podem gerar novo aviso. A navegação fica preservada por trás do diálogo.
Os APKs são escolhidos pelas arquiteturas suportadas pelo Android, mantendo as verificações de integridade e assinatura.
O Android ainda pede autorização para instalar e a confirmação final; não há instalação silenciosa.

## Teste com os registros que já foram criados

1. Instalar a 1.03/code5 com adb install -r, sem remover o aplicativo ou listas.
2. Conferir os mesmos favoritos e os itens de continuar assistindo em filmes e séries.
3. Fechar, reabrir e conferir novamente; confirmar a versão em Configurações > Atualização.
4. Somente depois publicar os APKs no GitHub e atualizar o painel.

Não houve perda de dados confirmada: o usuário informou possível limpeza prévia de listas.
Não há migração nem recuperação automática de favoritos neste pacote.
O diagnóstico anterior não acessou os dados do telefone; o teste de atualização real permanece necessário.

## Painel e Cloudflare

Preparar-central-opcional.ps1 -PastaDestino caminho cria uma pasta nova, sem substituir o site antigo.
Contém Worker, public, wrangler.jsonc e package.json completos.
O preparador não publica. Leia o LEIA-ME.txt gerado antes do deploy.
Usa o Worker central-iptv-premium, o banco existente, domínio e cron já confirmados.
Mantém segredos remotos; não cria banco, não executa migrations e não hospeda APKs no Cloudflare.
D1 continua com um único registro de metadados de até 16 KiB, substituído a cada publicação.

O Worker novo retorna update:null às versões antigas para retirar a exigência na próxima consulta bem-sucedida.
Elas precisam instalar a 1.03 manualmente uma vez para adotar o fluxo opcional.
Clientes novos informam o protocolo 2 e recebem a publicação opcional e as variantes por arquitetura.
A sincronização de comandos e licenças permanece independente dessa compatibilidade.

## Verificação realizada

- Compilação Android assembleDebug, todos os APKs 1.03/code5.
- 12 cenários de políticas, seleção e inspeção de ABI.
- 10 cenários do gerenciador de atualizações em JVM com serviços Android simulados.
- 9 cenários Worker, 4 sincronizações autenticadas com comandos criptografados e 5 contratos de formulário.
- 6 cenários em navegador Chromium real com Worker e D1 simulado.
- 10 verificações de preparadores, integridade, backups, versão e preservação de arquivos.
- Wrangler 4.151.1 deploy --dry-run, reconhecendo DB e ASSETS.

Os testes locais não confirmam instalação real, navegação pelo controle remoto nem preservação dos registros do usuário após atualização.
Nenhum APK compilado na nuvem ou segredo está incluído na entrega. A compilação e assinatura finais são feitas no computador do usuário.
