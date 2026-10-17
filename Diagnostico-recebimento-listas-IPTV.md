# Diagnóstico de recebimento de listas

Auxiliar de leitura dos logs existentes, sem alterar aplicativo, APK, listas, dados, configuração de rede ou DNS. Não limpa o logcat. Captura apenas o PID atual do IPTV Premium, com filtros das rotinas de push, sincronização, rede, importação e erros. Salva apenas versão do pacote e linhas de diagnóstico; omite URLs e campos reconhecidos de credencial/token.

## Como reproduzir

1. Abra o aplicativo no emulador e mantenha-o aberto durante a captura.
2. Execute Diagnosticar-envio-lista.ps1 no PowerShell. Não precisa estar na pasta do projeto.
3. Envie uma lista pelo site e aguarde cerca de 30 segundos sem sincronizar manualmente.
4. Se não houver carregamento, clique em Sincronizar uma vez. Aguarde o resultado ou erro.
5. Pressione Ctrl+C no terminal e envie o arquivo Diagnostico-envio-IPTV-*.txt salvo na Área de Trabalho. O script imprime o caminho completo.

Não reinicie o aplicativo durante a captura: o PID mudaria. Se houver mais de um aparelho conectado, especifique -Serial; se o SDK estiver em outro local, use -AdbPath. -PastaSaida permite escolher a pasta do relatório.

## O que observar

- PortalPush: aviso recebido/processado, início/fim de consulta à central ou adiamento.
- M3uSourceSync/M3uImportPerf/XtreamCatalogSync: início da carga, etapas, parser e persistência.
- PlaylistConnection: status HTTP, classe do erro, etapa e tempo.
- CacheGeneration: publicação/limpeza do catálogo.
- Horários distinguem o envio automático da tentativa manual posterior.

A ausência de uma linha PortalPush indica que não foi registrado um aviso tratado nessa captura; sozinha não prova que o Firebase nunca entregou a mensagem. Os logs atuais não detalham todas as falhas HTTP internas de pull/ACK da central. Se esse ponto ficar sem detalhes, será proposta instrumentação específica para aprovação, sem redesenhar telas nem alterar a sincronização às cegas.

## Verificação do auxiliar

Testado em PowerShell portátil com ADB simulado: seleção do processo, metadados somente de versão, logs em sequência, remoção de URL/token e tratamento de aplicativo fechado/múltiplos dispositivos. -Autoteste verifica o filtro localmente. A captura real precisa ser executada no computador do usuário, que está conectado ao emulador.
