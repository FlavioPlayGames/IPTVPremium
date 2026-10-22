Preservar o formato da lista

Uma lista cadastrada como Xtream, inclusive quando convertida de M3U no portal, passava para o importador M3U quando a API Xtream apresentava erro e havia um link original disponível. Agora a atualização usa apenas o formato cadastrado. M3U nativa continua M3U. Se Xtream falhar, o erro é informado e a geração ativa anterior é preservada. O link original continua salvo como metadado; o cadastro e as credenciais não são alterados.

Alteração restrita a M3uSourceSynchronizer.kt; ícones, navegação, atualização da lista ativa, limites de cache e lotes de importação não foram alterados. Backup automático e verificação da versão antes de gravar.
