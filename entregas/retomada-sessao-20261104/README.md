# Retomada da sessão

O aplicativo mantém a tela acordada enquanto está aberto, sem temporizador de saída por inatividade. Ao abrir outro aplicativo, salva a navegação e pausa o player. Ao voltar, retoma o mesmo filme/episódio na posição salva; uma pausa manual permanece pausada. Canais reconectam à transmissão atual do mesmo canal.

O Android ainda pode encerrar o processo para liberar memória. Esta atualização grava uma cópia local do estado da interface no armazenamento privado, sem expiração por tempo, para reconstruí-lo na próxima abertura. A tarefa também passa a reter a navegação após longos períodos. A recuperação depende do conteúdo continuar disponível e das permissões/acesso válidos. Não recupera dados após desinstalação ou limpeza dos dados. Sair explicitamente do aplicativo encerra a sessão. A cópia de estado é compatível com a mesma versão do aplicativo; não é backup entre aparelhos.

Mantém o histórico e os favoritos unificados da atualização anterior. Pacote cumulativo em relação ao java.zip enviado, aceita também os fontes da entrega historico-unificado. Inclui uma alteração no AndroidManifest.xml para alwaysRetainTaskState; o instalador valida a versão do manifesto antes de alterar, sem sobrescrever personalizações desconhecidas.

Extraia o ZIP e execute no PowerShell dentro da pasta extraída:

```powershell
.\Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL:

```powershell
.\Instalar-TV.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium'
```

Aplicar cria backup; Desfazer restaura e recusa conflito com edições posteriores. Sem APK ou configurações privadas. Validar na TV: Home/outro app durante filme, série e canal; retornar imediatamente e após longo período; pausa manual; recriação do processo; navegação de catálogo; sair explicitamente. Os relatórios informam os testes executados e seus limites.
