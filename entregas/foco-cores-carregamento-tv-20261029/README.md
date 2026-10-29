# Foco, cores e carregamento

Base: java.zip enviado em 29/10. Corrige recorte de titulos rolantes e transicoes de foco entre grade, categorias e toolbar; limpa destino anterior ao reabrir a mesma categoria. Cores mais vivas na interface, preservando o amarelo aprovado e a arte da Home. Nao modifica o motor de rolagem.

Busca externa de legendas passa a ocorrer ao abrir Legendas. Registro leve mede preparo e buffering de ambos os motores, sem URL/credenciais. Nao altera buffers ou decoders e nao promete eliminar pausas de rede.

Baixe `foco-cores-carregamento-tv.zip`, execute `Aplicar.ps1 -Projeto <pasta> -Testar` e `Instalar-TV.ps1`. Backup e `Desfazer.ps1` incluidos. Se o carregamento persistir, execute `Capturar-Carregamento.ps1`, abra o mesmo video em ambos os players e envie o TXT da Area de Trabalho.

[Validacao e limites](VALIDACAO.txt). [Detalhes](LEIA-ME.txt).
