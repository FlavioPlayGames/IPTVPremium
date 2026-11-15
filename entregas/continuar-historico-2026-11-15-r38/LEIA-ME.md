# Continuar assistindo — correção r38

Base: r37, correspondente ao java.zip enviado para avaliação. Projeto padrão: C:\Users\flavi\AndroidStudioProjects\IPTVPremium.

## Aplicar e instalar

No PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File "$pasta\Atualizar-TV.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

Atualizar-TV aplica, compila com a configuração/assinatura locais e instala na TV já conectada e autorizada no ADB. Aplicar.ps1 somente aplica. O pacote verifica a base antes de gravar e cria recuperação externa em Downloads. Base divergente interrompe sem sobrescrever edições. A aplicação repetida reconhece os fontes já atualizados. Para desfazer use Desfazer.ps1 -Backup com o caminho mostrado na aplicação. Não muda pareamento nem limpa dados do aplicativo.

## Correções

- Série com histórico de outra lista/versão deixa de sumir porque a tela dos detalhes estava carregando ou falhou: somente resultado pronto registra episódios, e resposta vazia não apaga o mapeamento conhecido. A resposta válida continua vinculando IDs reais do provedor.
- Duração positiva não é aceita automaticamente: sem evento de fim, duração igual/inferior à posição é descartada. Queda superior a 10% em relação à duração anterior conhecida (ou duração do catálogo na ausência desta) conserva a referência válida. Sem referência coerente, salva progresso sem inferir conclusão pela duração suspeita.
- Callbacks de duração só são aceitos do mesmo ID, tipo e URL de reprodução. Evita contaminar o conteúdo atual com dados do player anterior durante a troca.
- Limpeza da posição legada e escolha do próximo episódio usam o resultado efetivamente salvo no histórico. Mantidos conclusão normal em 95%, evento real de fim, histórico por perfil e próxima reprodução pendente.

## Limites

Proteção conservadora contra durações inconsistentes, não medição independente da duração real. Uma edição genuinamente mais curta pode continuar em Continuar até o evento de fim se houver referência anterior maior. Não restaura automaticamente fatos antigos que já tenham sido gravados como concluídos: seria possível ressuscitar conteúdos realmente terminados. Assistir novamente gera o novo checkpoint normal.

Mantida a regra de mostrar somente obras encontradas na lista ativa. Nenhuma exclusão do histórico, alteração de capas, temas, layout, limite de memória, importador, site, chave ou assinatura. Confira VALIDACAO.md. Os testes de histórico são locais com classes reais compiladas; não houve teste físico na TV.
