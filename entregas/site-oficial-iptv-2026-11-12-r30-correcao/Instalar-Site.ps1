[CmdletBinding()]
param([string]$Destino="$env:USERPROFILE\AndroidStudioProjects\Site IPTV")
$ErrorActionPreference='Stop'
$source=Join-Path $PSScriptRoot 'site'
$manifest=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'manifesto.json') -Raw|ConvertFrom-Json
$manifest=@($manifest | ForEach-Object { $_ })
if($manifest.Count -eq 0){throw 'Manifesto vazio.'}
foreach($item in $manifest){
    $p=Join-Path $source ([string]$item.path)
    if(!(Test-Path -LiteralPath $p -PathType Leaf) -or (Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash -ne $item.sha256){throw "Pacote invalido: $($item.path)"}
}
if(Test-Path -LiteralPath $Destino){
    foreach($entry in @(Get-ChildItem -LiteralPath $Destino -Force)){
        if($entry.Name -in @('node_modules','.wrangler','.worker-preview','manutencao-iptv')){continue}
        $expected=@($manifest|Where-Object {($_.path -split '/')[0] -eq $entry.Name})
        if($expected.Count -eq 0){throw "Destino ja contem arquivos. Preserve e confira antes de instalar: $($entry.FullName)"}
    }
    foreach($item in $manifest){
        $p=Join-Path $Destino ([string]$item.path)
        if((Test-Path -LiteralPath $p) -and (!(Test-Path -LiteralPath $p -PathType Leaf) -or (Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash -ne $item.sha256)){throw "Arquivo divergente no destino; nenhuma escrita feita: $($item.path)"}
    }
}
New-Item -ItemType Directory -Path $Destino -Force|Out-Null
foreach($item in $manifest){
    $target=Join-Path $Destino ([string]$item.path)
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force|Out-Null
    Copy-Item -LiteralPath (Join-Path $source ([string]$item.path)) -Destination $target -Force
}
foreach($item in $manifest){if((Get-FileHash -LiteralPath (Join-Path $Destino ([string]$item.path)) -Algorithm SHA256).Hash -ne $item.sha256){throw "Falha na verificacao: $($item.path)"}}
Write-Host "Projeto instalado e conferido em: $Destino"
Write-Host 'A pasta antiga foi preservada. Nenhuma publicacao foi executada.'
