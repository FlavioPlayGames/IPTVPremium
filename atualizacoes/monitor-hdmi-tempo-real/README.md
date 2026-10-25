# Monitor HDMI local em tempo real

Monitor somente de leitura para rodar no PC Windows conectado à TV por ADB. Não é acesso remoto do assistente e não altera HDR, VRR, resolução, taxa, driver ou firmware.

## Usar

Com a TV autorizada no ADB e exibindo a entrada do PC:

```powershell
$arquivo = Join-Path $env:TEMP 'Monitor-HDMI.ps1'
Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/main/atualizacoes/monitor-hdmi-tempo-real/Monitor-HDMI.ps1' -OutFile $arquivo -ErrorAction Stop
powershell -NoProfile -ExecutionPolicy Bypass -File $arquivo
```

O script encontra o único dispositivo ADB conectado. Se houver mais de um, use `-Tv IDENTIFICADOR` com o nome completo mostrado em `adb devices`. O caminho do ADB pode ser informado em `-AdbPath`. O padrão é o SDK do Android Studio em LocalAppData.

O monitor exibe contadores por segundo e grava eventos HDMI filtrados. Duração padrão: 180 segundos. Pode informar `-DuracaoSegundos 300`. Os relatórios ficam em uma pasta `HDMI-data-hora` e num ZIP com o mesmo nome, em Downloads.

## Marcar o teste

As teclas funcionam **somente quando o terminal está em foco**:

- **F:** você percebeu linhas piscando; registra o horário do PC.
- **1:** marca que você colocou 144 Hz.
- **2:** marca que você colocou 120 Hz.
- **3:** marca que você desligou o VRR.
- **Q:** encerra antes do tempo.

1/2/3 são anotações manuais: não alteram nem detectam o modo real da TV. Informe junto com o relatório quais condições estavam ativas no começo, incluindo HDR e VRR. Mude uma variável por vez. É possível apenas observar sem mudar qualquer configuração.

Ao encerrar, envie o ZIP gerado. Ele inclui registros filtrados, marcações e uma consulta inicial de driver/frequência no Windows, quando disponível. A leitura do Windows não é uma medição contínua do sinal HDMI. O arquivo temporário de Logcat amplo é apagado e não entra no ZIP. A coleta para em caso de desconexão ou limite de 64 MB do temporário. Eventos podem ser perdidos ao encerrar a coleta ou durante desconexões.

## Limites

O contador mostra mensagens do firmware, **não o número de piscadas da tela**. Mensagens repetidas de MI_RENDER/PQ não comprovam defeito. O ADB comum não fornece necessariamente contadores de erro FRL, osciloscópio do sinal ou estado real de VRR/HDR da entrada HDMI. Os 60 Hz do compositor Android não substituem a leitura do sinal do PC. Correlacionar as marcações com os eventos e os testes visuais ajuda a investigar; não fecha diagnóstico físico sozinho.

## Validação

Verificado com PowerShell 7 e ADB simulado: leitura incremental, filtro de eventos, remoção de URLs/credenciais dos novos registros, limpeza do temporário, geração do ZIP e registro de desconexão. Usa sintaxe compatível com Windows PowerShell 5.1; execução na TV/Windows do usuário ainda precisa ser validada. Nenhum arquivo do aplicativo IPTV foi modificado.
