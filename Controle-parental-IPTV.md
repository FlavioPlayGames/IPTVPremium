# Controle parental por categorias e cadastro manual

O controle parental agora tem opções independentes:

- **Geral:** pedir PIN ao abrir ou voltar ao aplicativo.
- **Filmes:** escolher a lista e bloquear categorias individuais ou todos os filmes daquela lista.
- **Séries:** o mesmo controle, separado dos filmes e das outras listas.
- **Canais:** um filtro de conteúdo adulto, sem exigir a seleção de centenas de canais.

## Como usar

Em **Configurações > Controle parental**, crie um PIN de 4 a 8 números ou valide o PIN existente. Depois escolha Filmes ou Séries, selecione a lista e marque as categorias. As alterações são salvas automaticamente. A pesquisa e a paginação facilitam listas com muitas categorias.

As categorias bloqueadas continuam visíveis com cadeado. Ao selecionar uma delas, o aplicativo solicita o PIN antes de trocar a categoria. O PIN libera somente aquela categoria durante a sessão; ao sair do aplicativo, ela fica bloqueada novamente. A opção de bloquear novamente nos controles permite encerrar as liberações sem sair.

Enquanto bloqueados, os títulos dessas categorias são removidos de Todos, buscas, favoritos e Continuar assistindo. A reprodução e os detalhes também verificam a proteção, inclusive os episódios. Os favoritos e as posições do histórico não são apagados: reaparecem quando houver autorização.

O filtro de canais considera o nome do canal e sua categoria: Adultos, XXX, 18+, Porn, Sexy Hot e outras marcas adultas reconhecidas. Ele não analisa o vídeo nem consegue identificar com certeza um canal que o provedor forneça sem nome ou categoria indicativos. Autorizar uma categoria adulta por PIN libera o filtro adulto durante aquela sessão. A proteção também é aplicada à prévia e à troca de canais.

As regras de filmes e séries são separadas por lista e tipo. O bloqueio de todos os títulos da seção inclui futuras categorias; uma autorização individual libera apenas a categoria escolhida. Alterar o PIN conserva as regras. Desativar o controle exige acesso aos controles por PIN e confirmação, e desativa os filtros.

PINs já existentes continuam protegendo a abertura do aplicativo, preservando o comportamento anterior. Ao criar um PIN novo, o responsável escolhe se quer ativar o bloqueio geral, além dos filtros. O limite de cinco erros e espera de 30 segundos é compartilhado pelos pedidos de PIN.

## Cadastro manual da lista

No primeiro passo, **Voltar** fica na parte inferior esquerda, alinhado a **Continuar**. Ao selecionar Xtream Codes ou M3U, o foco vai para Continuar depois que o botão é habilitado. O próximo OK confirma a escolha; não há avanço automático para o formulário.

## Instalação

Execute **Ampliar-controle-parental.ps1** na raiz do projeto Android. É uma entrega cumulativa que inclui o ajuste de cadastro manual e conserva os temas e fundos aprovados. O script verifica arquivos e hashes antes de escrever, recusa versões incompatíveis e guarda backup em `.iptv-backups/parental-DATA-HORA`.

Depois execute **Gerar-APKs.ps1** e instale o APK apropriado sobre o atual, sem desinstalar e com a assinatura original. `local.properties`, `google-services.json`, versão e assinatura do projeto não são modificados.

## Validação e limites

- AssembleDebug dos cinco APKs concluído, com a atualização do cadastro manual.
- Lógica Kotlin de regras e PIN testada em JVM com adaptadores mínimos de Context e AtomicFile: hash, validação, tentativas, migração do comportamento anterior, regras por lista/tipo/categoria, bloqueio geral da seção, categorias futuras/sem categoria, marcadores adultos, concessão/revogação de autorização, persistência, desativação e falha fechada para regras corrompidas.
- As oito consultas reais de paginação foram executadas em SQLite com categorias permitidas/bloqueadas, listas e gerações distintas, favoritos, categorias nulas/vazias, bloqueio de toda a seção e liberação de uma categoria.
- As exclusões/autorizações são aplicadas antes de materializar as páginas; a verificação de conteúdo continua ativa como segunda barreira. Até 400 IDs por lista SQL mantêm os parâmetros controlados; eventuais regras adicionais continuam sendo verificadas por item.
- O controle usa somente dados locais do catálogo e regras pequenas, sem requisições periódicas novas ao site.
- Instalador testado com versões compatíveis anteriores e com a instalação atual: backup, verificação sem escrita, repetição sem alterações e preservação dos arquivos privados.
- A navegação física com controle, teclado virtual e solicitações de PIN ainda deve ser conferida no aparelho do usuário; não foi executado teste em TV/celular físico neste ambiente.
