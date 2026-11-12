[CmdletBinding()]
param(
    [string]$Projeto = '',
    [Alias('Tv')][string]$Serial = '',
    [Alias('AdbPath')][string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [switch]$SomenteAplicar
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2
$encoding = [System.Text.UTF8Encoding]::new($false, $true)
function Normalizar([string]$Texto) { return $Texto.Replace("`r`n", "`n") }
function Hash-Texto([string]$Texto) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($encoding.GetBytes((Normalizar $Texto))))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
if ([string]::IsNullOrWhiteSpace($Projeto)) {
    $candidatos = @((Get-Location).Path, (Join-Path $env:USERPROFILE 'AndroidStudioProjects\IPTVPremium'))
    foreach ($candidato in $candidatos) {
        if (Test-Path -LiteralPath (Join-Path $candidato 'app\build.gradle.kts') -PathType Leaf) { $Projeto = $candidato; break }
    }
    if ([string]::IsNullOrWhiteSpace($Projeto)) { throw 'Informe -Projeto com a pasta Android IPTVPremium, nao o ZIP do site.' }
}
$Projeto = (Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath (Join-Path $Projeto 'app\build.gradle.kts') -PathType Leaf)) { throw 'A pasta informada nao contem app/build.gradle.kts.' }
$manifesto = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'manifesto.json') -Raw -Encoding UTF8 | ConvertFrom-Json
if ($manifesto.revision -ne 'r25' -or @($manifesto.files).Count -ne 3) { throw 'Manifesto do pacote invalido.' }
$permitidos = @('ExternalSubtitleSync.kt','SubtitleSyncAssistantBar.kt','SubtitleSyncBar.kt')
$plano = @()
foreach ($arquivo in $manifesto.files) {
    $nome = [System.IO.Path]::GetFileName($arquivo.path)
    $esperado = 'app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/' + $nome
    if ($nome -notin $permitidos -or $arquivo.path -cne $esperado -or @($plano | Where-Object { $_.Path -eq $arquivo.path }).Count -ne 0) { throw 'Caminho inesperado no pacote.' }
    $destino = Join-Path $Projeto $arquivo.path
    $origem = Join-Path (Join-Path $PSScriptRoot 'arquivos') $arquivo.path
    if (!(Test-Path -LiteralPath $destino -PathType Leaf)) { throw "Arquivo ausente: $destino. Nada foi alterado." }
    if (!(Test-Path -LiteralPath $origem -PathType Leaf)) { throw "Pacote incompleto: $nome. Nada foi alterado." }
    $novo = Normalizar ([System.IO.File]::ReadAllText($origem, $encoding))
    if ((Hash-Texto $novo) -ne $arquivo.after) { throw "Integridade do pacote invalida: $nome. Nada foi alterado." }
    $atual = [System.IO.File]::ReadAllText($destino, $encoding)
    $hash = Hash-Texto $atual
    if ($hash -ne $arquivo.before -and $hash -ne $arquivo.after) {
        throw "O arquivo $nome difere da base enviada em 12/11/2026. Nada foi alterado. Envie esse arquivo atual para adaptar a correcao sem sobrescrever suas mudancas."
    }
    $plano += [PSCustomObject]@{ Path=$arquivo.path; Destino=$destino; Novo=$novo; Original=[System.IO.File]::ReadAllBytes($destino); Alterar=($hash -ne $arquivo.after) }
}
$pendentes = @($plano | Where-Object { $_.Alterar })
if ($pendentes.Count -gt 0) {
    $backup = Join-Path $Projeto ('backups-codex\legendas-r25-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
    foreach ($item in $pendentes) {
        $copia = Join-Path $backup $item.Path
        [System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($copia)) | Out-Null
        [System.IO.File]::WriteAllBytes($copia, $item.Original)
    }
    # Backups de todos os arquivos concluidos antes da primeira alteracao.
    $escritos = @()
    try {
        foreach ($item in $pendentes) {
            $escritos += $item
            [System.IO.File]::WriteAllText($item.Destino, $item.Novo, $encoding)
        }
    } catch {
        $erroOriginal = $_
        foreach ($item in $escritos) { [System.IO.File]::WriteAllBytes($item.Destino, $item.Original) }
        throw $erroOriginal
    }
    Write-Host "Correcao r25 aplicada. Backup: $backup"
} else { Write-Host 'Correcao r25 ja aplicada. Nenhum arquivo foi regravado.' }
if ($SomenteAplicar) { Write-Host 'SomenteAplicar: compilacao e instalacao nao executadas.'; exit 0 }
# Se build/instalacao falharem, preserve a correcao e seu backup para diagnostico.
# O instalador nao desinstala o aplicativo nem apaga seus dados.
& (Join-Path $PSScriptRoot 'Instalar-TV.ps1') -Projeto $Projeto -Serial $Serial -Adb $Adb
