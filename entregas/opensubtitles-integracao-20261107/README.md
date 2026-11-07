# SubDL + OpenSubtitles

Substitui SubSource por OpenSubtitles na busca e nas configurações do aplicativo. Mantém o SubDL com chave pessoal e o visual do player. Versão 1.13 preservada.

No OpenSubtitles, cada pessoa entra com usuário e senha da própria conta OpenSubtitles.com em Configurações > Legendas > OpenSubtitles. Não pedimos chave de API ao usuário. A senha fica apenas na memória durante o login; o token de sessão é criptografado pelo Android Keystore e gravado fora do backup. A sessão pode expirar e exigir novo login.

## Habilitação necessária antes do teste real

O responsável pelo aplicativo precisa registrar o aplicativo na API do OpenSubtitles e configurar sua chave técnica e User-Agent no build. Isso é diferente do cadastro de cada usuário. Não foi fornecida uma chave nesta conversa; o pacote contém o campo vazio, sem credenciais compartilhadas ou de terceiros. Até essa configuração, o app mostra que o OpenSubtitles ainda não está disponível nesta versão e mantém o SubDL funcionando. Consulte CONFIGURACAO-APP.md. Não há teste real de login, busca ou download no serviço nesta entrega.

## Comportamento

A pesquisa começa somente ao abrir o menu Legendas de filmes/séries. Canais ao vivo continuam com suas faixas nativas. OpenSubtitles consulta português brasileiro, inglês e espanhol. PT-BR vem primeiro; o SubDL mantém sua seleção em português. Os nomes dos idiomas aparecem corretamente no menu. A busca verifica obra, ano quando disponível, temporada e episódio; não escolhe outra obra ou episódio para preencher a lista. A disponibilidade depende dos catálogos e da identificação do título.

Pesquisa não solicita link de download nem baixa arquivos de legenda. Somente a escolha explícita de uma opção pede o download. Arquivos válidos já no cache existente são reutilizados antes de qualquer pedido de link. O OpenSubtitles recebe file_id, não subtitle_id. O POST /download pode consumir a cota da conta; sem seleção não é chamado. Não há download automático da primeira opção ou de todos os idiomas.

A consulta pode pedir os três idiomas juntos. Se a resposta tiver múltiplas páginas e um idioma não aparecer na primeira página, há busca complementar somente para os idiomas faltantes. Máximo de 30 opções por idioma. Resultados são guardados por 30 minutos; ausência confirmada por 10 minutos. Falhas de autenticação/rede não viram cache vazio. HTTP 429 respeita Retry-After; não há limite diário artificial de pesquisas no app. Os limites reais são os do serviço, sem prometer pesquisas ou downloads ilimitados.

O cache é separado por conta e mantém a identidade da mesma conta ao renovar o login. Troca/logout não aplicam resultados de outra conta; um erro antigo de autenticação não derruba uma sessão nova. URLs temporárias de download não ficam salvas no cache. API-Key e token não são enviados ao host de download. Credenciais só trafegam por HTTPS, com hosts oficiais validados e sem encaminhar autenticação após redirecionamentos.

SubSource deixa de fazer chamadas e sai da tela. O portal/QR existente permanece para SubDL; OpenSubtitles usa login no aparelho. Não alteramos o site do portal: envios novos de chave SubSource pela central antiga são recusados, remoções antigas são aceitas e o snapshot mantém o esquema anterior com SubSource inativo. As chaves antigas nunca são reutilizadas como credenciais OpenSubtitles. Sem limpeza de histórico, migração de banco, alteração de fontes de vídeo ou de qualidade.

## Aplicação

Extraia e execute no PowerShell dentro da pasta:

```powershell
.Aplicar.ps1 -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, use Instalar-TV.ps1 com o mesmo -Projeto. Inclui backup automático e Desfazer.ps1. Instalação incremental sobre o java.zip enviado; fontes diferentes ou pacote corrompido são recusados. A configuração privada da chave técnica não é substituída pelo instalador. Sem APK ou configurações pessoais no pacote.

Na TV, após habilitar a chave do app: testar login válido/inválido, expiração, logout e troca de conta, três idiomas, filmes/episódios, seleção e cache, limite de download e retorno ao player. A falha HEVC do VLC observada anteriormente não é corrigida por esta troca de provedor.
