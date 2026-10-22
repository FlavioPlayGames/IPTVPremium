Teste automatizado das marcações de cor, preservação de acentos, limites dos tokens e compartilhamento do bitmap local. Um teste Compose também captura uma prévia real.

Depois de aplicar o instalador, copie esta pasta para o projeto e rode na raiz:

```powershell
.\gradlew.bat -I caminho\testes\validar-titulos.gradle :app:testDebugUnitTest --tests '*CinemaTitleTest'
```

Requer JDK/SDK compatíveis com seu projeto. A imagem de demonstração é salva em `app/build/cinema-title-preview.png`. A validação original foi feita com Robolectric 4.16.1, Android 35 e gráficos nativos; não em TV física.
