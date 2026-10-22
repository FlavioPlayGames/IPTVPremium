Instalador 1.11 compatível com maxOf

Reconhece o formato enviado pelo usuário: versionCode = maxOf(12, localProperties.getProperty("APP_VERSION_CODE", "12").toInt()).also { ... } e versionName = "1.10". Define nome 1.11 e código interno 13; se houver um código local maior, incrementa esse código. Mantém maxOf, validações, assinatura, applicationId, processadores e demais configurações do Gradle.

Os mesmos três fontes do tema claro continuam no pacote, sem mudanças. Somente o instalador foi corrigido. Backup automático antes da escrita.
