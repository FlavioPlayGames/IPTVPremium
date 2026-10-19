# Validação de APKs — 1.07 / interno 9

Corrige o instalador que rejeitava o resultado do usuário: applicationId correto, versão 1.07/interno 9, um único app-x86-debug.apk. Agora aceita conjuntos dirigidos a uma arquitetura e o conjunto completo de cinco saídas, sempre validando pacote, variante debug, versão, filtros ABI, nomes, unicidade e existência dos arquivos. Para instalar, prefere universal quando disponível; caso contrário consulta as ABIs do emulador e escolhe um APK compatível. Incompatibilidade continua interrompendo a instalação, sem desinstalar ou limpar dados.

A aceitação de uma compilação dirigida serve para instalar/testar naquele processador; não certifica disponibilidade para todos os processadores. O conjunto completo continua reconhecido separadamente.

Payload de fontes byte a byte igual ao de Preparar-Foco-Entrada-1.07.ps1, incluindo foco, quatro capas maiores e versão 1.07. Nenhuma alteração no aplicativo nesta correção. O script pode ser executado novamente sobre o projeto já atualizado. Conserva os nove testes nativos e o diagnóstico opcional -CapturarLog.

Validação: 17 cenários de saídas/compatibilidade e integridade do payload; 16 cenários do instalador, incluindo proteção contra downgrade, preservação de configuração/dados, aplicação idempotente, hashes e rollback. Executados com PowerShell em nuvem; a instalação real no emulador Windows precisa ser conferida pelo usuário.
