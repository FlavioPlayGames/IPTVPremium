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
$prefixo = 'app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/'
$permitidos = @('SubtitleSettingsHome.kt','SubtitleSetupWizard.kt','SubtitleAppearanceSettings.kt','ManualSubtitleSettings.kt','ManualSubtitleUploadScreen.kt','player/ManualSubtitleArchive.kt','player/ManualSubtitleTarget.kt','player/ManualSubtitleStore.kt','player/ManualSubtitleClient.kt','player/ExternalSubtitleRepositor.kt','player/PlaybackScreen.kt','player/SubtitleMenuPolicy.kt')
if ($manifesto.revision -ne 'r26' -or @($manifesto.files).Count -ne $permitidos.Count) { throw 'Manifesto do pacote invalido.' }
$plano = @()
foreach ($arquivo in $manifesto.files) {
    if ($arquivo.path -cnotin @($permitidos | ForEach-Object { $prefixo + $_ }) -or @($plano | Where-Object { $_.Path -ceq $arquivo.path }).Count -ne 0) { throw 'Caminho inesperado no pacote.' }
    $nome = [System.IO.Path]::GetFileName($arquivo.path)
    $destino = Join-Path $Projeto $arquivo.path
    $origem = Join-Path (Join-Path $PSScriptRoot 'arquivos') $arquivo.path
    if (!(Test-Path -LiteralPath $origem -PathType Leaf)) { throw "Pacote incompleto: $nome. Nada foi alterado." }
    $novo = Normalizar ([System.IO.File]::ReadAllText($origem, $encoding))
    if ((Hash-Texto $novo) -ne $arquivo.after) { throw "Integridade do pacote invalida: $nome. Nada foi alterado." }
    $existe = Test-Path -LiteralPath $destino -PathType Leaf
    $original = $null
    $hash = $null
    if ($existe) {
        $original = [System.IO.File]::ReadAllBytes($destino)
        $hash = Hash-Texto ([System.IO.File]::ReadAllText($destino, $encoding))
        if ($hash -ne $arquivo.after -and ($null -eq $arquivo.before -or $hash -ne $arquivo.before)) {
            throw "O arquivo $nome difere da base enviada em 12/11/2026. Nada foi alterado. Envie esse arquivo atual para adaptar sem sobrescrever suas mudancas."
        }
    } elseif ($null -ne $arquivo.before) { throw "Arquivo ausente: $nome. Nada foi alterado." }
    elseif (Test-Path -LiteralPath $destino) { throw "O destino $nome nao e um arquivo. Nada foi alterado." }
    $plano += [PSCustomObject]@{ Path=$arquivo.path; Destino=$destino; Novo=$novo; Original=$original; Existia=$existe; Alterar=($hash -ne $arquivo.after) }
}
$pendentes = @($plano | Where-Object { $_.Alterar })
if ($pendentes.Count -gt 0) {
    $backup = Join-Path $Projeto ('backups-codex\legendas-r26-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
    [System.IO.Directory]::CreateDirectory($backup) | Out-Null
    foreach ($item in $pendentes) {
        if ($item.Existia) {
            $copia = Join-Path $backup $item.Path
            [System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($copia)) | Out-Null
            [System.IO.File]::WriteAllBytes($copia, $item.Original)
        }
    }
    @($pendentes | ForEach-Object { [PSCustomObject]@{Path=$_.Path;NovoArquivo=(!$_.Existia)} }) |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'restauracao.json') -Encoding UTF8
    # Todo o preflight e todos os backups terminam antes da primeira alteracao.
    $escritos = @()
    try {
        foreach ($item in $pendentes) {
            $escritos += $item
            [System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($item.Destino)) | Out-Null
            [System.IO.File]::WriteAllText($item.Destino, $item.Novo, $encoding)
        }
    } catch {
        $erroOriginal = $_
        foreach ($item in $escritos) {
            if ($item.Existia) { [System.IO.File]::WriteAllBytes($item.Destino, $item.Original) }
            elseif (Test-Path -LiteralPath $item.Destino -PathType Leaf) { [System.IO.File]::Delete($item.Destino) }
        }
        throw $erroOriginal
    }
    Write-Host "Atualizacao r26 aplicada. Backup: $backup"
} else { Write-Host 'Atualizacao r26 ja aplicada. Nenhum arquivo foi regravado.' }
Write-Host 'Portal: publique separadamente os arquivos da pasta portal e configure MANUAL_SUBTITLES. O envio manual depende disso.'
if ($SomenteAplicar) { Write-Host 'SomenteAplicar: compilacao e instalacao nao executadas.'; exit 0 }
# Falhas de build/instalacao preservam a alteracao e o backup para diagnostico.
# O instalador nao desinstala o aplicativo e nao publica o portal.
& (Join-Path $PSScriptRoot 'Instalar-TV.ps1') -Projeto $Projeto -Serial $Serial -Adb $Adb
