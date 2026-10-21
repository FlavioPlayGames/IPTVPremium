[CmdletBinding()]
param([string]$Projeto = (Get-Location).Path)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $Projeto).Path
$source = Join-Path $root 'app/src/main/java/br/com/flaviotorres/iptvpremium/ui/components/CatalogLayoutPreview.kt'
$res = Join-Path $root 'app/src/main/res'
if (!(Test-Path -LiteralPath $source -PathType Leaf)) { throw 'Abra o PowerShell na raiz do projeto IPTVPremium.' }
$utf8 = New-Object System.Text.UTF8Encoding($false, $true)
$raw = [IO.File]::ReadAllBytes($source)
$text = $utf8.GetString($raw)
$pattern = '\bR\.drawable\.(preview_poster_\d+)\b'
$names = @([regex]::Matches($text, $pattern) | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
$present = @{}
Get-ChildItem -LiteralPath $res -Directory | Where-Object { $_.Name -match '^drawable(?:-|$)' } | ForEach-Object {
    Get-ChildItem -LiteralPath $_.FullName -File | Where-Object { $_.Extension -match '^\.(jpg|jpeg|png|webp|gif|xml)$' } | ForEach-Object {
        $name = $_.BaseName -replace '\.9$', ''
        $present[$name] = $true
    }
}
$missing = @($names | Where-Object { !$present.ContainsKey($_) })
if ($missing.Count -eq 0) { Write-Host 'Nenhuma referencia de capa ausente. Nada foi alterado.'; return }
$available = @($names | Where-Object { $present.ContainsKey($_) })
if ($available.Count -eq 0) { throw 'Nenhuma capa da previa foi mantida. Restaure ao menos uma preview_poster antes de executar. Nenhum arquivo foi alterado.' }
$fallback = $available[0]
$updated = [regex]::Replace($text, $pattern, [System.Text.RegularExpressions.MatchEvaluator]{ param($match)
    if ($present.ContainsKey($match.Groups[1].Value)) { return $match.Value }
    return "R.drawable.$fallback"
})
$backupDir = Join-Path $root ('.navigation-backups/previa-' + [guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($backupDir) | Out-Null
$backup = Join-Path $backupDir 'CatalogLayoutPreview.kt'
[IO.File]::WriteAllBytes($backup, $raw)
try { [IO.File]::WriteAllBytes($source, $utf8.GetBytes($updated)) }
catch { [IO.File]::WriteAllBytes($source, $raw); throw }
Write-Host ('Referencias substituidas: ' + ($missing -join ', '))
Write-Host "Capa usada na previa: $fallback"
Write-Host "Backup: $backup"
