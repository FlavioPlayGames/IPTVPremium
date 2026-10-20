# Teste de capas 400 x 600

Requer a atualizacao base-restaurada-categorias-cache (3a607cd). O instalador confere todos os arquivos antes de escrever, faz backup e permite reaplicar sem duplicar alteracoes. Configuracao privada e Gradle nao sao incluidos.

- Filmes, series e carrossel usam capas com limite de 400 x 600 pixels. Mantem a proporcao original sem corte ou ampliacao das pequenas.
- Novos downloads maiores sao reduzidos e salvos em JPEG, qualidade 90, respeitando a reserva de armazenamento. Duas conversoes simultaneas no maximo. Imagens menores nao sao recomprimidas.
- Capas existentes sao reaproveitadas e reduzidas ao decodificar. Nao ha limpeza geral, migracao em massa nem novo download forcado. Os arquivos antigos permanecem no tamanho anterior ate a rotacao natural do cache.
- A URL original permanece: o volume recebido da internet nao diminui. A reducao acontece localmente depois do download.
- Navegacao, animacao, tamanho visual das capas, antecipacao, limites de rede, importacao e cotas de 100 MB para imagens / 350 MB para cache gerenciado permanecem iguais. Fundos, logos e fotos mantem a resolucao anterior.

Backup exato: `.navigation-backups/capas-400x600-*`. O patch inclui apenas dois consumidores de poster, um helper e testes de resolucao. Os arquivos de navegacao e importacao incluidos como guardas nao sao alterados.

Validacao: testes de limite 400x600 na RAM e disco, proporcao, imagens pequenas, arquivo invalido e reuso do cache sem nova requisicao. A nitidez e fluidez na TV precisam ser comparadas no aparelho.

Validacao concluida: 6 testes automatizados passaram, APK debug e APK de testes compilados. Instalador PowerShell testado com LF, CRLF, reaplicacao e rejeicao de arquivo editado.
