[CmdletBinding()]
param([string]$Projeto=(Get-Location).Path,[switch]$SomenteInventario)
$ErrorActionPreference='Stop'
$Projeto=(Resolve-Path -LiteralPath $Projeto).Path
if (-not (Test-Path -LiteralPath (Join-Path $Projeto 'gradlew.bat'))) { throw 'Execute na raiz IPTVPremium, onde esta gradlew.bat.' }
$log=Join-Path $Projeto 'diagnostico-referencias-forcado-1.09.txt'
$writer=[IO.StreamWriter]::new($log,$false,[Text.UTF8Encoding]::new($false))
$writer.AutoFlush=$true
$gradleExit=$null
function Write-Diagnostic([string]$line) { Write-Host $line; $writer.WriteLine($line) }
try {
    Write-Diagnostic ('Projeto: '+$Projeto)
    Write-Diagnostic ('Data: '+(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Write-Diagnostic 'Diagnostico: inventario de fontes e recompilacao forcada. Nao instala APK nem limpa dados.'
    foreach ($name in @('settings.gradle.kts','settings.gradle','app/build.gradle.kts')) {
        Write-Diagnostic ($name+': '+(Test-Path -LiteralPath (Join-Path $Projeto $name)))
    }
    $sourceRoot=Join-Path $Projeto 'app/src'
    if (Test-Path -LiteralPath $sourceRoot) {
        $files=@(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.kt')
        Write-Diagnostic ('Arquivos Kotlin em app/src: '+$files.Count)
        foreach ($name in @('CatalogLayoutPreview.kt','PremiumThemes.kt','ThemeArtwork.kt','CatalogStyle.kt','CatalogThemeBackdrop.kt','CatalogWordmark.kt','CatalogTwoRowFrame.kt','CatalogGridNavigation.kt','CatalogGridGeometry.kt')) {
            $found=@($files | Where-Object { $_.Name -eq $name })
            if ($found.Count -eq 0) { Write-Diagnostic ('AUSENTE: '+$name); continue }
            foreach ($file in $found) {
                Write-Diagnostic ('Arquivo: '+$file.FullName)
                Write-Diagnostic ('SHA256: '+(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash)
                $package=Select-String -LiteralPath $file.FullName -Pattern '^\s*package\s+\S+' | Select-Object -First 1
                Write-Diagnostic ('Pacote: '+$(if ($package) { $package.Line.Trim() } else { '(nao encontrado)' }))
            }
        }
    } else { Write-Diagnostic 'AUSENTE: app/src' }
    if (-not $SomenteInventario) {
        Push-Location $Projeto
        $savedErrorAction=$ErrorActionPreference
        try {
            Write-Diagnostic 'GRADLE: :app:compileDebugKotlin --rerun-tasks --no-configuration-cache --no-daemon --max-workers=2 --console=plain'
            $ErrorActionPreference='Continue'
            & .\gradlew.bat :app:compileDebugKotlin --rerun-tasks --no-configuration-cache --no-daemon --max-workers=2 --console=plain 2>&1 | ForEach-Object {
                $line=$_.ToString()
                Write-Diagnostic $line
            }
            $gradleExit=$LASTEXITCODE
        } finally { $ErrorActionPreference=$savedErrorAction; Pop-Location }
        Write-Diagnostic ('Codigo de saida do Gradle: '+$gradleExit)
    }
} finally { $writer.Dispose() }
Write-Host "Envie este arquivo: $log"
if ($null -ne $gradleExit -and $gradleExit -ne 0) { Write-Host 'A compilacao falhou; o diagnostico completo foi salvo. Nenhum APK foi instalado.' }
