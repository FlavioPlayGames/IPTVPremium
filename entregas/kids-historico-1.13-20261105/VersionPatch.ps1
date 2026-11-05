function VersionTextHash([string]$Text){
 $value=$Text.Replace("`r`n","`n").TrimStart([char]0xFEFF)
 $hash=[Security.Cryptography.SHA256]::Create()
 try{return ([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($value)))).Replace('-','').ToLowerInvariant()}finally{$hash.Dispose()}
}
function New-PlayerVersionEntry([string]$Root){
 $relative='app/build.gradle.kts';$file=Join-Path $Root $relative
 if(!(Test-Path -LiteralPath $file)){throw 'app/build.gradle.kts ausente. Nada foi alterado.'}
 $before=[IO.File]::ReadAllText($file)
 $namePattern='(?m)^([ \t]*versionName[ \t]*=[ \t]*)"([^"]+)"'
 $codePattern='(?m)^([ \t]*versionCode[ \t]*=[ \t]*)maxOf\((\d+),[ \t]*localProperties\.getProperty\("APP_VERSION_CODE",[ \t]*"(\d+)"\)\.toInt\(\)\)'
 $names=[regex]::Matches($before,$namePattern);$codes=[regex]::Matches($before,$codePattern)
 if($names.Count -ne 1 -or $codes.Count -ne 1){throw 'Formato de versao desconhecido em app/build.gradle.kts. Nada foi alterado.'}
 $current=$names[0].Groups[2].Value
 if($current -notin @('1.12','1.13')){throw ('Esperada versao 1.12 ou 1.13; encontrada '+$current+'. Nada foi alterado.')}
 $floor=[long]$codes[0].Groups[2].Value
 $fallback=[long]$codes[0].Groups[3].Value
 $configured=$fallback;$properties=Join-Path $Root 'local.properties'
 if(Test-Path -LiteralPath $properties){
  $versionMatches=[regex]::Matches([IO.File]::ReadAllText($properties),'(?m)^[ \t]*APP_VERSION_CODE[ \t]*=[ \t]*(\d+)[ \t]*\r?$')
  if($versionMatches.Count -gt 1){throw 'APP_VERSION_CODE duplicado. Nada foi alterado.'}
  if($versionMatches.Count -eq 1){$configured=[long]$versionMatches[0].Groups[1].Value}
 }
 if($current -eq '1.13'){$next=[Math]::Max(15,$floor)}
 else{$next=[Math]::Max(15,[Math]::Max($floor,$configured)+1)}
 if($next -gt 2100000000){throw 'Nao ha versionCode superior valido. Nada foi alterado.'}
 $after=[regex]::Replace($before,$namePattern,{param($m) $m.Groups[1].Value+'"1.13"'})
 $after=[regex]::Replace($after,$codePattern,{param($m) $m.Groups[1].Value+'maxOf('+$next+', localProperties.getProperty("APP_VERSION_CODE", "'+$next+'").toInt())'})
 return [pscustomobject]@{path=$relative;sha=(VersionTextHash $after);before=@((VersionTextHash $before));allowMissing=$false;binary=$false;remove=$false;textContent=$after}
}
