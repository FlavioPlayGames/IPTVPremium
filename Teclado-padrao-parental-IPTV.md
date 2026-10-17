# Teclado padrão aprovado na busca parental

Reutiliza o mesmo AppKeyboard de Filmes, com suas teclas, dimensões, cores e contornos originais. O componente aparece diretamente na parte inferior da tela, sem Dialog, escurecimento, título, moldura externa ou campo adicional. O texto digitado e o cursor aparecem na busca do cabeçalho.

Enter na busca parental continua sendo encaminhado à tecla virtual em foco, sem chamar Concluir antecipadamente. Concluir e Voltar fecham somente o teclado e devolvem o foco à busca. Os contornos metálicos da tela de categorias são preservados; não se aplicam novos contornos às teclas.

Compatível com as duas atualizações anteriores: Ajustar-parental-referencia e Corrigir-teclado-busca-parental. Valida dois arquivos antes de escrever, cria backup em .iptv-backups/teclado-padrao-* e pode ser repetido. Não altera regras, PIN, assinatura, versão, Firebase ou Android mínimo.

Na raiz do projeto, execute Usar-teclado-padrao-parental.ps1 e depois .\gradlew.bat :app:assembleDebug. Instale com a mesma assinatura sobre o app existente, sem desinstalar.

Verificação: cinco variantes debug compiladas; teste do manipulador de Enter/numpad Enter/letras/apagar passou; instalador testado em simulação, aplicação, backup, repetição, BOM/CRLF e rejeição de fontes desconhecidas sem escrita. Aparência e foco físico na TV ainda precisam ser conferidos no aparelho.
