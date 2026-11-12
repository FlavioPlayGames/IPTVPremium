# SubSource — correção de séries r20

Base exigida: r19 (legendas-sincronia-iptv-premium-r19). O aplicador verifica os hashes antes de alterar qualquer fonte. Não usar sobre versões modificadas sem comparar o código. Não muda o site.

## Mudanças

- Segue a paginação oficial de /subtitles: até cinco páginas de 100 resultados por idioma, parando ao acabar, ao repetir uma página ou ao reunir 30 opções individuais. Até 16 requisições de pesquisa por conteúdo (uma obra + quinze páginas), respeitando o intervalo original de um segundo entre chamadas e o bloqueio 429.
- Reconhece S05E06, 5x06 e indicações como Episode 6 na temporada previamente identificada. Lê releaseInfo como lista e também como texto para compatibilidade.
- Mostra pacotes de temporada como opções explicitamente identificadas. Metadados indicam somente candidatos: o episódio ainda será conferido no ZIP depois da escolha. Não baixa arquivos durante a pesquisa. Um pacote pode não conter o episódio desejado; nesse caso ele não é carregado.
- Dá preferência às versões individuais; mostra até 30 opções por idioma. A busca é limitada, não garante consultar todas as páginas de catálogos muito grandes. O diagnóstico informa quando o limite foi atingido.
- Separa o arquivo extraído do SubSource por conta, ID, temporada e episódio. O mesmo pacote não reaproveita a legenda de outro episódio.
- A nova versão do cache de pesquisa deixa de reutilizar os resultados vazios da r19. Mantém até 32 entradas de pesquisa e o orçamento original dos arquivos de legenda. Nenhuma limpeza de dados da TV.
- ZIP: até 500 entradas, 1 MB por legenda e 10 MB descompactados no total. Processamento em fluxo, sem guardar todos os episódios ou gravar caminhos do ZIP no disco. Limite anterior de 5 MB por resposta de download preservado.
- Diagnóstico exporta somente números e códigos por idioma, sem chaves, nomes de conteúdo ou URLs. Outros provedores e ajuste manual não foram alterados. Auxiliar de sincronização continua fora desta entrega.

## Aplicar e instalar

Feche o Android Studio se ele estiver compilando; use o PowerShell em que a TV já está conectada/autorizada:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\legendas-subsource-iptv-premium-r20\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

O script preserva a assinatura local, compila desempenho e instala com adb install -r. Não remove o aplicativo nem muda o pareamento. Instalação requer a TV já autorizada no ADB. O backup dos fontes fica fora do projeto, em Downloads\IPTV-restauracao-r20-....

## Validar na TV

1. Pesquisar legendas de O Conto da Aia T05E06. Abrir o idioma e conferir opções identificadas como SubSource (depois do SubDL, antes do OpenSubtitles).
2. Selecionar uma versão individual; testar ativar, ajuste manual e desativar.
3. Se disponível, selecionar um pacote. Conferir o episódio; depois testar outro episódio com o mesmo pacote. Não deve reaproveitar o arquivo anterior.
4. Conferir que o ajuste manual permanece por temporada; filme continua individual.
5. Se faltar uma opção, iniciar o coletor e pesquisar um episódio novo, ou esperar dez minutos para expirar o cache negativo:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\legendas-subsource-iptv-premium-r20\Coletar-Diagnostico.ps1" -AoVivo
```

Aguardar terminar a pesquisa e usar Ctrl+C. Arquivo: Downloads\IPTV-legendas-r20.txt. Novos campos: subsource_works (obras recebidas/aceitas) e subsource_results (idioma, páginas, recebidas, aceitas, rejeitadas, pacotes e truncated).

## Desfazer

O aplicador informa o comando com o caminho exato do backup. Use Desfazer.ps1 -Backup "caminho" -Instalar para restaurar os fontes e compilar/instalar a base novamente. A restauração recusa sobrescrever arquivos editados depois da aplicação. Configurações e caches existentes da TV não são revertidos; o orçamento de cache continua o original.

## Limites da validação

Os testes de contrato usam respostas simuladas baseadas no PDF oficial recebido. Não tivemos acesso à API real do SubSource neste ambiente. A compilação e os testes locais não comprovam que a opção específica apareça na TV: isso requer o teste acima. A contagem de 400 MB do aplicativo inteiro não foi medida nem garantida por esta alteração.
