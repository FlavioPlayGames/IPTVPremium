# Validação r16 — 11/11/2026

- Android: `assembleDesempenho` e `lintDesempenho` concluídos. Lint: 0 erros, 154 avisos e 6 sugestões. Os avisos existentes não foram objeto desta etapa.
- Servidor recebido: 37 verificações com SQLite e provedor simulados, incluindo sessão, associação à TV, entrega criptografada, ACK e rejeições. Nenhuma escrita em produção.
- Cliente OpenSubtitles: 56 verificações JVM com respostas simuladas; sem acesso à API real.
- Ramo real de recebimento de sessão: sessão válida salva antes da notificação; provedor/registro inválidos não alteram a conta nem notificam o player.
- Portal atual: 34 verificações em Chromium nos tamanhos 390 × 844 e 1440 × 1000, com API simulada. Formulários, navegação, autenticação por escolha explícita, limpeza de credenciais, confirmação por recebimento simulado na TV e ausência de credenciais no armazenamento do navegador.
- Configurador privado: XML escapado, exclusão do Git, repetição segura, preservação de configuração diferente e ausência de chave na saída.
- Scripts: análise sintática PowerShell; aplicação, repetição, restauração e bloqueio de base divergente em PowerShell 7 e simulação da forma de JSON do PowerShell 5.1. Não equivale à execução integral em Windows 5.1.
- A configuração de lote de importação r15 foi preservada byte a byte. O manifesto contém somente os dois fontes Kotlin da correção.

Os testes de protocolo reproduzíveis estão em `testes/`. Os testes não usam credenciais reais. O site recebido não integra o pacote público.

## Ainda exige configuração e teste real

Registro API Consumer e chave técnica no APK/Worker; login com conta real; pesquisa e download oficiais; cotas; persistência no Android Keystore; apresentação, troca e desativação em ExoPlayer/VLC; teste após reabrir o app e isolamento de duas TVs. Não houve publicação do site, instalação na TV nem alteração de segredos Cloudflare nesta execução. Nenhuma garantia nova sobre o limite global de 400 MB é feita nesta etapa.
