# IPTVPremium: corrige o triangulo preto nos efeitos do player. Windows PowerShell 5.1 / PowerShell 7.
[CmdletBinding()]
param([string]$Projeto = (Get-Location).Path)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
function Get-BytesHash([byte[]]$Bytes) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($Bytes))).Replace('-', '') }
    finally { $sha.Dispose() }
}
function Get-TextHash([byte[]]$Bytes) {
    $text = [Text.Encoding]::UTF8.GetString($Bytes).TrimStart([char]0xFEFF).Replace("`r`n", "`n")
    return Get-BytesHash ([Text.Encoding]::UTF8.GetBytes($text))
}
$files = @(
    @{ Path = 'app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/PlaybackImageEffects.kt'; Allowed = @('09DE39B540CD04B1A4EC501CF2C707F07252B716EAC4451A77E3BCF2AE0B3E85'); MayCreate = $false; After = 'D9283AD762EB31BBF65129F46FC7A6590923A1946BB3F2CF616944799E9A1CBE'; PayloadHash = 'D9283AD762EB31BBF65129F46FC7A6590923A1946BB3F2CF616944799E9A1CBE'; Data = 'H4sIAAAAAAACA7VY23LbNhB911ds/ZAhU4VOL09qlamdxI5m4thjxeljBiKXIhoQ4ACgYzXjT+pX9Me6AC8mKUpWJg1fJAF7PXt2Aapg8Se2RljpKFZ5lAp2y5VVWqOJeGFvC405L3Pa1BgVgm1QTyY8L5S2wGSiFU9oT1qUNnrpPu/scFsVKNciOn/7evnz88HmXZRjwtkvznmuZPQ6TTG2jwh94AmqM81yvNIqRmO4XL++i7GwXMlHdEvLKRRBimsycJjwDX0cIrnkf+MhcjfSWLYSeFLwXeLocYhOmcFzscxYgvqRkGuNc7Efwlasb3Ry/PQpeEifJZrfooQLLw9Fi/BvwC1IvEUNJs4wKQUaoHJrBqlTNMAs2Awh4cYRBTSmxKIMNLMYwdPjyR9tMExKZZkrV3RZ2IUMOpDMZrFgxoQTTmzSkglQq78oZrgioyui6yInwlZZGvgyAXrSUkKskRwFcUXCGdRsnMItEyXOBuoyYzLGnGg7hVKasnBoYTKDYGk1ZRvCsxdwI7kNZ/CWG/t75fBF7dA9PIXA245ylSDM530XF7QYXZ6dhQSELbUEzAu7cbaCsLVB+pAjeYwNzKGOPSLYVKkJ96jG8qIS6amRUCksaeWlh85ZvkybODs+DonzbHn9U9iY/HEOS9ti3CA6BatLrOGcttbdU2cQfeaJza74HQozbRcz5OvMNqsdrB8ifMRvyoRpHf8Pznw1Kp9+9X7iPwoiPjEIPP16kTi4WVH0OQVlYWImiFmnSglkslp9jG094LYfZ6Iu+p8uwRksZO2vXn7jU6zWDzC2n9wwg2ZidJitqMk1DVnfV1YNhsVIi5UG3yS6RSJ0Vns6MO+F6lFpzBGy0wbMtspdDKb91Bt3O+rbC56bd+qyCLgsStvF0y90kAzb2Kmh6ljgyZNe0L2SXinB402kSkuG3ODv+OiZ35tK6Lqxo0lgd3XD3fTsIbhdkEZ4lKePsKan/I107tka4fXI9lfwu6eedQg4AGBvC7Q+SHv7uA0yRzSajQ/d0YMHyepdZ3RTbdAaf+cJjoy3ZI65Q+1jJRuthRFHYbQqqes0JtfoZIIwIlLDFzpjafqz5D3ZCkIq/ZjTWBWbw1w6yW92qKEgFzbTqlxn5JcQGZdqWh3a+1VQ5Tz1ZxDphVXsNJxxEH6v0lUuTqfmbgiDxJCZskqsMjbY1jEzdd59w4eBEE4eDk+iSKf2zXnqc+nwKji6QJEpzRkkaOhmc8sSRrcl47D59x+6r8KbV9fHr5RYbeADN+7mM4ithi8iNE59lCeWmLoq6U5zxK6U4e66dDSF6j4ardG+UzpngmZP8lIpnXBJlThVpUxMEE7h1wfz9+PDkSqQ8nWp8aD56IbcAInqGsKMclNzbDx2EXq4gkzBN1V9rO+Ym+EW5rWnH+YgSyHCQSzu6bqrpMMtGWfphw6hx+x0y5GgQCpBuE9olPKO6Xu1vkele256Xesub1ti95Pdv1xxjav5HDqdSIci9ODbsvk9jsktJ77pd56bu/rqTChmDU39lPBcpFd0B6QzLDgqF7IOj1BPndCJ1mxzmXaijazy6g7wjquH5TD8ard0FNGrsc2GXqtWMfVuxHIqtR3Yr6+xHlNXpijl2hCM/rtB6u3k0QGQaPbZv/RVedL4WyTtweyD9O9o73mON4beg5Q7N/vtYvVmpIGa5Gm6jnROB5olywuB2nuuASJc6Ge9cVSD7QWm8Hy3sRWXSdtH5kQ25sxIAMfHdW/ByjcRMI1AfeVOBiAlYEDgc7qW0msq/ShQEwT0NkozS0Zb5qp/Nui8eUV4+hqaoF47f/vx/fXi5B39/Likb1eUw2jD1r1OL9bxJ7qRa630IPB7iJmNMwjQbc4aDRJu/voYG2WuTT/D3n9MKoNjJQ93jIcddNI0Kpmr+BhHtgYqJZTSIBPCbdLcRh09GBj6up/cT/4DwqST2KsSAAA=' }
)
$root = (Resolve-Path -LiteralPath $Projeto).Path
if (!(Test-Path -LiteralPath (Join-Path $root 'app/build.gradle.kts'))) {
    throw 'Abra o terminal na raiz de IPTVPremium (pasta com app e gradlew), ou informe -Projeto.'
}
# Verifica a versao de todos os arquivos antes de escrever qualquer mudanca.
$pending = @()
foreach ($file in $files) {
    $path = Join-Path $root $file.Path
    $present = Test-Path -LiteralPath $path -PathType Leaf
    $original = $null
    if ($present) {
        $original = [IO.File]::ReadAllBytes($path)
        $current = Get-TextHash $original
        if ($current -eq $file.After) { continue }
        if ($file.Allowed -notcontains $current) {
            throw "Arquivo com mudancas diferentes da versao enviada: $($file.Path). Nada foi alterado; suas mudancas foram preservadas."
        }
    } elseif (!$file.MayCreate) {
        throw "Arquivo necessario ausente: $($file.Path). Nada foi alterado."
    }
    $packed = [IO.MemoryStream]::new([Convert]::FromBase64String($file.Data))
    $gzip = [IO.Compression.GZipStream]::new($packed, [IO.Compression.CompressionMode]::Decompress)
    $plain = [IO.MemoryStream]::new()
    try { $gzip.CopyTo($plain); $bytes = $plain.ToArray() }
    finally { $gzip.Dispose(); $plain.Dispose(); $packed.Dispose() }
    if ((Get-BytesHash $bytes) -ne $file.PayloadHash) { throw 'Pacote com integridade invalida. Nada foi alterado.' }
    $pending += @{ Path = $path; Relative = $file.Path; Bytes = $bytes; Original = $original; Present = $present; Hash = $file.PayloadHash }
}
if ($pending.Count -eq 0) {
    Write-Host 'Esta atualizacao ja esta aplicada.' -ForegroundColor Green
    return
}
$backup = Join-Path $root ('.navigation-backups/correcao-imagem-player-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
[IO.Directory]::CreateDirectory($backup) | Out-Null
foreach ($file in $pending) {
    if ($file.Present) {
        $copy = Join-Path $backup $file.Relative
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($copy)) | Out-Null
        [IO.File]::Copy($file.Path, $copy, $false)
        if ((Get-FileHash -LiteralPath $copy -Algorithm SHA256).Hash -ne (Get-BytesHash $file.Original)) {
            throw 'Backup nao passou na verificacao. Nenhum arquivo do projeto foi alterado.'
        }
    }
}
@($pending | ForEach-Object { @{ path = $_.Relative; existed = $_.Present } }) |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'manifest.json') -Encoding UTF8
try {
    foreach ($file in $pending) {
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($file.Path)) | Out-Null
        [IO.File]::WriteAllBytes($file.Path, $file.Bytes)
        if ((Get-FileHash -LiteralPath $file.Path -Algorithm SHA256).Hash -ne $file.Hash) { throw "Falha ao gravar $($file.Relative)." }
    }
} catch {
    foreach ($file in $pending) {
        if ($file.Present) { [IO.File]::WriteAllBytes($file.Path, $file.Original) }
        elseif (Test-Path -LiteralPath $file.Path -PathType Leaf) { [IO.File]::Delete($file.Path) }
    }
    throw
}
Write-Host 'Correcao aplicada: quadro completo nos modos Leve e FSR 1.' -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host 'Compile e execute pelo Android Studio para validar no seu aparelho.'
