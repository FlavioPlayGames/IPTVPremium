# Edição de lista: reavaliar Xtream e preservar ordem do provedor

Incremental sobre envio-lista-simples. Versão 1.13 mantida. Aplicativo e site não foram publicados/instalados automaticamente.

## Causas encontradas

Na versão anterior, um catálogo nativo M3U já existente era preservado sem tentar novamente Xtream. Além disso, editar e reenviar exatamente o mesmo link podia reaproveitar o catálogo pronto sem iniciar importação. Por isso a regra de tentar Xtream primeiro funcionava para cadastros novos, mas não cobria a edição relatada. O rótulo M3U do site e da lista de aparelhos mostrava o tipo do cadastro, sem distinguir o formato usado na importação.

O importador nativo M3U também não gravava providerOrder nos filmes. Com esse campo vazio, a consulta de ordenação padrão terminava usando o nome, produzindo A–Z.

## Correções

- Cada envio explícito de uma lista M3U pelo portal solicita reavaliação uma vez, inclusive quando o link é igual ao já cadastrado. A entrada mantém seu ID; não cria outra lista.
- A edição força uma nova solicitação de catálogo. A sincronização confirma novamente a API, ignorando a decisão M3U anterior para esse envio.
- O transporte Xtream só é confirmado depois de concluir a importação. O catálogo anterior continua ACTIVE enquanto a nova geração é preparada. Cancelamentos e erros locais de armazenamento não são tratados como motivo para baixar outra lista.
- A importação automática tenta Xtream até duas vezes, em vez de quatro. Só depois usa a M3U original quando essa alternativa for necessária. Atualizações comuns não reavaliam repetidamente listas nativas que já funcionam; o envio explícito pelo portal dispara essa reavaliação.
- Filmes M3U passam a gravar a posição da entrada. O modo Padrão respeita essa ordem; A–Z continua disponível quando escolhido pela pessoa. A correção da ordem aparece na próxima importação, sem alterar as preferências de ordenação do usuário.
- Aplicativo e site passam a distinguir Automático/Verificando, M3U confirmado e Xtream confirmado. O site recebe o transporte nos dados da TV; um link M3U sem confirmação não é apresentado como prova de importação nativa.

Não foi possível identificar daqui o formato realmente utilizado no aparelho só pelos rótulos antigos, nem medir a demora no provedor real. A correção cobre o caso reproduzido de editar a lista existente, com testes locais simulados. Links assinados, filtrados ou genéricos que não permitem inferir uma API continuam preservados.

## Aplicar

Extraia o ZIP em C:\Users\flavi\Downloads\lista-editada-xtream (arquivos na raiz, sem pasta duplicada). No PowerShell:

```powershell
Set-Location "C:\Users\flavi\Downloads\lista-editada-xtream"
powershell -ExecutionPolicy Bypass -File .\Aplicar.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium" -Testar
```

Somente após BUILD SUCCESSFUL:

```powershell
powershell -ExecutionPolicy Bypass -File .\Instalar-TV.ps1 -Projeto "C:\Users\flavi\AndroidStudioProjects\IPTVPremium"
powershell -ExecutionPolicy Bypass -File .\Aplicar-Site.ps1 -Site "C:\Users\flavi\Downloads\site-identidade-aprovada" -Testar
```

Após o teste do site concluir, publique:

```powershell
Set-Location "C:\Users\flavi\Downloads\site-identidade-aprovada"
npx.cmd wrangler deploy
```

Se a janela não encontrar Node, atualize o PATH da sessão antes do teste do site:

```powershell
$env:Path = "C:\Program Files\nodejs;" + [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
```

## Validar na sua TV

Com o aplicativo atualizado, abra a mesma TV no site, escolha Editar na lista existente e clique Enviar lista, mesmo que não altere o link. Não exclua nem cadastre outra entrada. Essa nova entrega disparará a reavaliação. Aguarde a importação concluir e confira Padrão nos filmes. Consulte os dados do aparelho novamente para receber o transporte confirmado.

Os instaladores validam as versões e criam backup; Desfazer.ps1 e Desfazer-Site.ps1 restauram com proteção de conflitos. O patch do site não muda configuração Cloudflare, D1, domínio, agendamento ou segredos. Não apaga histórico/favoritos nem executa migrações de banco. A correspondência de conteúdos continua seguindo as regras já existentes do aplicativo.
