# Vídeo dentro da margem e busca após 7 segundos

Correção sobre a entrega anterior de Canais. Mantém a versão 1.11 (13), os catálogos, o histórico e os favoritos.

- A superfície nativa estava sendo ampliada em ZOOM além da moldura. A prévia agora preserva a proporção com FIT, mantendo o vídeo dentro da área reservada, sem cobrir a barra superior ou o guia. Conteúdos de outra proporção podem apresentar faixas dentro do quadro.
- Cantos da moldura da prévia reduzidos para **6 dp**, com recorte interno de **4 dp**, mais próximo da referência enviada. Em tela cheia, os cantos ficam retos.
- Mantém o mesmo player e SurfaceView ao expandir; não reabre a transmissão por mudar de tamanho.
- Prazo máximo para iniciar uma transmissão ou recuperar um travamento reduzido de 12 para **7 segundos**. A contagem é monotônica e avisos repetidos de buffering não reiniciam esse prazo.
- Falhas detectadas antes disso avançam para a próxima candidata sem esperar os 7 segundos. Removida a repetição interna da mesma tentativa antes de avisar a falha. Conexão/leitura HTTP também limitadas a 7 segundos.
- A ordem de busca, as mensagens, o retorno ao último canal, a lista lateral e a avaliação sem animações da entrega anterior permanecem.

## Aplicar

```powershell
$base = 'https://raw.githubusercontent.com/FlavioPlayGames/IPTVPremium/entrega-iptvpremium-otimizado/entregas/ajuste-video-canais-7s-20261022'
$script = Join-Path $env:TEMP 'Preparar-Ajuste-Video-Canais.ps1'
Invoke-WebRequest -UseBasicParsing "$base/Preparar-Ajuste-Video-Canais.ps1" -OutFile $script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $script -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Aplica somente três arquivos Kotlin, com verificação prévia e backup em `.live-margin-backups`. Precisa da entrega anterior de Canais aplicada. Se detectar mudanças locais diferentes, interrompe antes de copiar. Não remove imagens nem dados.

Para pacote local, acrescente `-PacoteLocal 'C:\caminho\ajuste-video-canais-7s.zip'`.

Depois, gere e instale o novo APK sobre o app existente. Confira na TV os limites da prévia, os cantos e a expansão. O vídeo deve permanecer dentro da margem com conteúdo 16:9, 4:3 e outros formatos. Para um servidor sem resposta, confira a passagem à próxima transmissão em até cerca de sete segundos por tentativa; um erro imediato deve avançar antes disso.

Compilação e testes locais estão em `verificacao.txt`. O recorte da superfície e a fluidez precisam ser conferidos na TV; não houve teste visual no aparelho nesta entrega.
