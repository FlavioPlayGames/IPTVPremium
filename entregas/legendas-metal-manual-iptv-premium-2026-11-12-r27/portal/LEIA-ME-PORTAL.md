# Publicação do portal — legendas manuais

Este pacote prepara o Android, o site e o Worker. Não publica nada na sua conta Cloudflare. O código do Worker usa como base o texto enviado em 12/11/2026; as rotas anteriores permanecem e foram acrescentadas somente rotas de envio manual.

## Seu Worker confirmado

A captura enviada mostra `central-iptv-premium`, com os vínculos `ASSETS` e `DB`. Este pacote não contém um wrangler.jsonc inventado e não substitui domínios, rotas, agendamentos ou segredos. O site continua preto e dourado, independentemente do tema da TV.

## Antes de publicar

1. Guarde uma cópia da versão atual do Worker e do site.
2. No Cloudflare R2, crie um bucket **privado** para arquivos temporários de legenda, por exemplo `iptv-legendas-manuais`. Não habilite acesso público nem domínio r2.dev.
3. Na mesma tela da captura: **Workers e Pages → central-iptv-premium → Configurações → Vínculos (Bindings) → Adicionar uma vinculação (+)**. Escolha **Bucket R2**. No nome da variável/vínculo, escreva exatamente **MANUAL_SUBTITLES** e selecione o bucket privado criado. Salve a alteração. Mantenha ASSETS e DB.

   O nome do bucket pode ser `iptv-legendas-manuais`; o nome que o código procura é o vínculo `MANUAL_SUBTITLES`, com letras maiúsculas.
4. No bucket, configure uma regra de ciclo de vida para excluir objetos com prefixo `manual/` após **um dia**. Essa regra remove sobras caso a TV ou o navegador sejam fechados antes de concluir. Não aplique a regra a buckets antigos de outros conteúdos.
5. Preserve os vínculos existentes **DB** e **ASSETS**, os segredos e a configuração atual de OpenSubtitles/Firebase. Não é necessário alterar o esquema ou apagar o banco.

## O que está pronto e o que falta

O código de conexão Android → Worker → site → Worker → TV está incluído. Os testes de servidor usam DB e R2 simulados. Não houve publicação no domínio do usuário, pois esta sessão não tem acesso autenticado à conta Cloudflare. Apenas instalar o Android não cria o bucket nem atualiza o site.

## Publicação

- O arquivo `worker.js` é o módulo completo atualizado, pronto para substituir o código do Worker que foi enviado como texto. Publique pelo mesmo procedimento que você já utiliza na sua conta.
- O arquivo `site-public-atual-r27.zip` contém a pasta `public/` completa: seus arquivos atuais mais o envio manual. Publique essa pasta pelo procedimento que você já usa para atualizar os assets do Worker. A pasta precisa continuar vinculada como `ASSETS`; trocar apenas o texto do Worker não atualiza os arquivos estáticos do site. Se sua publicação usa Wrangler, mantenha seu wrangler.toml/jsonc, seus IDs e segredos; o ZIP não inventa ou substitui essas configurações.
- Instale o aplicativo atualizado pelo `Atualizar-TV.ps1` da entrega. Não use um APK antigo para testar o novo fluxo.

## Uso

Na TV: Configurações → Legendas → Envio manual → Filme ou Série. Para série, escolha temporada inteira ou episódio. Selecione o título no catálogo da lista ativa, complete temporada/episódio e escolha **Conectar para enviar**. Só então é criado o QR.

No celular: leia o QR ou entre normalmente na conta do site e abra Legendas → Envio manual → Atualizar conexão. O site mostra a TV e o destino definido pelo aplicativo. Selecione um SRT ou ZIP e envie.

Na TV: confira as legendas identificadas; quando houver opções repetidas ou arquivo sem número de episódio, escolha o arquivo/destino sem renomear nada. Confirme **Salvar**. As legendas aparecem no player no grupo **Legendas manuais**, com o nome original.

O site só confirma recebimento depois que a TV salva e confirma. Upload concluído sozinho significa apenas arquivo enviado; não significa instalado/aplicado na TV. É possível precisar confirmar novamente se a rede cair após salvar.

## Limites e retenção

- Pedido/QR por 15 minutos, vinculado à TV e ao destino. Novo pedido substitui o anterior; cancelar na TV invalida o envio.
- Um arquivo por pedido: SRT até 1 MB; ZIP até 8 MB, até 512 entradas e 16 MB expandidos, até 128 SRTs válidos. Outros tipos são ignorados e os caminhos do ZIP nunca são extraídos no sistema de arquivos.
- Até 4 MB de legendas escolhidas por importação. Temporadas maiores podem ser enviadas em partes. O Android continua usando o orçamento compartilhado existente de 5 MB para arquivos de legenda; arquivos antigos podem sair do cache quando faltar espaço.
- SRTs validados são gravados na área privada existente do aplicativo. O ZIP e os demais arquivos extraídos não são mantidos pela TV. O nome original é somente um rótulo, não um caminho de gravação.
- O objeto do upload no servidor é removido após confirmação, rejeição ou cancelamento. Pedidos abandonados/erros de rede podem deixar objetos temporários até a regra de ciclo de vida de um dia; o acesso já fica recusado ao expirar o pedido.
- O Worker não recebe nem armazena o catálogo completo ou a URL privada do vídeo nesse fluxo. Só recebe os dados do conteúdo selecionado.

## Conferência

Teste primeiro em um aparelho autorizado: abra e saia sem escolher opção (nenhum QR deve surgir); teste as contas antigas; teste Aparência; envie um SRT para filme; envie um ZIP de dois episódios; cancele um pedido e tente usar o QR antigo. Não remova vínculos, não limpe o banco e não desinstale o aplicativo para contornar erro de assinatura.

## Verificação depois da publicação

1. A nova aba **Envio manual** aparece em Legendas no site.
2. Na TV, escolha filme/temporada/episódio antes de abrir o QR.
3. Leia o QR: a central deve abrir diretamente em Envio manual e mostrar o destino, sem pedir para editar o nome do arquivo.
4. Envie um SRT. O site deve mostrar **Arquivo enviado · confira e salve na TV**.
5. Confira e salve na TV. Somente então o site mostra **Legenda recebida pela TV**.
6. No player daquele conteúdo, abra **Legendas manuais** e selecione o arquivo. Teste depois um ZIP de dois episódios.

Se aparecer “O envio manual ainda não foi habilitado na central”, confira o vínculo R2. Se a aba não aparecer, confira a publicação dos assets e recarregue a página. Não apague DB nem ASSETS para tentar corrigir.
