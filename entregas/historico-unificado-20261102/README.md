# Histórico e favoritos unificados

Pacote incremental para o java.zip enviado nesta conversa. Aplicar.ps1 valida os fontes e cria backup antes de alterar. Recusa versões desconhecidas. Desfazer.ps1 restaura o backup e protege alterações posteriores.

- Histórico acumulado no aparelho entre listas. Cada lista apresenta apenas títulos disponíveis nela, com uma edição por obra. Títulos ausentes ficam guardados para listas futuras, respeitado o limite de 400 registros canônicos.
- Filmes de Favoritos e Continuar alimentam a busca entre listas. Identidade por IMDb/TMDB ou título normalizado e ano, com títulos alternativos confirmados. Qualidade/idioma podem mudar quando a edição anterior não existe. Não usa semelhança vaga para trocar um filme por outro.
- Favoritos de filmes e séries do player e do catálogo são os mesmos. Favoritos antigos exclusivos do player são incorporados uma vez. Perfis normal e Kids permanecem isolados. Marcações individuais de episódios são preservadas.
- Progresso salvo a cada 10 segundos e ao pausar, sair ou colocar em segundo plano. Mantidos os limites de 10 segundos de posição para Continuar e 95% para conclusão.
- Episódios concluídos ficam registrados; o próximo só é indicado quando há um episódio real disponível. Remover de Continuar também impede que posições antigas reapareçam.
- Ajuste automático e visual aprovados preservados.

Extraia o ZIP e execute no PowerShell:

```powershell
& '.\Aplicar.ps1' -Projeto 'C:\Users\flavi\AndroidStudioProjects\IPTVPremium' -Testar
```

Após BUILD SUCCESSFUL, use Instalar-TV.ps1 com o mesmo -Projeto. Sem APK ou configurações privadas neste pacote. Não sincroniza entre aparelhos. Validação na TV ainda necessária.
