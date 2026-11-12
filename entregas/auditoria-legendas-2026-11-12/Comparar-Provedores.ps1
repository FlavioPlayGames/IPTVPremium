[CmdletBinding()]
param([int]$Temporada=1,[int]$Episodio=3)
$ErrorActionPreference='Stop'
[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12
$saida=Join-Path "$env:USERPROFILE\Downloads" 'IPTV-respostas-provedores.txt'
$report=New-Object System.Collections.Generic.List[string]
$report.Add("Respostas de pesquisa; temporada=$Temporada episodio=$Episodio. Nenhum download de legenda.")
function Ler-Segredo([string]$Mensagem) {
    $secure=Read-Host $Mensagem -AsSecureString
    $ptr=[IntPtr]::Zero
    try {
        $ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        if($ptr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)}
        $secure.Dispose()
    }
}
function Api([string]$Url,[hashtable]$Headers,[string]$Body='') {
    Start-Sleep -Seconds 1
    try {
        if($Body){return Invoke-RestMethod -Uri $Url -Method Post -Headers $Headers -ContentType 'application/json' -Body $Body -TimeoutSec 30 -MaximumRedirection 0}
        return Invoke-RestMethod -Uri $Url -Method Get -Headers $Headers -TimeoutSec 30 -MaximumRedirection 0
    } catch {
        $code=0
        if($null -ne $_.Exception.Response){$code=[int]$_.Exception.Response.StatusCode}
        throw "Falha HTTP $code (0 = falha de transporte). Corpo e credenciais omitidos."
    }
}
function Registrar-Erro($Erro) {
    if($Erro.Exception.Message -match '^Falha HTTP ([0-9]+) '){$report.Add("Falha HTTP $($Matches[1]); detalhes privados omitidos.")}
    else {$report.Add('Falha na consulta ou autenticacao; detalhes privados omitidos.')}
}
function Texto($Valor) {
    if($null -eq $Valor){return '-'}
    return ([string]$Valor -replace '[\r\n\t]',' ').Substring(0,[Math]::Min(([string]$Valor).Length,100))
}
$series=@(
    @{Nome='The Handmaids Tale';Tmdb=69478},
    @{Nome='Fear the Walking Dead';Tmdb=62286},
    @{Nome='The Good Doctor';Tmdb=71712},
    @{Nome='House';Tmdb=1408},
    @{Nome='Teen Wolf';Tmdb=34524}
)
$subKey='';$openKey='';$password='';$token='';$subHeaders=@{};$openHeaders=@{};$loginHeaders=@{}
try {
    Write-Host 'As chaves sao digitadas ocultas e nao entram no relatorio. Enter pula o provedor.'
    $subKey=Ler-Segredo 'Chave pessoal SubDL (ou Enter para pular)'
    if($subKey) {
        $subHeaders=@{Authorization="Bearer $subKey";Accept='application/json'}
        foreach($serie in $series) {
            Write-Host "SubDL: $($serie.Nome)"
            $report.Add("");$report.Add("SubDL | $($serie.Nome) | tmdb=$($serie.Tmdb)")
            try {
                # Same v2 endpoint/authentication as the app; omit language to inspect actual returned values.
                $url="https://api.subdl.com/api/v2/subtitles/search?type=tv&tmdb_id=$($serie.Tmdb)&season=$Temporada&episode=$Episodio&subs_per_page=30&unpack=1"
                $r=Api $url $subHeaders
                $report.Add("status=$(Texto $r.status); campos-raiz=$($r.PSObject.Properties.Name -join ',')")
                if($r.status -eq $false){$report.Add('API recusou a consulta. Mensagem bruta omitida.');continue}
                $rows=@($r.subtitles | Where-Object {$null -ne $_})
                $report.Add("Pacotes recebidos: $($rows.Count). Limite solicitado: 30. Sem filtro explicito de idioma; possiveis padroes do servidor nao sao presumidos.")
                foreach($item in $rows) {
                    $report.Add("pacote lang=[$(Texto $item.lang)] language=[$(Texto $item.language)] season=$(Texto $item.season) episode=$(Texto $item.episode) full_season=$(Texto $item.full_season)")
                    foreach($file in @($item.unpack_files | Where-Object {$null -ne $_} | Select-Object -First 30)) {
                        $report.Add("arquivo lang=[$(Texto $file.lang)] language=[$(Texto $file.language)] season=$(Texto $file.season) episode=$(Texto $file.episode)")
                    }
                }
            } catch {Registrar-Erro $_}
        }
    }
    $openKey=Ler-Segredo 'Chave tecnica do aplicativo OpenSubtitles (ou Enter para pular)'
    if($openKey) {
        $agent=Read-Host 'User-Agent cadastrado no OpenSubtitles (Enter usa IPTVPremium v1.13)'
        if(!$agent){$agent='IPTVPremium v1.13'}
        $user=Read-Host 'Usuario da sua conta OpenSubtitles.com'
        $password=Ler-Segredo 'Senha OpenSubtitles.com'
        $loginHeaders=@{'Api-Key'=$openKey;'User-Agent'=$agent;Accept='application/json'}
        try {
            $body=@{username=$user;password=$password}|ConvertTo-Json -Compress
            $login=Api 'https://api.opensubtitles.com/api/v1/login' $loginHeaders $body
            $body='';$password=''
            if(!$login.token){throw 'Login sem token. Resposta bruta omitida.'}
            $token=[string]$login.token
            $base='https://api.opensubtitles.com/api/v1'
            if($login.base_url) {
                $hostText=([string]$login.base_url).Trim().TrimEnd('/')
                if($hostText -eq 'vip-api.opensubtitles.com' -or $hostText -eq 'https://vip-api.opensubtitles.com'){$base='https://vip-api.opensubtitles.com/api/v1'}
                elseif($hostText -ne 'api.opensubtitles.com' -and $hostText -ne 'https://api.opensubtitles.com'){throw 'Host retornado pelo login nao reconhecido; token nao enviado.'}
            }
            $openHeaders=@{'Api-Key'=$openKey;'User-Agent'=$agent;Authorization="Bearer $token";Accept='application/json'}
            foreach($serie in $series) {
                Write-Host "OpenSubtitles: $($serie.Nome)"
                $report.Add('');$report.Add("OpenSubtitles | $($serie.Nome) | tmdb=$($serie.Tmdb)")
                try {
                    for($page=1;$page -le 5;$page++) {
                        # Documented canonical trailing slash avoids the redirect seen in app logs.
                        $url="$base/subtitles/?type=episode&parent_tmdb_id=$($serie.Tmdb)&season_number=$Temporada&episode_number=$Episodio&page=$page"
                        $r=Api $url $openHeaders
                        $rows=@($r.data | Where-Object {$null -ne $_})
                        $report.Add("pagina=$page paginas=$(Texto $r.total_pages) recebidas=$($rows.Count)")
                        foreach($row in $rows) {
                            $a=$row.attributes;$f=$a.feature_details
                            $report.Add("language=[$(Texto $a.language)] type=$(Texto $f.feature_type) parent_tmdb_id=$(Texto $f.parent_tmdb_id) season_number=$(Texto $f.season_number) episode_number=$(Texto $f.episode_number)")
                        }
                        if(!$r.total_pages -or $page -ge $r.total_pages){break}
                        if($page -eq 5){$report.Add('Limite de cinco paginas atingido; extracao parcial.')}
                    }
                } catch {Registrar-Erro $_}
            }
        } catch {Registrar-Erro $_}
    }
} finally {
    $password='';$token='';$openKey='';$subKey='';$body='';$login=$null
    $subHeaders.Clear();$openHeaders.Clear();$loginHeaders.Clear()
    $report | Set-Content -LiteralPath $saida -Encoding UTF8
    Write-Host "Relatorio: $saida"
}
