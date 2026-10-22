Depois de aplicar o instalador, copie esta pasta para seu projeto. Na raiz, execute:

```powershell
.\gradlew.bat -I caminho\testes\validar-tentativas.gradle :app:testDebugUnitTest --tests '*MovieOpeningDeadlineTest' --tests '*RealMovieRetryTest'
```

O teste usa o cliente HTTP do aplicativo com um protocolo local de conexões simuladas. Não consulta seu provedor nem usa credenciais. Verifica novas conexões apesar de leitura e disconnect antigos bloqueados. Há testes de tempo virtual, cancelamento ao voltar, cache e concorrência com antecipação em segundo plano. Requer JDK/SDK compatíveis com o projeto.
