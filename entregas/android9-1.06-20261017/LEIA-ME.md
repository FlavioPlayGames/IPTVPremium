# Android 9 ou superior — IPTV Premium1.06 / número interno8

Reduz o mínimo de instalação de Android12/API31 para Android9/API28. Mantém compileSdk/targetSdk37, dependências e os cinco APKs por arquitetura/Universal, applicationId e assinatura local. Não usa overrideLibrary para forçar dependências incompatíveis. Os manifestos das dependências são validados na compilação. Corrige também declarações de adesão a APIs experimentais do Media3 com a anotação AndroidX apropriada, sem mudar a lógica do player nem desativar verificações.

O desfoque de fundos estáticos em Filmes/Séries, Minhas listas e ativação tem alternativa para Android9–11: uma imagem temática reduzida a no máximo384x216 é desfocada fora da thread da interface e reutilizada dentro do orçamento compartilhado de40MiB das artes. No Android12+ mantém o efeito original. Preserva cores, fundos e contornos, sem alterar imagens de capas ou animações. Outros desfoques opcionais de sobreposição do Compose seguem o comportamento da biblioteca nas versões antigas.

A versão permanece1.06/code8: aplique sobre o projeto com os pacotes anteriores, compile e instale por cima para testar. A atualização da biblioteca entre listas permanece. Não desinstale, limpe dados, recrie banco nem troque a chave de assinatura.

Preparar-Android9-1.06.ps1 valida todos os fontes/hashes antes de aplicar, faz backup, preserva configuração local e dados e detecta o Java do AndroidStudio. -Verificar apenas confere; -NaoCompilar aplica sem compilar. Arquivos locais diferentes interrompem o processo antes de sobrescrever.

A compilação e análise estática verificam as dependências e APIs; a verificação de manifestos/APKs confirma minSdk28. Os testes do desfoque cobrem cores dos sete temas, opacidade, limites e entrada preservada. A execução, reprodução de vídeo e instalação em um Android9 real ainda precisam ser testadas no aparelho: este ambiente não tem um emulador Android9 em execução. Os codecs/HDR continuam dependendo do dispositivo.

Instale o APK compatível com a arquitetura do aparelho ou o Universal. Não é necessário mudar o site/Cloudflare para esta correção. O script não publica APKs nem faz instalação automática.
