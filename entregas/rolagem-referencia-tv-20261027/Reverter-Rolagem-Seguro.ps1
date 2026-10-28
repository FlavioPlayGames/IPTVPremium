param([Parameter(Mandatory=$true)][string]$Projeto)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath $Projeto).Path
if(!(Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))){throw 'Projeto Android nao encontrado.'}
$installer=Join-Path $PSScriptRoot 'Instalar-TV.ps1'
if(!(Test-Path -LiteralPath $installer)){throw 'Mantenha este script junto de Instalar-TV.ps1 na pasta do pacote.'}
$relative='app/src/main/java/br/com/flaviotorres/iptvpremium/ui/components/CatalogScrollMotion.kt'
$target=Join-Path $root $relative
if(!(Test-Path -LiteralPath $target)){throw 'Arquivo de rolagem nao encontrado. Nada foi alterado.'}
$bytes=[Convert]::FromBase64String('cGFja2FnZSBici5jb20uZmxhdmlvdG9ycmVzLmlwdHZwcmVtaXVtLnVpLmNvbXBvbmVudHMKCi8qKiBSZXVzZWQgZm9yIGFuIGVudGlyZSBnZXN0dXJlOiB0YXJnZXRzIG1heSBjaGFuZ2Ugd2l0aG91dCByZXNldHRpbmcgbW9tZW50dW0uICovCmludGVybmFsIGNsYXNzIENhdGFsb2dTY3JvbGxNb3Rpb24gewogICAgdmFyIHZlbG9jaXR5OiBGbG9hdCA9IDBmCiAgICAgICAgcHJpdmF0ZSBzZXQKCiAgICBmdW4gYWR2YW5jZShkaXN0YW5jZTogRmxvYXQsIGVsYXBzZWRTZWNvbmRzOiBGbG9hdCwgcm93SGVpZ2h0OiBGbG9hdCwgc3BlZWRGYWN0b3I6IEZsb2F0ID0gMWYpOiBGbG9hdCB7CiAgICAgICAgdmFsIGR0ID0gZWxhcHNlZFNlY29uZHMuY29lcmNlSW4oLjAwMWYsIC4wNWYpCiAgICAgICAgLy8gVHJpYWwgcHJvZmlsZTogMjUlIHNsb3dlciB0aGFuIHRoZSBvcmlnaW5hbCwgcmV0YWluaW5nIGZyYW1lIGNhZGVuY2UgYW5kIGtleSBpbnRlbnRzLgogICAgICAgIHZhbCBzcGVlZCA9IHNwZWVkRmFjdG9yLmNvZXJjZUluKDFmLCAzLjU5Mzc1ZikgKiAuNzVmCiAgICAgICAgdmFsIG9tZWdhID0gMThmICogc3BlZWQKICAgICAgICB2YWwgZXJyb3IgPSAtZGlzdGFuY2UKICAgICAgICB2YWwgZGVjYXkgPSBrb3RsaW4ubWF0aC5leHAoLW9tZWdhICogZHQpCiAgICAgICAgdmFsIG5leHRFcnJvciA9IChlcnJvciArICh2ZWxvY2l0eSArIG9tZWdhICogZXJyb3IpICogZHQpICogZGVjYXkKICAgICAgICB2YWwgbmV4dFZlbG9jaXR5ID0gKHZlbG9jaXR5IC0gb21lZ2EgKiAodmVsb2NpdHkgKyBvbWVnYSAqIGVycm9yKSAqIGR0KSAqIGRlY2F5CiAgICAgICAgdmFsIGxpbWl0ID0gcm93SGVpZ2h0LmNvZXJjZUF0TGVhc3QoMWYpICogNmYgKiBzcGVlZAogICAgICAgIHZhbCByZXF1ZXN0ZWQgPSBuZXh0RXJyb3IgLSBlcnJvcgogICAgICAgIHZhbCBkZWx0YSA9IHJlcXVlc3RlZC5jb2VyY2VJbigtbGltaXQgKiBkdCwgbGltaXQgKiBkdCkKICAgICAgICB2ZWxvY2l0eSA9IGlmIChkZWx0YSAhPSByZXF1ZXN0ZWQpIGRlbHRhIC8gZHQgZWxzZSBuZXh0VmVsb2NpdHkuY29lcmNlSW4oLWxpbWl0LCBsaW1pdCkKICAgICAgICByZXR1cm4gZGVsdGEKICAgIH0KfQo=')
$current=[IO.File]::ReadAllBytes($target)
if([Convert]::ToBase64String($current) -ne [Convert]::ToBase64String($bytes)){
 $backup=Join-Path $root ('.rolagem-referencia-resgate/'+[Guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $backup -Force | Out-Null
 $saved=Join-Path $backup 'CatalogScrollMotion.kt'
 Copy-Item -LiteralPath $target -Destination $saved
 if((Get-FileHash -LiteralPath $target).Hash -ne (Get-FileHash -LiteralPath $saved).Hash){throw 'Falha ao verificar copia de seguranca. Nada foi restaurado.'}
 Write-Host ('Copia do arquivo atual: '+$saved)
 [IO.File]::WriteAllBytes($target,$bytes)
}
Write-Host 'Restaurado o perfil anterior de rolagem (25%). Compilando...'
Push-Location $root
try{
 & ./gradlew.bat --no-daemon --no-configuration-cache --max-workers=2 :app:assembleDesempenho
 if($LASTEXITCODE -ne 0){throw 'Compilacao falhou. A instalacao nao foi iniciada.'}
}finally{Pop-Location}
& $installer -Projeto $root
if($LASTEXITCODE -ne 0){throw 'Fontes restauradas, mas a instalacao falhou. Execute Instalar-TV.ps1 novamente.'}
