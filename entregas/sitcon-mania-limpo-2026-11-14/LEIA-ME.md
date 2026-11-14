# Substituir a pasta do projeto

1. Feche o projeto no Android Studio.
2. Renomeie a pasta antiga SitconMania para SitconMania-backup.
3. Extraia o ZIP novo no mesmo local; ele cria uma nova pasta SitconMania.
4. Abra a nova pasta no Android Studio e sincronize o Gradle.
5. Compile e teste antes de remover o backup antigo.

AGP corrigido para 9.4.0, compatível com o limite informado. Android mínimo 9 (API28). Caches, .idea, build e local.properties foram removidos. O Android Studio recria suas configurações locais; mantenha o SDK37/JDK25 conforme README.

Os 15 Kotlin e recursos do ZIP recebido coincidem com a base validada; após corrigir a linha AGP para9.4.0, todos os 60 arquivos de entrada coincidem byte a byte com o projeto anteriormente compilado/testado. Não houve alteração nas regras ou telas.
