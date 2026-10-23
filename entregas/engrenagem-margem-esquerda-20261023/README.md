# Margem esquerda da engrenagem

Aumenta somente a margem esquerda do recorte da engrenagem em 6 unidades na composição de 1672 pixels. Mantém o tamanho e a ampliação do ícone e o limite das outras bordas. O ícone Atualizar permanece como na correção anterior.

Aplicar depois de canais-voltar-uma-vez-20261023.

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/engrenagem-margem-esquerda-20261023'
$script = Join-Path $env:TEMP 'Preparar-Margem-Engrenagem.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Margem-Engrenagem.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Alteração visual pequena conferida no fonte. Instalador valida o arquivo e cria backup fora de app/src. Sem validação visual na TV; a opção -Testar compila o APK no projeto local.
