# Auditoria local do projeto IPTV Premium

Auditoria de metadados, sem alteracao ou exclusao no projeto. Percorre somente a pasta indicada, sem seguir links/juncoes, e exporta caminhos relativos, tamanhos e datas. Nao abre o conteudo dos fontes, listas, configuracoes ou arquivos de assinatura. Os relatorios sao gravados em Downloads, fora do projeto.

Arquivos gerados: arquivos.csv, pastas.csv (subtotais de ate dois niveis), avisos.csv e LEIA-ME.txt, reunidos em um ZIP pequeno chamado IPTV-auditoria-DATA-HORA.zip. Links e pastas inacessiveis aparecem nos avisos e nao participam dos totais. Os grupos "possivel backup" e "gerado/cache" sao indicios para revisao, nao uma lista autorizada de exclusoes. Nao some linhas de pastas.csv: os subtotais se sobrepoem.

O tamanho do projeto no PC nao e o tamanho instalado do aplicativo. Gradle, configuracoes e ferramentas de compilacao precisam ser preservados mesmo quando nao entram no APK. Assinatura, configuracoes privadas e fontes fora da pasta nao sao coletados como conteudo.

Salve Auditar-Projeto.ps1 em Downloads e execute:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\Auditar-Projeto.ps1" -Projeto "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium"
```

Envie somente o ZIP dos relatorios indicado ao final. Nao e necessario enviar os 4,32 GB do projeto. Prefira executar sem uma compilacao em andamento para obter tamanhos estaveis. Caso a auditoria encontre um problema, o arquivo de avisos registra a limitacao.

Validacao em PowerShell 7: inventario/ZIP, hashes do projeto inalterados, conteudos de teste privados ausentes do ZIP, links ignorados e recusa de gravar relatorios dentro do projeto. Sintaxe destinada tambem ao Windows PowerShell 5.1; execucao real no Windows ainda depende do teste local. Nao faz analise de referencias Kotlin/XML/Gradle, hashes de duplicatas nem verifica uso de recursos: essas etapas usam os fontes e o inventario posteriormente.
