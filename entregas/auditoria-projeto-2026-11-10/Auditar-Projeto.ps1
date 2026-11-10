[CmdletBinding()]
param(
    [string]$Projeto = "$env:USERPROFILE\AndroidStudioProjects\IPTVPremium",
    [string]$Downloads = "$env:USERPROFILE\Downloads"
)
$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path -LiteralPath $Projeto).Path.TrimEnd([char[]]'\/')
if (!(Test-Path -LiteralPath $raiz -PathType Container)) { throw 'Pasta do projeto nao encontrada.' }
if (!(Test-Path -LiteralPath $Downloads -PathType Container)) { throw 'Pasta Downloads nao encontrada.' }
$destinoBase = (Resolve-Path -LiteralPath $Downloads).Path.TrimEnd([char[]]'\/')
$separador = [IO.Path]::DirectorySeparatorChar
if ($destinoBase -eq $raiz -or $destinoBase.StartsWith($raiz + $separador, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'A pasta de saida precisa ficar fora do projeto.'
}
$nome = 'IPTV-auditoria-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
$saida = Join-Path $destinoBase $nome
New-Item -ItemType Directory -Path $saida | Out-Null
$arquivos = [Collections.Generic.List[object]]::new()
$problemas = [Collections.Generic.List[object]]::new()
$pastas = [Collections.Generic.Stack[string]]::new()
$pastas.Push($raiz)
function Relativo([string]$Caminho) {
    $Caminho.Substring($raiz.Length).TrimStart([char[]]'\/').Replace('\','/')
}
function Grupo([string]$Caminho) {
    if ($Caminho -match '(^|/)\.git(/|$)') { return 'Versionamento - preservar' }
    if ($Caminho -match '(^|/)(build|\.gradle|\.kotlin|\.cxx|\.externalNativeBuild)(/|$)') { return 'Gerado ou cache - conferir' }
    if ($Caminho -match '(^|/)(\.iptv-backups|backups?|atualizacoes|entregas|old|antigos?)(/|$)') { return 'Possivel backup ou entrega - revisar' }
    if ($Caminho -match '^app/src/') { return 'Fonte ou recurso - preservar ate analisar referencias' }
    return 'Outros - revisar referencias'
}
while ($pastas.Count -gt 0) {
    $pasta = $pastas.Pop()
    try { $itens = @(Get-ChildItem -LiteralPath $pasta -Force -ErrorAction Stop) }
    catch {
        $problemas.Add([pscustomobject]@{Caminho=(Relativo $pasta);Motivo='Pasta nao enumerada'})
        continue
    }
    foreach ($item in $itens) {
        $rel = Relativo $item.FullName
        if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            $problemas.Add([pscustomobject]@{Caminho=$rel;Motivo='Link ou juncao ignorado'})
            continue
        }
        if ($item.PSIsContainer) { $pastas.Push($item.FullName); continue }
        try {
            $arquivos.Add([pscustomobject]@{
                Caminho=$rel;Bytes=[long]$item.Length;Modificado=$item.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss');Grupo=(Grupo $rel)
            })
        } catch {
            $problemas.Add([pscustomobject]@{Caminho=$rel;Motivo='Metadados indisponiveis'})
        }
    }
    if ($arquivos.Count -gt 0) {
        Write-Progress -Activity 'Inventariando o projeto, sem ler o conteudo dos arquivos' -Status "$($arquivos.Count) arquivos encontrados"
    }
}
Write-Progress -Activity 'Inventariando o projeto, sem ler o conteudo dos arquivos' -Completed
$lista = $arquivos.ToArray()
$lista | Sort-Object Caminho | Export-Csv -LiteralPath (Join-Path $saida 'arquivos.csv') -NoTypeInformation -Encoding UTF8
# Resumo ate dois niveis: inclui o tamanho recursivo de cada pasta listada.
$totais = @{}
foreach ($arquivo in $lista) {
    $partes = $arquivo.Caminho.Split('/')
    $maximo = [Math]::Min(2, $partes.Length - 1)
    if ($maximo -eq 0) {
        if (!$totais.ContainsKey('[arquivos da raiz]')) { $totais['[arquivos da raiz]'] = [long]0 }
        $totais['[arquivos da raiz]'] += $arquivo.Bytes
    }
    for ($nivel=1; $nivel -le $maximo; $nivel++) {
        $chave = ($partes[0..($nivel-1)] -join '/')
        if (!$totais.ContainsKey($chave)) { $totais[$chave] = [long]0 }
        $totais[$chave] += $arquivo.Bytes
    }
}
$totais.GetEnumerator() | ForEach-Object {
    [pscustomobject]@{Pasta=$_.Key;Bytes=$_.Value;MB=[Math]::Round($_.Value/1000000,2)}
} | Sort-Object Bytes -Descending | Export-Csv -LiteralPath (Join-Path $saida 'pastas.csv') -NoTypeInformation -Encoding UTF8
$problemas.ToArray() | Export-Csv -LiteralPath (Join-Path $saida 'avisos.csv') -NoTypeInformation -Encoding UTF8
$total = ($lista | Measure-Object -Property Bytes -Sum).Sum
@(
    "Arquivos: $($arquivos.Count)"
    "Bytes encontrados: $total"
    "MB encontrados: $([Math]::Round($total/1000000,2))"
    "Avisos: $($problemas.Count)"
    'Somente nomes, tamanhos e datas foram coletados. O conteudo dos arquivos nao foi lido.'
    'Nenhum arquivo do projeto foi apagado ou alterado.'
    'Os grupos sao indicios para revisao, nao autorizam exclusao automatica.'
    'Links/juncoes e pastas inacessiveis nao participam do total.'
    'pastas.csv inclui subtotais sobrepostos; nao some suas linhas.'
    'O tamanho do projeto no computador nao representa o tamanho instalado do aplicativo.'
) | Set-Content -LiteralPath (Join-Path $saida 'LEIA-ME.txt') -Encoding UTF8
$zip = $saida + '.zip'
Compress-Archive -LiteralPath @((Join-Path $saida 'arquivos.csv'),(Join-Path $saida 'pastas.csv'),(Join-Path $saida 'avisos.csv'),(Join-Path $saida 'LEIA-ME.txt')) -DestinationPath $zip
Write-Host "Auditoria concluida. Envie este ZIP de relatorios: $zip"
