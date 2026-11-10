[CmdletBinding()]
param([string]$Projeto="$env:USERPROFILE\AndroidStudioProjects\IPTVPremium",[string]$Downloads="$env:USERPROFILE\Downloads",[string]$Recuperacao='')
$ErrorActionPreference='Stop'
. "$PSScriptRoot\Seguranca.ps1"
$Projeto=Projeto-Valido $Projeto
$plano=Get-Content -Raw -LiteralPath "$PSScriptRoot\plano-fisico.json"|ConvertFrom-Json
function Arquivos-Seguros([string]$Pasta) {
    $fila=[Collections.Generic.Stack[string]]::new();$fila.Push($Pasta)
    while($fila.Count -gt 0) {
        foreach($item in @(Get-ChildItem -LiteralPath $fila.Pop() -Force)) {
            if($item.Attributes -band [IO.FileAttributes]::ReparsePoint){throw "Link/juncao encontrado; interrompido: $($item.FullName)"}
            if($item.PSIsContainer){$fila.Push($item.FullName)}else{$item}
        }
    }
}
$pastas=@($plano.backups|ForEach-Object {
    if($_.path -notmatch '^\.[a-z0-9.-]+-backups$'){throw 'Pasta de backup invalida no plano.'}
    $p=Caminho-Seguro $Projeto $_.path
    if(Test-Path -LiteralPath $p -PathType Container){$p}
})
$pastas+=@($plano.extrasArquivar|ForEach-Object {
    if($_ -ne 'entregas-locais/perfis-importacao-1.07'){throw 'Pasta de teste inesperada no plano.'}
    $p=Caminho-Seguro $Projeto $_
    if(Test-Path -LiteralPath $p -PathType Container){$p}
})
$gerados=@($plano.gerados|ForEach-Object {
    if($_ -notin @('app/build','build','.gradle','.kotlin')){throw 'Pasta gerada invalida no plano.'}
    $p=Caminho-Seguro $Projeto $_
    if(Test-Path -LiteralPath $p -PathType Container){$p}
})
# Preflight every tree before changing anything. Never traverse links or infer targets by wildcard.
foreach($p in @($pastas)+@($gerados)){ $null=@(Arquivos-Seguros $p) }
Write-Host 'Encerrando os daemons Gradle antes da limpeza. Mantenha o Android Studio fechado.'
Push-Location -LiteralPath $Projeto
$anterior=$ErrorActionPreference
try {
    $ErrorActionPreference='Continue'
    & (Join-Path $Projeto 'gradlew.bat') --stop
    $codigo=$LASTEXITCODE
} finally { $ErrorActionPreference=$anterior;Pop-Location }
if($codigo -ne 0){throw 'Nao foi possivel parar o Gradle. Nenhuma pasta foi removida.'}
if([string]::IsNullOrWhiteSpace($Recuperacao)) {
    $saida=Destino-Externo $Projeto $Downloads 'IPTV-recuperacao-fisica-'
} else {
    $saida=(Resolve-Path -LiteralPath $Recuperacao).Path.TrimEnd([char[]]'\/')
    if((Split-Path $saida -Leaf) -notlike 'IPTV-recuperacao-fisica-*' -or $saida -eq $Projeto -or $saida.StartsWith($Projeto+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Pasta de recuperacao invalida.'}
    if(Test-Path -LiteralPath (Join-Path $saida 'backups-antigos.zip')){throw 'Esta recuperacao ja contem um ZIP. Use outra pasta para preservar o arquivo existente.'}
}
Write-Host "Recuperacao externa: $saida"
$apks=Join-Path $saida 'APKs-desempenho'
New-Item -ItemType Directory -Path $apks|Out-Null
$comparacao=[Collections.Generic.List[object]]::new()
foreach($abi in @('arm64-v8a','armeabi-v7a','x86','x86_64')) {
    $nome="app-$abi-desempenho.apk"
    $origem=Caminho-Seguro $Projeto "app/build/outputs/apk/desempenho/$nome"
    if(Test-Path -LiteralPath $origem -PathType Leaf) {
        $destino=Join-Path $apks $nome
        Copy-Item -LiteralPath $origem -Destination $destino -Force
        if((Hash-Arquivo $origem) -ne (Hash-Arquivo $destino)){throw 'Copia do APK nao confere; limpeza interrompida.'}
        $antes=$plano.apksInventario.PSObject.Properties[$nome]
        $comparacao.Add([pscustomobject]@{APK=$nome;BytesInventario=if($null -ne $antes){$antes.Value}else{$null};BytesAtual=(Get-Item -LiteralPath $destino).Length})
    }
}
$comparacao.ToArray()|Export-Csv -LiteralPath (Join-Path $saida 'tamanhos-apk.csv') -NoTypeInformation -Encoding UTF8
if($pastas.Count -gt 0) {
    $hashes=@{}
    $originais=@{}
    foreach($p in $pastas) {
        foreach($f in @(Arquivos-Seguros $p)) {
            $rel=$f.FullName.Substring($Projeto.Length+1).Replace('\','/')
            $hashes[$rel]=Hash-Arquivo $f.FullName
            $originais[$rel]=$f.FullName
        }
    }
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $arquivo=Join-Path $saida 'backups-antigos.zip'
    $zip=[IO.Compression.ZipFile]::Open($arquivo,[IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach($rel in $originais.Keys) {
            $null=[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$originais[$rel],$rel,[IO.Compression.CompressionLevel]::Optimal)
        }
    } finally {$zip.Dispose()}
    $zip=[IO.Compression.ZipFile]::OpenRead($arquivo)
    try {
        $vistos=0
        foreach($entry in $zip.Entries) {
            if($entry.FullName.EndsWith('/')){continue}
            $nome=$entry.FullName.Replace('\','/')
            if(!$hashes.ContainsKey($nome)){throw 'ZIP contem um arquivo inesperado.'}
            $stream=$entry.Open();$sha=[Security.Cryptography.SHA256]::Create()
            try{$hash=([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','').ToLowerInvariant()}
            finally{$stream.Dispose();$sha.Dispose()}
            if($hash -ne $hashes[$nome]){throw 'ZIP de recuperacao nao confere; backups originais preservados.'}
            $vistos++
        }
        if($vistos -ne $hashes.Count){throw 'ZIP de recuperacao incompleto; backups originais preservados.'}
    } finally {$zip.Dispose()}
    $atuais=@($pastas|ForEach-Object {Arquivos-Seguros $_})
    if($atuais.Count -ne $hashes.Count){throw 'Backups mudaram durante a limpeza; nao foram apagados.'}
    foreach($f in $atuais) {
        $rel=$f.FullName.Substring($Projeto.Length+1).Replace('\','/')
        if(!$hashes.ContainsKey($rel) -or (Hash-Arquivo $f.FullName) -ne $hashes[$rel]){throw 'Backup mudou; original preservado.'}
    }
    foreach($p in $pastas){Remove-Item -LiteralPath $p -Recurse -Force}
}
$liberado=[long]0
foreach($p in $gerados) {
    $arquivos=@(Arquivos-Seguros $p)
    $bytes=($arquivos|Measure-Object -Property Length -Sum).Sum
    Remove-Item -LiteralPath $p -Recurse -Force
    $liberado+=[long]$bytes
}
@(
    "Pastas antigas arquivadas e retiradas do projeto: $($pastas.Count)"
    "Bytes de arquivos gerados removidos do projeto: $liberado"
    'APKs desempenho por arquitetura existentes foram copiados e conferidos por SHA256.'
    'Backups antigos foram compactados e conferidos por SHA256 antes de apagar os originais.'
    'Para recuperar backups antigos, extraia backups-antigos.zip na raiz do projeto.'
    'Os diretorios de compilacao/cache voltam a ser criados em uma compilacao futura.'
    'tamanhos-apk.csv compara com o inventario anterior; nao e uma medicao controlada de velocidade.'
    'Esta recuperacao pode conter configuracoes privadas antigas. Nao publique nem envie este arquivo.'
)|Set-Content -LiteralPath (Join-Path $saida 'LEIA-ME.txt') -Encoding UTF8
Write-Host "Limpeza fisica concluida. Recuperacao e APKs em: $saida"
Write-Host 'A limpeza nao alterou dados ou cache da TV. Compilacoes futuras recriam as pastas geradas.'
