# IPTV_APK_OUTPUT_VALIDATION_BEGIN
function Get-IptvDebugApks {
    param([string]$Folder, [int]$VersionCode = 9, [string]$VersionName = '1.07')
    $m = Get-Content -LiteralPath (Join-Path $Folder 'output-metadata.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($m.applicationId -cne 'br.com.flaviotorres.iptvpremium') { throw ('Identificador de aplicativo inesperado: ' + $m.applicationId) }
    if ($m.variantName -cne 'debug') { throw ('Variante de compilacao inesperada: ' + $m.variantName) }
    $entries = @($m.elements | Where-Object { $null -ne $_ })
    if ($entries.Count -lt 1 -or $entries.Count -gt 5) { throw ('Quantidade de APKs inesperada: ' + $entries.Count) }
    $seen = @{}
    $result = @()
    foreach ($entry in $entries) {
        if ($entry.versionCode -ne $VersionCode -or $entry.versionName -cne $VersionName) { throw ('Versao inesperada no APK: ' + $entry.outputFile) }
        $file = [string]$entry.outputFile
        if ($file -cnotmatch '^app(?:(?:-universal|-armeabi-v7a|-arm64-v8a|-x86|-x86_64))?-debug\.apk$') { throw ('Nome de APK inesperado: ' + $file) }
        $filters = @($entry.filters | Where-Object { $null -ne $_ })
        $abi = 'universal'
        if ($filters.Count -gt 0) {
            if ($filters.Count -ne 1 -or $filters[0].filterType -cne 'ABI' -or
                @('armeabi-v7a','arm64-v8a','x86','x86_64') -cnotcontains $filters[0].value) { throw ('Filtro de APK inesperado: ' + $file) }
            $abi = [string]$filters[0].value
            if ($file -cne ('app-' + $abi + '-debug.apk')) { throw 'Nome de APK diferente da arquitetura declarada.' }
        } elseif ($file -cne 'app-universal-debug.apk' -and $file -cne 'app-debug.apk') { throw 'APK por arquitetura sem filtro ABI.' }
        if ($seen.ContainsKey($abi)) { throw ('Arquitetura duplicada: ' + $abi) }
        $seen[$abi] = $true
        $path = Join-Path $Folder $file
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw ('APK ausente: ' + $file) }
        $result += [pscustomobject]@{ Abi = $abi; Path = $path; File = $file }
    }
    return [pscustomobject]@{ Entries = @($result); FullSet = ($seen.Count -eq 5 -and $seen.ContainsKey('universal')) }
}
function Select-IptvEmulatorApk {
    param($Outputs, [string[]]$Abis)
    $universal = @($Outputs.Entries | Where-Object { $_.Abi -ceq 'universal' })
    if ($universal.Count -eq 1) { return $universal[0].Path }
    foreach ($abi in $Abis) {
        $match = @($Outputs.Entries | Where-Object { $_.Abi -ceq $abi.Trim() })
        if ($match.Count -eq 1) { return $match[0].Path }
    }
    throw ('Nenhum APK gerado e compativel com o emulador. ABIs do emulador: ' + ($Abis -join ', '))
}
# IPTV_APK_OUTPUT_VALIDATION_END
