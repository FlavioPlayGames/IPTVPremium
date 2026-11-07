# Central IPTV Premium — visual aprovado

Projeto completo reconstruído a partir do Worker recuperado e das páginas disponíveis. Não é necessário ter a antiga pasta do site para abrir este projeto. Aplicativo Android 1.13 mantido; esta entrega altera o site. Nenhum site ou banco em produção foi alterado.

## O que mudou

- Fundo grafite, painéis translúcidos mais opacos, contornos dourados e seleção amarela.
- Menu inferior no celular e menu lateral no computador. Listas, Legendas e Aparelho separados.
- Ao entrar com MAC e código, ou abrir uma TV no administrador, consulta automaticamente os dados, mesmo se já houver uma cópia anterior. Não existe botão de buscar listas.
- Mostra os dados recebidos anteriormente enquanto espera a TV responder. A existência desses dados não é apresentada como prova de que o aparelho está conectado neste momento. Erros de consulta mantêm a cópia anterior com explicação. Uma resposta atrasada não troca a TV selecionada.
- Adicionar/editar lista: M3U ou Xtream, verificar, conferir e enviar. “Voltar e editar” preserva os dados; nada é enviado antes da confirmação. O administrador mantém a opção M3U direta.
- Listas só mostram os seus envios. Legendas mostram as conexões e as preferências de legenda.
- SubDL e OpenSubtitles têm telas de conexão próprias, links para cadastro oficial e status de recebimento. A aparência das legendas permanece separada. Senhas e chaves não ficam no armazenamento do navegador.
- Aparelho reúne nome, MAC, licença, renovação e saída. Administração mantém pesquisa, filtros, seleção, ações em lote, prazos, Pix e publicação de APK por arquitetura. No celular, a tabela de aparelhos vira cartões.

## Extrair e testar no Windows

O ZIP tem os arquivos diretamente na raiz, sem uma segunda pasta de mesmo nome. No PowerShell:

```powershell
Expand-Archive -LiteralPath "C:\Users\flavi\Downloads\site-identidade-aprovada.zip" -DestinationPath "C:\Users\flavi\Downloads\site-identidade-aprovada" -Force
Set-Location "C:\Users\flavi\Downloads\site-identidade-aprovada"
powershell -ExecutionPolicy Bypass -File .\Testar-Site.ps1
```

Requer Node.js 22 ou mais recente. O teste instala as dependências locais quando necessário, verifica JavaScript e executa o empacotamento Cloudflare com `--dry-run`. Não publica e não executa comandos D1, migrações ou operações de banco.

## Antes da publicação

O `wrangler.jsonc` é o rascunho de configuração que você enviou anteriormente, preservado. O Worker recuperado corresponde à base de código usada aqui, mas o texto do Worker não contém os metadados da implantação. Confirme no painel da Cloudflare o nome do Worker existente, o vínculo DB com o D1 existente, data/flags de compatibilidade, domínios/rotas, agendamento e configuração de Assets. Não crie outro banco.

Os segredos que já estão na Cloudflare continuam lá. `keep_vars: true` preserva as variáveis existentes; não coloque APP_KEY, senha administrativa, chaves privadas ou arquivos de recuperação no código. A mesma APP_KEY é necessária para ler os dados criptografados existentes; não a substitua. O projeto não contém os valores dos segredos.

A publicação deve atualizar o Worker existente com a pasta `public` como Assets. Não publique só o JavaScript do Worker: as páginas, scripts, estilos, logo, fontes e QR precisam acompanhar o servidor. Este pacote não publica automaticamente. Guarde a versão atual do serviço antes de publicar.

## OpenSubtitles

O usuário precisa apenas de sua conta do OpenSubtitles.com. Para habilitar o serviço, o responsável pelo aplicativo deve registrar e configurar a mesma chave técnica no APK e no servidor, nos segredos `OPENSUBTITLES_API_KEY` e `OPENSUBTITLES_USER_AGENT`. Isso ainda não foi feito nesta conversa. O site explica a indisponibilidade em vez de pedir uma senha quando a integração não está pronta. O SubDL usa a chave pessoal do usuário.

O cadastro/conexão não baixa legendas; o download continua ocorrendo somente ao selecionar uma legenda no player. A senha do OpenSubtitles é usada na autenticação oficial e não é salva ou enviada à TV; apenas uma sessão validada segue criptografada e vinculada à TV correta.

## Recuperação e validação

O Worker original recuperado está em `recuperacao/worker-publicado-original.mjs`, para referência e preservação. O ponto de entrada atualizado é `worker-atualizacao-abi.js`, na raiz. O empacotamento da base anterior corresponde ao Worker recuperado, desconsiderando só comentários e espaços em branco. As alterações do servidor acrescentam a integração OpenSubtitles preparada anteriormente, sem remover os endpoints existentes e sem mudar o esquema do banco.

A pasta `previas` traz capturas reais do site em navegador com dados simulados, incluindo celular e computador. `validacao` contém os resultados dos testes. Foram verificados navegação, listas, confirmação de envio, consultas automáticas de usuário/administrador, separação de envios, credenciais, Pix, administração e larguras de 360, 390, 768 e 1440 pixels.

Não houve teste com a TV real, conta real do OpenSubtitles ou publicação. Para a consulta automática receber dados novos, a TV precisa estar conectada e o aplicativo deve conseguir sincronizar. O site não inventa dados nem marca como concluído um envio que ainda não foi recebido.

## Repetir os testes de código

`npm test` executa os testes de servidor com SQLite em memória e provedor simulado. Não usa o banco remoto. Para repetir os testes de navegador opcionais: instale Playwright (`npm install --no-save playwright` e `npx playwright install chromium`) e execute `node validacao/browser.test.mjs`. Eles usam somente o servidor local e respostas simuladas.
