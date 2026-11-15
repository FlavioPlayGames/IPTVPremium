# Layouts de configurações e Minhas Listas r34

Base obrigatória: r33 já aplicada no projeto C:\Users\flavi\AndroidStudioProjects\IPTVPremium.

## Instalar

Feche o aplicativo e use o PowerShell do Android Studio onde a TV já está conectada. Execute Atualizar-TV.ps1 -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium". O script aplica, compila localmente e instala mantendo assinatura e dados. Não altera o pareamento do ADB.

O aplicador verifica hashes antes de gravar e guarda recuperação fora do projeto, em Downloads. Se houver fonte divergente, interrompe sem sobrescrever. Desfazer.ps1 -Backup "pasta informada" -Instalar restaura, protegendo edições posteriores. Aplicar.ps1 permite apenas aplicar os fontes, sem compilar/instalar.

## Layout aprovado

- Configurações mantém grade 3×3; cabeçalhos e margens seguem padrão compartilhado com as telas internas.
- Telas internas com conteúdo limitado em largura, painéis agrupados, rolagem quando necessária e foco pelo controle remoto. Assinatura, categorias, reprodução, histórico, atualização e proteção mantêm suas funções.
- Legendas em três cartões: Contas de legendas, Envio manual e Aparência. Contas preserva os três provedores. Conexão e envio mantêm QR reais e formulários existentes.
- Envio manual: cartões compactos Filme/Série, painéis de escolha/destino e resumo com largura proporcional.
- Aparência: menu compacto; as opções de cada grupo são abertas no próprio painel. Abaixo, prévias reais da Home com a cor/modo selecionados, do visual Kids e da grade de capas. As prévias recebem foco para serem alcançadas pelo controle e trazidas para a área visível.
- Minhas Listas: listas à esquerda e ações à direita, sem o grande painel externo. Ícone da lixeira vermelho. Na lista ativa, bolinha verde e Conectado imediatamente abaixo do nome; protocolo aparece na linha seguinte. Conectado representa a lista ativa local, não uma sondagem de disponibilidade do servidor. A seleção de outra lista não a ativa automaticamente.

## Cores e fundos preservados

Este pacote muda layout. As paletas, cores dos temas, modos claro/escuro, Kids e imagens estáticas de fundo são os existentes. Nenhuma arte, fundo desenhado, gradiente de cenário novo ou biblioteca foi incluído. Os backdrops usam os renderizadores existentes. As exceções de estado solicitadas são lixeira vermelha e Conectado verde.

Nenhuma alteração de importação, legendas/provedores, sincronização, player, dados, armazenamento, limites de cache ou site está incluída. O problema de recarregar capas genéricas permanece para uma entrega separada.

Confira VALIDACAO.md. A aparência real e a navegação em TV devem ser conferidas após instalar; as imagens de aprovação são propostas, não capturas reais do APK.
