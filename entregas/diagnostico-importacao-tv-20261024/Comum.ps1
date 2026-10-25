#requires -Version 5.1
$ErrorActionPreference='Stop'
function Quote-Arg([string]$Value) {
 if($Value.Length -gt 0 -and $Value -notmatch '[\s"]'){return $Value}
 '"'+[regex]::Replace([regex]::Replace($Value,'(\\*)"','$1$1\"'),'(\\+)$','$1$1')+'"'
}
function Run-Adb([string[]]$Argumentos,[int]$TimeoutMs=10000){
 $clock=[Diagnostics.Stopwatch]::StartNew()
 $info=New-Object Diagnostics.ProcessStartInfo
 $info.FileName=$script:Adb
 $info.Arguments=(@('-s',$script:Device)+$Argumentos | ForEach-Object {Quote-Arg $_}) -join ' '
 $info.UseShellExecute=$false;$info.CreateNoWindow=$true
 $info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
 $proc=New-Object Diagnostics.Process;$proc.StartInfo=$info
 [void]$proc.Start();$out=$proc.StandardOutput.ReadToEndAsync();$err=$proc.StandardError.ReadToEndAsync()
 $timedOut=!$proc.WaitForExit($TimeoutMs)
 if($timedOut){try{$proc.Kill()}catch{};[void]$proc.WaitForExit(1000)}
 $text='';$errorText=''
 if($out.Wait(1000)){$text=$out.Result}
 if($err.Wait(1000)){$errorText=$err.Result}
 $exit=-1;if($proc.HasExited){$exit=$proc.ExitCode};$proc.Dispose()
 [pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');ms=$clock.ElapsedMilliseconds;exit=$exit;timeout=$timedOut;stdout=$text;stderr=$errorText}
}
function Save-Adb([string]$Name,[string[]]$ArgsList,[int]$TimeoutMs=10000){
 $r=Run-Adb $ArgsList $TimeoutMs
 $r | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $script:RunFolder ($Name+'.json')) -Encoding UTF8
 [IO.File]::WriteAllText((Join-Path $script:RunFolder ($Name+'.txt')),($r.stdout+"`nSTDERR:`n"+$r.stderr),[Text.Encoding]::UTF8)
 return $r
}
function Start-AdbLog([string]$Name,[string[]]$ArgsList){
 $argsText=(@('-s',$script:Device)+$ArgsList | ForEach-Object {Quote-Arg $_}) -join ' '
 Start-Process -FilePath $script:Adb -ArgumentList $argsText -PassThru -NoNewWindow -RedirectStandardOutput (Join-Path $script:RunFolder ($Name+'.txt')) -RedirectStandardError (Join-Path $script:RunFolder ($Name+'-stderr.txt'))
}
function Read-Json([string]$Path){
 ConvertFrom-Json -InputObject ([IO.File]::ReadAllText($Path).TrimStart([char]0xFEFF))
}
function Number-Field([string]$Text,[string]$Name){
 $m=[regex]::Match($Text,'(?:^|\s)'+[regex]::Escape($Name)+'=(\d+(?:\.\d+)?)')
 if($m.Success){return [double]::Parse($m.Groups[1].Value,[Globalization.CultureInfo]::InvariantCulture)}
 return $null
}
