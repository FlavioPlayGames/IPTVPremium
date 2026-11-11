# IPTV Premium r19 — sincronismo manual, idiomas e SubSource

![Referência visual aprovada](PREVIA-APROVADA.png)

Referência de layout aprovada, gerada para revisão; não é captura do APK. A imagem fica somente neste pacote de documentação, sem ser adicionada aos recursos do aplicativo.

## O que muda

O menu Legendas passa a mostrar os idiomas que realmente têm opções. Ao escolher um idioma, aparecem as faixas internas desse idioma e as externas na ordem **SubDL → SubSource → OpenSubtitles**. Cada opção externa mostra o provedor e a versão/release do arquivo. SubDL pode apresentar até 30 alternativas já recebidas na pesquisa, em vez de descartar todas depois da primeira. Português Brasil fica separado do português genérico; os idiomas conhecidos são ordenados antes dos desconhecidos.

As pesquisas dos provedores configurados continuam em paralelo. Entrar em um idioma não inicia download nem cancela uma pesquisa que ainda está em andamento. Só escolher um arquivo pode baixá-lo; se já estiver salvo e válido no cache, o arquivo local é reutilizado.

Depois de ativar uma legenda externa, abra **Legendas → Sincronizar** ou a opção de sincronismo dentro do idioma selecionado. O painel é uma **faixa baixa de uma linha, centralizada na parte inferior**, abaixo da área da legenda: Atrasar, Adiantar, Ajuste e Zerar, com o deslocamento aplicado ao lado. O vídeo continua reproduzindo. O botão Voltar do controle fecha a faixa e retorna ao vídeo.

**Ajuste** define quanto muda por clique: 0,1 / 0,5 / 1 / 5 segundos; o padrão é 1 segundo e o deslocamento é limitado a ±120 segundos. O indicador mostra +0,0 s quando não houver valor salvo, e o valor efetivamente escolhido quando houver. Não existe um atraso fixo de oito segundos no aplicativo.

O valor é salvo por **série + temporada** e reaplicado aos episódios daquela temporada, independentemente do arquivo/provedor de legenda escolhido. Para **filmes**, cada obra tem seu ajuste individual. Normal e Kids ficam separados. O valor permanece após reabrir o app, até modificar ou zerar. Outra temporada, série ou filme sem configuração começa em zero. A identificação usa ID da obra ou título/ano; sem identificação confiável, IDs locais são separados pela lista, evitando juntar conteúdos distintos.

Cada configuração salva contém somente um deslocamento numérico e um identificador por hash. Ela não expira nem é removida pela rotatividade do cache; Zerar remove o ajuste daquela obra/temporada. Não há endereços de vídeo ou credenciais nesses registros. Ao escolher a legenda no player, o ajuste da obra/temporada é carregado. Isso não ativa uma legenda sozinho ao iniciar o vídeo. A camada externa é compartilhada pelo ExoPlayer e VLC; os tempos do arquivo/cache não são modificados, não há restart ou seek para ajustar. Legendas internas mantêm o tratamento do próprio mecanismo.

**Exemplo do teste relatado, não uma configuração automática:** se O Conto da Aia T05E06 continuar com a legenda adiantada em oito segundos, use Ajuste de 1 segundo e Atrasar oito vezes, até +8,0 s. Confira uma fala e avance no episódio. A configuração será da quinta temporada dessa série. Resident Evil e a sexta temporada continuam em zero, salvo configuração própria. Se a diferença crescer com o tempo, um deslocamento fixo não corrige mudança de ritmo/edição; escolha outra versão da legenda.

## Tempos presentes no arquivo

Os tempos de início/fim em milissegundos do SRT/VTT já eram interpretados. Continuam preservados no arquivo/cache; o novo ajuste é aplicado ao relógio usado para exibição. +8 segundos significa atrasar, não adiantar.

A coleta agora registra contagem de falas, primeiro/último tempo do arquivo, origem CACHE/DOWNLOAD, posição do vídeo, ajuste aplicado e a fala ativa naquele momento. Não registra o texto da fala, URL, chave ou token. Se não houver fala ativa naquele instante, início/fim são -1.

Esses horários são a referência automática normal da legenda; não indicam, por si só, quanto ela difere da fala da edição do vídeo. Não recebemos o arquivo real para conferir um eventual campo adicional de correção. Os 8 segundos observados em O Conto da Aia permanecem como ajuste manual, sem valor global. A r19 não sincroniza pelo áudio nem converte FPS automaticamente.

## SubSource

O app passa a ter cadastro/configuração SubSource na TV e no portal. Cada usuário usa sua própria chave pessoal; ela é entregue criptografada à TV vinculada e protegida no Android Keystore, fora do backup. Chaves de uma implementação antiga não são reutilizadas. SubDL/OpenSubtitles não são desconectados ou reconfigurados.

A implementação usa a API v1, cabeçalho X-API-Key, identificação por IMDb verificado a partir do TMDB quando disponível, ou título/ano sem escolher homônimos ambíguos. Temporada/episódio são conferidos pelos dados de identificação e pelos marcadores do release. Idiomas consultados: português brasileiro, inglês e espanhol. Falhas de autenticação/servidor não viram uma pesquisa vazia salva. Metadados de buscas SubSource são limitados a 32 entradas; arquivos usam o mesmo cache de legendas existente, sem aumentar sua cota.

**Validação real ainda pendente:** o acesso direto ao domínio SubSource foi bloqueado pela política de rede deste ambiente. O contrato foi conferido em clientes públicos atuais e testado com respostas simuladas; não houve conta/chave real. Disponibilidade, formato atual das respostas e cotas gratuitas devem ser confirmados na conta e na TV. Não há promessa de 20 downloads/dia para esse provedor. Não envie a chave na conversa.

## 1. Instalar na TV

Extraia em `Downloads\legendas-sincronia-iptv-premium-r19`. No PowerShell do Android Studio, com a TV autorizada:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\legendas-sincronia-iptv-premium-r19\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

A atualização parte da r18, valida hashes e guarda recuperação externa dos fontes. Compila localmente com sua configuração privada e instala com adb install -r, preservando dados/contas. Lote r15 de 2.000, capas r17, correção de redirecionamento r18, histórico, assinatura, versão, resolução e limites/rotatividade de armazenamento permanecem. O limite global de 400 MB não foi novamente medido nesta etapa.

O sincronismo e a configuração manual SubSource na TV já podem ser testados após a instalação. Para a chave, use Configurações → Legendas → passo 2 → Prefere configurar pela TV? → Chave SubSource. Salvar a chave confirma armazenamento local, não que a API a aceitou; a pesquisa confirma o acesso.

## 2. Atualizar o portal para o terceiro provedor

O pacote contém um patch dos três arquivos necessários do site, não sua configuração privada ou uma cópia integral do Worker. Antes de alterar, confira `site/alteracoes-site.patch`. O aplicador exige a mesma base revisada; se o site mudou, para sem sobrescrever arquivos divergentes. Ele não faz deploy.

```powershell
$pasta = "$env:USERPROFILE\Downloads\legendas-sincronia-iptv-premium-r19"
powershell -ExecutionPolicy Bypass -File "$pasta\Aplicar-Site.ps1" -Site "$env:USERPROFILE\Downloads\site-identidade-aprovada"
```

O backup dos três fontes anteriores fica fora da pasta do site em Downloads\IPTV-site-restauracao-r19-.... Nenhum .env, wrangler.jsonc, D1, APP_KEY, ADMIN_PASSWORD ou segredo OpenSubtitles é alterado.

Depois de conferir a aplicação local, publique no mesmo projeto Cloudflare já usado pelo site:

```powershell
Push-Location "$env:USERPROFILE\Downloads\site-identidade-aprovada"
npx wrangler deploy
Pop-Location
```

Na TV atualizada, abra o passo 2 de Legendas, gere o QR do portal e escaneie no celular. Em Serviços, escolha SubSource e informe a chave pessoal. Aguarde “Configurado na TV”. Em TVs antigas a opção fica desabilitada até o app enviar suporte ao SubSource; use a r19. O status confirma recebimento/armazenamento, não validação de cota ou pesquisa no serviço.

## 3. Conferir os tempos na TV

Inicie a coleta no PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\legendas-sincronia-iptv-premium-r19\Coletar-Diagnostico.ps1" -AoVivo
```

Saia e reabra O Conto da Aia T05E06, escolha o idioma e a legenda, aplique +8 segundos e confira uma fala. Teste Resident Evil sem ajuste, abra outro episódio da quinta temporada para conferir a mesma configuração; confira uma sexta temporada e outro filme sem herdar o valor. Teste zerar, reabrir o app e trocar de arquivo e, quando possível, ExoPlayer e VLC. Para SubSource, faça primeiro uma pesquisa conhecida; download somente após escolher uma opção.

Encerre com Ctrl+C e envie `Downloads\IPTV-legendas-r19.txt`. Os campos stage=timing / stage=sync distinguem horários do arquivo de deslocamento local. stage=subsource mostra resposta HTTP. Não são gravados URLs, títulos, conteúdo das legendas ou credenciais.

## Restaurar

App: `Desfazer.ps1 -Backup "PASTA_DA_RESTAURACAO_APP" -Instalar`, usando o caminho impresso ao aplicar. Restaura os fontes desta etapa, sem limpar dados ou configurações pessoais da TV.

Site: `Desfazer-Site.ps1 -Backup "PASTA_DA_RESTAURACAO_SITE"`. Restaura os três fontes localmente, com verificação de hashes. Para reverter o site publicado, execute npx wrangler deploy na pasta original depois da restauração. A restauração não remove segredos Cloudflare, dados D1 ou contas da TV.
