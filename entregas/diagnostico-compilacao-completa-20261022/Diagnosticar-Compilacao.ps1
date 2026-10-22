param(
 [Parameter(Mandatory=$true)][string]$Projeto,
 [switch]$SomenteInventario
)
$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path -LiteralPath $Projeto).Path
if(!(Test-Path -LiteralPath (Join-Path $projectRoot 'gradlew.bat'))) { throw 'Projeto Android nao encontrado.' }
$desktop=[Environment]::GetFolderPath('Desktop')
if([string]::IsNullOrWhiteSpace($desktop) -or !(Test-Path -LiteralPath $desktop)) { $desktop=$projectRoot }
$report=Join-Path $desktop ('diagnostico-compilacao-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,6)+'.txt')
'DIAGNOSTICO DE COMPILACAO - FONTES NAO ALTERADOS' | Set-Content -LiteralPath $report -Encoding UTF8
('Data: '+(Get-Date -Format o)) | Add-Content -LiteralPath $report -Encoding UTF8
$sourceRoot=Join-Path $projectRoot 'app/src'
$files=@(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File | Where-Object { $_.Extension -eq '.kt' })
$declarations=@{}
foreach($file in $files) {
 $text=[IO.File]::ReadAllText($file.FullName)
 $packageMatch=[regex]::Match($text,'(?m)^\s*package\s+([A-Za-z_][\w.]*)')
 if(!$packageMatch.Success) { continue }
 $relative=$file.FullName.Substring($projectRoot.TrimEnd('\','/').Length).TrimStart('\','/')
 # Conservative candidates only: no private declarations, no indented nested classes.
 $pattern='(?m)^(?:(?:public|internal|data|sealed|enum|annotation|value|open|abstract|expect|actual)\s+)*(?:class|interface|object)\s+([A-Za-z_]\w*)'
 foreach($match in [regex]::Matches($text,$pattern)) {
  $key=$packageMatch.Groups[1].Value+'.'+$match.Groups[1].Value
  if(!$declarations.ContainsKey($key)) { $declarations[$key]=New-Object 'System.Collections.Generic.List[string]' }
  $declarations[$key].Add($relative)
 }
}
"`nCANDIDATOS A DECLARACOES DUPLICADAS (variantes diferentes podem ser legitimas):" | Add-Content -LiteralPath $report -Encoding UTF8
$duplicateCount=0
foreach($key in ($declarations.Keys | Sort-Object)) {
 $paths=@($declarations[$key] | Select-Object -Unique)
 if($paths.Count -gt 1) {
  $duplicateCount++
  $key | Add-Content -LiteralPath $report -Encoding UTF8
  $paths | ForEach-Object { ('  '+$_) | Add-Content -LiteralPath $report -Encoding UTF8 }
 }
}
('Candidatos: '+$duplicateCount+'; arquivos Kotlin: '+$files.Count) | Add-Content -LiteralPath $report -Encoding UTF8
if(!$SomenteInventario) {
 "`nSAIDA COMPLETA DO GRADLE (debug e compilacao Kotlin release):" | Add-Content -LiteralPath $report -Encoding UTF8
 Write-Host 'Compilando todo o modulo. Aguarde terminar; nenhuma fonte sera alterada.'
 Push-Location $projectRoot
 $oldPreference=$ErrorActionPreference
 try {
  # Windows PowerShell emits stderr as ErrorRecord; keep compiler errors in the report.
  $ErrorActionPreference='Continue'
  & .\gradlew.bat clean :app:assembleDebug :app:compileReleaseKotlin --continue --no-daemon --no-configuration-cache --no-build-cache --max-workers=2 --console=plain 2>&1 | ForEach-Object {
   $line=$_.ToString()
   Add-Content -LiteralPath $report -Value $line -Encoding UTF8
   Write-Host $line
  }
  $buildExit=$LASTEXITCODE
 } finally { $ErrorActionPreference=$oldPreference; Pop-Location }
 ("`nCodigo de saida do Gradle: "+$buildExit) | Add-Content -LiteralPath $report -Encoding UTF8
} else { 'Inventario apenas: Gradle nao executado.' | Add-Content -LiteralPath $report -Encoding UTF8 }
Write-Host "Envie este arquivo completo: $report"
