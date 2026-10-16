$ErrorActionPreference = "Stop"
$payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("eyJwYXRjaGVzIjogW3siZmlsZSI6ICJhcHAvYnVpbGQuZ3JhZGxlLmt0cyIsICJvbGQiOiAiICAgIGJ1aWxkVHlwZXMge1xuIiwgIm5ldyI6ICIgICAgLy8gQVBLcyBjb21wbGV0b3MgcG9yIHByb2Nlc3NhZG9yIGUgdW0gQVBLIHVuaXZlcnNhbCBkZSByZXNlcnZhLlxuICAgIC8vIE1lc21vIGFwcGxpY2F0aW9uSWQsIHZlcnNpb25Db2RlIGUgYXNzaW5hdHVyYSBlbSB0b2RhcyBhcyBzYWlkYXMuXG4gICAgc3BsaXRzIHtcbiAgICAgICAgYWJpIHtcbiAgICAgICAgICAgIGlzRW5hYmxlID0gdHJ1ZVxuICAgICAgICAgICAgcmVzZXQoKVxuICAgICAgICAgICAgaW5jbHVkZShcImFybWVhYmktdjdhXCIsIFwiYXJtNjQtdjhhXCIsIFwieDg2XCIsIFwieDg2XzY0XCIpXG4gICAgICAgICAgICBpc1VuaXZlcnNhbEFwayA9IHRydWVcbiAgICAgICAgfVxuICAgIH1cblxuICAgIGJ1aWxkVHlwZXMge1xuIiwgImNvdW50IjogMX1dLCAiaGVscGVycyI6IFt7ImZpbGUiOiAiR2VyYXItQVBLcy5wczEiLCAidGV4dCI6ICIkRXJyb3JBY3Rpb25QcmVmZXJlbmNlID0gXCJTdG9wXCJcbiRiYXNlID0gJFBTU2NyaXB0Um9vdFxuaWYgKC1ub3QgKFRlc3QtUGF0aCAtTGl0ZXJhbFBhdGggKEpvaW4tUGF0aCAkYmFzZSBcImFwcFxcYnVpbGQuZ3JhZGxlLmt0c1wiKSkpIHsgdGhyb3cgXCJDb2xvcXVlIGVzdGUgYXV4aWxpYXIgbmEgcmFpeiBkbyBwcm9qZXRvIEFuZHJvaWQuXCIgfVxuUHVzaC1Mb2NhdGlvbiAkYmFzZVxudHJ5IHtcbiAgICAmIChKb2luLVBhdGggJGJhc2UgXCJncmFkbGV3LmJhdFwiKSA6YXBwOmFzc2VtYmxlRGVidWcgLS1jb25zb2xlPXBsYWluXG4gICAgaWYgKCRMQVNURVhJVENPREUgLW5lIDApIHsgdGhyb3cgXCJBIGNvbXBpbGFjYW8gZmFsaG91LiBOZW5odW0gQVBLIGRlc3RhIHRlbnRhdGl2YSBkZXZlIHNlciBwdWJsaWNhZG8uXCIgfVxuICAgICRkaXJlY3RvcnkgPSBKb2luLVBhdGggJGJhc2UgXCJhcHBcXGJ1aWxkXFxvdXRwdXRzXFxhcGtcXGRlYnVnXCJcbiAgICAkbWV0YWRhdGEgPSBHZXQtQ29udGVudCAtTGl0ZXJhbFBhdGggKEpvaW4tUGF0aCAkZGlyZWN0b3J5IFwib3V0cHV0LW1ldGFkYXRhLmpzb25cIikgLVJhdyB8IENvbnZlcnRGcm9tLUpzb25cbiAgICBpZiAoJG1ldGFkYXRhLmFwcGxpY2F0aW9uSWQgLW5lIFwiYnIuY29tLmZsYXZpb3RvcnJlcy5pcHR2cHJlbWl1bVwiKSB7IHRocm93IFwiUHJvamV0byBBbmRyb2lkIGluZXNwZXJhZG8uXCIgfVxuICAgICRleHBlY3RlZCA9IEAoXCJ1bml2ZXJzYWxcIiwgXCJhcm1lYWJpLXY3YVwiLCBcImFybTY0LXY4YVwiLCBcIng4NlwiLCBcIng4Nl82NFwiKVxuICAgICRzZWVuID0gQHt9XG4gICAgJHJvd3MgPSBAKClcbiAgICBmb3JlYWNoICgkZWxlbWVudCBpbiAkbWV0YWRhdGEuZWxlbWVudHMpIHtcbiAgICAgICAgJGZpbHRlcnMgPSBAKCRlbGVtZW50LmZpbHRlcnMgfCBXaGVyZS1PYmplY3QgeyAkXy5maWx0ZXJUeXBlIC1lcSBcIkFCSVwiIH0pXG4gICAgICAgICRhYmkgPSBpZiAoJGZpbHRlcnMuQ291bnQgLWVxIDApIHsgXCJ1bml2ZXJzYWxcIiB9IGVsc2UgeyBbc3RyaW5nXSRmaWx0ZXJzWzBdLnZhbHVlIH1cbiAgICAgICAgaWYgKCRleHBlY3RlZCAtbm90Y29udGFpbnMgJGFiaSAtb3IgJHNlZW4uQ29udGFpbnNLZXkoJGFiaSkpIHsgdGhyb3cgXCJTYWlkYSBkZSBhcnF1aXRldHVyYSBpbmVzcGVyYWRhIG91IGR1cGxpY2FkYS5cIiB9XG4gICAgICAgICRzZWVuWyRhYmldID0gJHRydWVcbiAgICAgICAgJG5hbWUgPSBbc3RyaW5nXSRlbGVtZW50Lm91dHB1dEZpbGVcbiAgICAgICAgaWYgKFtJTy5QYXRoXTo6R2V0RmlsZU5hbWUoJG5hbWUpIC1uZSAkbmFtZSAtb3IgLW5vdCAkbmFtZS5FbmRzV2l0aChcIi5hcGtcIikpIHsgdGhyb3cgXCJOb21lIGRlIEFQSyBpbnZhbGlkby5cIiB9XG4gICAgICAgICRmaWxlID0gR2V0LUl0ZW0gLUxpdGVyYWxQYXRoIChKb2luLVBhdGggJGRpcmVjdG9yeSAkbmFtZSlcbiAgICAgICAgJHJvd3MgKz0gW3BzY3VzdG9tb2JqZWN0XUB7XG4gICAgICAgICAgICBhcmNoaXRlY3R1cmUgPSAkYWJpXG4gICAgICAgICAgICBmaWxlID0gJG5hbWVcbiAgICAgICAgICAgIHZlcnNpb25Db2RlID0gW2xvbmddJGVsZW1lbnQudmVyc2lvbkNvZGVcbiAgICAgICAgICAgIHZlcnNpb25OYW1lID0gW3N0cmluZ10kZWxlbWVudC52ZXJzaW9uTmFtZVxuICAgICAgICAgICAgc2l6ZUJ5dGVzID0gJGZpbGUuTGVuZ3RoXG4gICAgICAgICAgICBzaGEyNTYgPSAoR2V0LUZpbGVIYXNoIC1MaXRlcmFsUGF0aCAkZmlsZS5GdWxsTmFtZSAtQWxnb3JpdGhtIFNIQTI1NikuSGFzaC5Ub0xvd2VySW52YXJpYW50KClcbiAgICAgICAgfVxuICAgIH1cbiAgICBpZiAoJHNlZW4uQ291bnQgLW5lIDUgLW9yIEAoJHJvd3MgfCBTZWxlY3QtT2JqZWN0IC1FeHBhbmRQcm9wZXJ0eSB2ZXJzaW9uQ29kZSAtVW5pcXVlKS5Db3VudCAtbmUgMSAtb3IgQCgkcm93cyB8IFNlbGVjdC1PYmplY3QgLUV4cGFuZFByb3BlcnR5IHZlcnNpb25OYW1lIC1VbmlxdWUpLkNvdW50IC1uZSAxKSB7IHRocm93IFwiRmFsdGFtIEFQS3Mgb3UgYXMgdmVyc29lcyBuYW8gY29pbmNpZGVtLlwiIH1cbiAgICAkcmVwb3J0ID0gSm9pbi1QYXRoICRkaXJlY3RvcnkgXCJhcGtzLWRpc3Bvbml2ZWlzLmpzb25cIlxuICAgICRqc29uID0gQHsgYXBwbGljYXRpb25JZCA9ICRtZXRhZGF0YS5hcHBsaWNhdGlvbklkOyBhcGtzID0gJHJvd3MgfSB8IENvbnZlcnRUby1Kc29uIC1EZXB0aCA2XG4gICAgW0lPLkZpbGVdOjpXcml0ZUFsbFRleHQoJHJlcG9ydCwgJGpzb24sIChOZXctT2JqZWN0IFRleHQuVVRGOEVuY29kaW5nKCRmYWxzZSkpKVxuICAgICRyb3dzIHwgU2VsZWN0LU9iamVjdCBhcmNoaXRlY3R1cmUsIGZpbGUsIHZlcnNpb25Db2RlLCB2ZXJzaW9uTmFtZSwgQHsgTmFtZT1cIk1CXCI7IEV4cHJlc3Npb249eyBbTWF0aF06OlJvdW5kKCRfLnNpemVCeXRlcyAvIDEwMDAwMDAsIDEpIH0gfSB8IEZvcm1hdC1UYWJsZSAtQXV0b1NpemVcbiAgICBXcml0ZS1Ib3N0IFwiQVBLcyBlIHJlbGF0b3JpbzogJGRpcmVjdG9yeVwiIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW5cbiAgICBXcml0ZS1Ib3N0IFwiQ2FkYSBBUEsgZSBjb21wbGV0by4gSW5zdGFsZSBzb21lbnRlIHVtLCBhZGVxdWFkbyBhbyBhcGFyZWxobywgc2VtIGRlc2luc3RhbGFyIG8gYXBsaWNhdGl2byBhdHVhbC5cIlxuICAgIFdyaXRlLUhvc3QgXCJPIHBhaW5lbCBhdHVhbCBhY2VpdGEgdW0gdW5pY28gbGluazogcHVibGlxdWUgbyBhcHAtdW5pdmVyc2FsLWRlYnVnLmFwayBwYXJhIGF0ZW5kZXIgdG9kb3Mgb3MgYXBhcmVsaG9zLlwiXG4gICAgV3JpdGUtSG9zdCBcIkVzdGUgYXV4aWxpYXIgbmFvIGFsdGVyYSB2ZXJzYW8sIGFzc2luYXR1cmEsIEZpcmViYXNlIG91IGNvbmZpZ3VyYWNvZXMgcHJpdmFkYXMgZSBuYW8gcHVibGljYSBhcnF1aXZvcy5cIlxufSBmaW5hbGx5IHsgUG9wLUxvY2F0aW9uIH1cbiJ9XX0=")) | ConvertFrom-Json
$base = (Get-Location).Path
if (-not (Test-Path -LiteralPath (Join-Path $base "app\src\main"))) {
    if ((Test-Path -LiteralPath (Join-Path $base "src\main")) -and (Test-Path -LiteralPath (Join-Path $base "..\settings.gradle.kts"))) {
        $base = Split-Path $base -Parent
    } else { throw "Abra o terminal na raiz do projeto Android." }
}
$marker = "IPTV_ABI_APKS_AND_UNIVERSAL_V1"
$staged = @{}
$formats = @{}
$utf8 = New-Object Text.UTF8Encoding($false, $true)
foreach ($change in $payload.patches) {
    $path = Join-Path $base ($change.file.Replace('/', '\'))
    if (-not $staged.ContainsKey($path)) {
        if (-not (Test-Path -LiteralPath $path)) { throw "Arquivo ausente: $($change.file)" }
        $bytes = [IO.File]::ReadAllBytes($path)
        $bom = $bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191
        $text = $utf8.GetString($bytes).TrimStart([char]0xFEFF)
        $formats[$path] = @{ Bom = $bom; CRLF = $text.Contains("`r`n"); Applied = $text.Contains($marker) }
        $staged[$path] = $text.Replace("`r`n", "`n")
    }
    if ($formats[$path].Applied) { continue }
    $old = $change.old.Replace("`r`n", "`n")
    $new = $change.new.Replace("`r`n", "`n")
    $count = [regex]::Matches($staged[$path], [regex]::Escape($old)).Count
    $expectedCount = if ($change.count) { [int]$change.count } else { 1 }
    if ($count -ne $expectedCount) { throw "Versao diferente em $($change.file). Nada foi alterado. Envie os arquivos Kotlin atuais para adaptar o patch." }
    $staged[$path] = $staged[$path].Replace($old, $new)
}
foreach ($helper in $payload.helpers) {
    $path = Join-Path $base ($helper.file.Replace('/', '\'))
    $text = $helper.text.Replace("`r`n", "`n")
    if (Test-Path -LiteralPath $path) {
        $existing = $utf8.GetString([IO.File]::ReadAllBytes($path)).TrimStart([char]0xFEFF).Replace("`r`n", "`n")
        if ($existing -ne $text) { throw "Arquivo auxiliar ja existe e foi modificado: $($helper.file). Nada foi alterado." }
        continue
    }
    $staged[$path] = $text
    $formats[$path] = @{ Bom = $false; CRLF = $true; Applied = $false; Helper = $true }
}
# Validate all files first; preserve original bytes in backups before any source write.
$stamp = Get-Date -Format "yyyyMMdd-HHmmssfff"
foreach ($path in $staged.Keys) {
    if ($formats[$path].Applied) { continue }
    if (Test-Path -LiteralPath $path) { Copy-Item -LiteralPath $path -Destination ($path + ".backup-" + $stamp) }
}
foreach ($path in $staged.Keys) {
    if ($formats[$path].Applied) { continue }
    $text = $staged[$path]
    if (-not $formats[$path].Helper) {
        $fileComment = if ([IO.Path]::GetExtension($path) -eq ".xml") { "<!-- " + $marker + " -->" } else { "// " + $marker }
        $text += "`n" + $fileComment + "`n"
    }
    if ($formats[$path].CRLF) { $text = $text.Replace("`n", "`r`n") }
    $encoding = New-Object Text.UTF8Encoding([bool]$formats[$path].Bom)
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))
    [IO.File]::WriteAllText($path + ".iptv-tmp", $text, $encoding)
    Move-Item -LiteralPath ($path + ".iptv-tmp") -Destination $path -Force
}
Write-Host "Concluido: APKs por processador e universal configurados. Execute Gerar-APKs.ps1 para compilar todos." -ForegroundColor Green
Write-Host "Backups dos arquivos alterados: .backup-$stamp"

