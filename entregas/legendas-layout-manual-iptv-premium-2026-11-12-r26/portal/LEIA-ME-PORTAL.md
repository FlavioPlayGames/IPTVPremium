# Publicação do portal — legendas manuais

Este pacote prepara o Android, o site e o Worker. Não publica nada na sua conta Cloudflare. O código do Worker usa como base o texto enviado em 12/11/2026; as rotas anteriores permanecem e foram acrescentadas somente rotas de envio manual.

## Antes de publicar

1. Guarde uma cópia da versão atual do Worker e do site.
2. No Cloudflare R2, crie um bucket **privado** para arquivos temporários de legenda, por exemplo `iptv-legendas-manuais`. Não habilite acesso público nem domínio r2.dev.
3. Em Configurações → Vínculos do seu Worker, acrescente um vínculo de bucket R2 com o nome exato **MANUAL_SUBTITLES**. Selecione o bucket criado.
4. No bucket, configure uma regra de ciclo de vida para excluir objetos com prefixo `manual/` após **um dia**. Essa regra remove sobras caso a TV ou o navegador sejam fechados antes de concluir. Não aplique a regra a buckets antigos de outros conteúdos.
5. Preserve os vínculos existentes **DB** e **ASSETS**, os segredos e a configuração atual de OpenSubtitles/Firebase. Não é necessário alterar o esquema ou apagar o banco.

## Publicação

- O arquivo `worker.js` é o módulo completo atualizado, pronto para substituir o código do Worker que foi enviado como texto. Publique pelo mesmo procedimento que você já utiliza na sua conta.
- O arquivo `site-public-atual-r26.zip` contém a pasta `public/` completa: seus arquivos atuais mais o envio manual. Publique essa pasta pelo procedimento atual dos assets do Worker. Se sua publicação usa Wrangler, mantenha seu wrangler.toml/jsonc, seus IDs e segredos; o ZIP não inventa ou substitui essas configurações.
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
