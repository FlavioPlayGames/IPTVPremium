# Avaliações pessoais, cabeçalho de referência e prioridade ao selecionar mídia — 1.8

## Filmes e séries

O cabeçalho segue a referência enviada: logo do título, cápsulas arredondadas com ícones para nota pública/ano/duração/gêneros, sinopse clara, Assistir com borda vermelha, coração e cinco estrelas com Avalie. Esse cabeçalho usa cores fixas em todos os temas, sem recoloração pelo tema ativo. Não inclui o rótulo numérico da imagem de demonstração.

A sinopse completa continua acessível. No cabeçalho compacto, o próprio resumo pode receber foco e abrir o texto completo com OK. Elenco, temporadas e episódios são preservados. O cabeçalho se compacta em telas baixas e tem área de rolagem quando necessário para não sobrepor as fileiras inferiores. As dimensões dos cards de elenco/episódios não foram aumentadas nem cortadas.

## Nota pessoal

Avalie abre a escolha de 1 a 5 estrelas. No controle remoto, esquerda/direita move entre estrelas e OK/Enter confirma. Fechar/Voltar cancela; uma nota existente pode ser alterada ou removida.

A avaliação é pessoal, salva no aparelho e separada da nota pública do provedor/TMDB/IMDb. Não altera as notas públicas da ficha ou da grade e não é enviada à central. Fica em preferências privadas, fora do cache descartável: reiniciar, instalar APK com `-r` ou limpar apenas cache preserva a nota. Apagar os dados/desinstalar o app pode removê-la, como os demais dados locais.

Filmes e séries têm espaços separados. IDs oficiais ligam a mesma obra entre listas quando disponíveis; o fallback local combina lista, ID do provedor e identidade do título/ano. Reutilizar o mesmo ID para outro filme não transfere a nota. Metadados chegando depois e alterações/remoções entre aliases preservam a escolha mais recente. As chaves são hashes; não grava URLs de streams ou credenciais. Nenhuma classificação de todos os filmes é necessária.

## Prioridade na abertura

O clique ativa a prioridade antes de navegar. Durante preparação, detalhes ou reprodução:

- a antecipação de capas da categoria pausa e cancela cooperativamente seu lote; conexões HTTP de antecipação fora da tela são desconectadas em IO;
- o carrossel reutiliza capas vizinhas já em RAM, sem iniciar downloads dos vizinhos enquanto o filme selecionado abre;
- a preparação do filme prioriza fundo/logo e não espera baixar até 12 retratos de atores. Os cards de elenco carregam suas fotos depois, na ficha;
- o logo exibido reutiliza as mesmas dimensões de decodificação do cache da preparação;
- novas atualizações automáticas de catálogo são adiadas; consultas remotas do histórico cedem entre operações; a manutenção de armazenamento cede de forma cooperativa;
- a série clicada reaproveita imediatamente os dados presentes no Paging, enquanto consulta seus dados reais.

Ao voltar à categoria, a antecipação retoma. Permanecem grupos de 5 e limites de 125 arquivos de filmes/125 de séries, sem ampliar o orçamento de bitmaps. Pedidos remotos já iniciados e o tempo do provedor ainda influenciam a abertura; não é uma garantia de força bruta do Android ou de latência zero.

Os **3 segundos obrigatórios da preparação dos filmes** e as regras atuais de abertura das séries são preservados. Licença, controle parental, filtros Kids, histórico e listas não são apagados. A entrega também mantém a remoção da aba automática Legendados e da exclusividade que atrasava as consultas.

## Instalar e testar no emulador

Na raiz do projeto: `Preparar-Avaliacoes-Prioridade-1.8.ps1 -Emulador emulator-5554 -Testar`.

Confere SHA-256 e todos os arquivos antes de gravar, cria backup em `.navigation-backups/avaliacoes-detalhes-prioridade-*`, aplica 17 arquivos, compila e instala sem desinstalar/limpar dados. Aceita a entrega anterior de categorias ou a remoção da aba já aplicada. Preserva versão 1.8 e os recursos Kids locais; não altera Gradle, local.properties ou schema do banco. Não força importação da lista. Não há APKs nesta entrega.

Resultado esperado: **OK (83 tests)**. Arquivo de diagnóstico: `diagnostico-avaliacoes-detalhes-prioridade-testes.txt`.

Opções `-Verificar`, `-SomenteAplicar`, `-Projeto` e `-PacoteLocal` disponíveis. `-Medir` mantém a medição opcional de abertura geral do app, não mede o clique em uma mídia. Código local diferente dos arquivos conhecidos bloqueia a aplicação para preservar alterações.

## Verificação realizada

- APK debug, APK de testes e Kotlin release compilados.
- 12 testes JVM do código de identidade e prioridade: aprovados.
- 11 testes JVM da lógica real do armazenamento pessoal usando um adaptador SharedPreferences em memória: aprovados. Não representam persistência real no Android; essa persistência está nos testes nativos.
- Instalador PowerShell validado em cópias antes/depois da remoção da aba: validação sem escrita, backup exato, preservação de arquivos/configurações e recusa atômica de alterações desconhecidas.
- 83 testes Android compilados, incluindo 29 novos casos de identidade, armazenamento com preferências Android isoladas, cache, prioridade e interface/controle remoto. **Não executados na nuvem**, que não tem emulador. O comando acima executa-os no emulador do usuário.
- O desempenho e a aparência na TV não foram medidos/renderizados aqui. Como anteriormente, a compilação usa aliases temporários para seis imagens Kids ausentes do upload Java; esses aliases não são entregues e não substituem os recursos Kids reais.
