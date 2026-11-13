# Validação r32 — 13/11/2026

Base: último java.zip recebido, extraído em android-recebido/java. Nenhum arquivo de assinatura, configuração local, chave de API ou código do site faz parte da entrega. O aplicador valida 13 dependências/recursos existentes e os hashes anteriores de cada alteração antes de escrever.

- Base recebida e telas novas compilaram com :app:compileDesempenhoKotlin, usando o SDK 37/Media3 1.11.1 e dependências existentes.
- Teste de relógio compilou o wrapper contra a interface real Media3 Renderer: sinais positivo/negativo, limites/overflow, troca de offset e delegação preservando o relógio elapsedRealtimeUs. Não comprova reprodução com decoder/dispositivo reais.
- 40 verificações de sincronia passaram: horários originais SRT/VTT, chave por obra/temporada, isolamento entre temporadas/filmes/Kids, persistência entre listas verificadas, Zerar, intervalos e auxiliar sem desconto com arredondamento aprovado.
- 40 verificações do SubSource passaram com API simulada; nenhum acesso à conta real durante testes.
- Aplicação, repetição, restauração byte a byte, recusa de base/edição divergentes e parser dos scripts passaram em PowerShell 7 e simulação da forma de arrays do Windows PowerShell 5.1. Não equivale a execução nativa Windows.
- A arte nova pesa 4.648 bytes; ícones são paths vetoriais originais. Nenhuma biblioteca nova, alteração no Gradle, orçamento de cache ou chaves no pacote.

APK e resultados finais de empacotamento são registrados em RESULTADO-APK.md. O APK local de validação não contém as configurações pessoais completas do PC e não é distribuído; o instalador compila com a configuração local.

Não há emulador/TV física conectado neste ambiente e não foram obtidas capturas reais do APK. A prancha aprovada é referência de projeto, não prova de igualdade pixel a pixel. Conferir foco, recortes de tela, QR real, etapas manuais e sincronia durante reprodução ExoPlayer/VLC na TV é o próximo teste necessário. Nenhum deploy no site ou alteração de D1/R2 ocorre nesta atualização.
