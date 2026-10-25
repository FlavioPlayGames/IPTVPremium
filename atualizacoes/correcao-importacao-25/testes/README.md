Depois de aplicar a atualização, copie esta pasta para o projeto e execute na raiz:

```powershell
.\gradlew.bat -I caminho\testes\validar-importacao.gradle :app:testDebugUnitTest --tests '*ImportBatchPipelineTest' --tests '*ImportFormatPreservationTest' --tests '*Import25HttpTest' --tests '*LargeImport25Test'
```

São 16 testes. Usam tempo virtual, Room/Robolectric Android 35 e conexões simuladas do cliente HTTP real. Não usam credenciais nem consultam o provedor. O teste de volume mantém as contagens da captura (44.724 filmes e 20.170 séries), mas seu tempo em computador não equivale ao tempo da TV. Requer JDK/SDK compatíveis com seu projeto.
