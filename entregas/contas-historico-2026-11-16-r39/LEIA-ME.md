# Contas e histórico persistentes r39

Base do aplicativo: r38. Projeto: C:\Users\flavi\AndroidStudioProjects\IPTVPremium.
Site oficial: C:\Users\flavi\AndroidStudioProjects\Site IPTV.

## Ordem

1. Aplicar e publicar o site com site/Atualizar-Publicar.ps1 -Site no caminho oficial. Não precisa R2 nem novas chaves técnicas; usa DB e criptografia já configurados. As tabelas persistentes são criadas na primeira chamada autenticada.
2. Executar Atualizar-TV.ps1 -Projeto no projeto Android. Requer TV conectada/autorizada no ADB e configurações de assinatura existentes. Aplicar.ps1 apenas aplica fontes, sem instalar.
3. Fazer um novo login OpenSubtitles pelo site uma vez. Logins anteriores entregavam apenas o token; não existe senha recuperável para renovar esses tokens antigos. O novo login é armazenado criptografado na central e removido ao desconectar.
4. No aplicativo, usar Meu aparelho → Sincronizar agora antes do primeiro teste de limpar dados. Só dados enviados com sucesso podem ser recuperados; alterações offline ou uma limpeza antes de concluir o envio ainda podem ser perdidas.

## Recuperação

Reconhece o cadastro existente pela identificação estável usada para recuperar o MAC, sem usar o MAC exibido como senha. Na mesma TV, usuário Android e assinatura do aplicativo, restaura as listas cadastradas, lista ativa, progresso/histórico normal e Kids, vínculos/favoritos e chaves SubDL/SubSource. OpenSubtitles recupera sua sessão pela conta criptografada na central.

Restaura os cadastros de todas as listas, mas solicita importação somente da lista ativa. As demais são carregadas nas trocas. Não copia catálogos, capas ou arquivos de legendas. Legendas manuais e arquivos baixados estão fora, conforme a definição aprovada.

O backup é atualizado por mudanças dos dados; envios cedem prioridade à abertura de conteúdo, agrupam mudanças próximas e repetem falhas. Não há reenvio contínuo de backup idêntico. A cópia da central não expira com a fila de comandos e não é apagada por Limpar dados/cache no Android. Exclusões confirmadas no aplicativo atualizam o estado salvo: não ressuscita listas ou histórico excluídos. Remover o cadastro da TV na central também remove os dados vinculados.

## Contas

O token OpenSubtitles continua tendo validade definida pelo provedor. O APK mantém a conta e renova a sessão quando necessário, em vez de desconectar ao expirar. Busca/download continuam autenticados por usuário. Não repete automaticamente um POST de download após 401 para evitar consumo duplicado de cota. Erro temporário de rede não apaga a conta. Troca de senha/revogação no provedor ainda pode exigir novo login. Desconectar revoga a renovação na central; respostas de login/renovação anteriores à saída não podem reconectar a conta.

Chaves/listas/histórico são enviados por HTTPS e criptografados em repouso com a chave existente da central. A senha OpenSubtitles não é gravada no APK nem na fila de entrega; a central guarda a credencial criptografada para renovar a sessão. A interface do site informa esse comportamento. O backup não copia ciphertext do Android Keystore, que deixaria de funcionar após limpar dados.

## Proteções e limites

Aplicadores verificam hashes e criam recuperação externa antes de alterar fontes. Base divergente bloqueia a operação; não restaure Worker antigo por cima de edições recentes. O aplicador do site é baseado nos últimos fontes r31 disponíveis neste ambiente e preserva os demais arquivos/configurações. Se o Worker atual tiver mudanças posteriores, precisa rebase antes de aplicar. Desfazer os fontes não apaga dados já salvos na central.

Backup lógico comprimido: até 4 MB descomprimidos/220000 caracteres Base64. Ao exceder, mantém dados locais e última cópia válida, sem truncar. Sem alteração de orçamento de memória/cache, layout, fundo, importador ou assinatura. Limpar dados remove a cópia local; a recuperação exige internet e acesso à central. Reset de fábrica, troca de usuário Android ou assinatura pode mudar a identificação e exigir recuperação administrativa.

Testes locais em VALIDACAO.md. Não foi executada limpeza real na TV nem publicação no Cloudflare neste ambiente.
