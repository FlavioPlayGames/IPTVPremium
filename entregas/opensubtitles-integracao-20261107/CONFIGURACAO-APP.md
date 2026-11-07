# Chave do aplicativo e conta do usuário

O usuário final cria uma conta no OpenSubtitles.com e entra com usuário/senha no app. Ele não precisa criar uma chave de API. A API também exige uma chave que identifica o aplicativo. O cadastro e a chave do aplicativo ainda não foram fornecidos; não é possível habilitar a API oficial somente com login de usuário.

O responsável pelo build registra o aplicativo na área de API / API Consumers de https://www.opensubtitles.com, obtém a chave e o User-Agent correspondente ao registro. Não envie chave, senha ou token na conversa. Não usamos chaves de outros aplicativos.

Para o build Desempenho usado neste projeto, crie localmente:

`app/src/desempenho/res/values/opensubtitles-private.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="opensubtitles_api_key" translatable="false">COLE_A_CHAVE_DO_APLICATIVO</string>
    <string name="opensubtitles_user_agent" translatable="false">IPTVPremium v1.13</string>
</resources>
```

Substitua o marcador pela chave real e use o User-Agent aceito para o seu registro. Escape caracteres XML se necessário. O nome do arquivo é distinto, e os recursos do build type sobrepõem os valores vazios do fonte principal. Para outro build type, use a pasta correspondente. Não crie outro arquivo com os mesmos nomes em `src/main/res/values`, pois valores duplicados no mesmo source set falham na compilação.

Mantenha esse arquivo fora do controle de versão e dos ZIPs de fontes; adicione ao `.gitignore`:

```gitignore
/app/src/*/res/values/opensubtitles-private.xml
```

O pacote não cria nem modifica esse arquivo privado. O valor padrão em `app/src/main/res/values/opensubtitles.xml` é vazio; sem sobreposição, não há tentativas de login no OpenSubtitles e o SubDL continua disponível.

Depois compile/instale o APK e entre com a conta pessoal em Configurações > Legendas > OpenSubtitles. Cada usuário usa seu próprio acesso e fica sujeito às regras/cotas da conta. A senha não fica salva; somente a sessão é criptografada. A implementação ainda precisa ser validada com a chave registrada, conta real e TV. Os testes incluídos usam respostas simuladas.
