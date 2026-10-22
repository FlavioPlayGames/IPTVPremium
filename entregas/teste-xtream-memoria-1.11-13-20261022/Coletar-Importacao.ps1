param([string]$Emulador='emulator-5554',[string]$Adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")
$ErrorActionPreference='Stop'
if (!(Test-Path -LiteralPath $Adb)) { throw 'Informe o caminho de adb.exe com -Adb.' }
$outputFile=Join-Path ([Environment]::GetFolderPath('Desktop')) ('importacao-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt')
Write-Host "Abra a lista e navegue. Ctrl+C encerra. Envie: $outputFile"
# Keep only the closed numeric diagnostic schema, never arbitrary application logs.
& $Adb -s $Emulador logcat -v raw -T 1 'ImportTiming:I' '*:S' | ForEach-Object {
 if ($_ -match '^v=1 run=[0-9]+ kind=(XTREAM|XTREAM_INDEX|XTREAM_MOVIES|XTREAM_SERIES|XTREAM_CHANNELS|M3U) outcome=(OK|ERROR|CANCELLED) totalMs=[0-9]+( (HEADERS|READ|PARSE|MAP_AND_WRITE|MERGE|READY)(Ms|Calls)=[0-9]+)+$') {
  $line=(Get-Date -Format o)+' '+$_
  Add-Content -LiteralPath $outputFile -Value $line -Encoding UTF8
  Write-Host $line
 }
}
