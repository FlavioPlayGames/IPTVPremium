param([Parameter(Mandatory=$true)][string]$Projeto)
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'Desfazer.ps1') -Projeto $Projeto -Testar
if($LASTEXITCODE -ne 0){throw 'Falha ao restaurar/compilar. A instalacao nao foi iniciada.'}
& (Join-Path $PSScriptRoot 'Instalar-TV.ps1') -Projeto $Projeto
if($LASTEXITCODE -ne 0){throw 'Fontes restauradas, mas a instalacao falhou. Execute Instalar-TV.ps1 novamente.'}
