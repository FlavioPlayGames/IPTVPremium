param([string]$Arquivo=(Join-Path $env:USERPROFILE ('Downloads/comparacao-catalogos-{0}.json' -f (Get-Date -Format 'yyyyMMdd-HHmmss'))))
$ErrorActionPreference='Stop';$ProgressPreference='SilentlyContinue'
Add-Type -AssemblyName System.Web
Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Collections.Generic;
public class CatalogTitleSet {
 public long Rows;
 public HashSet<string> Names=new HashSet<string>(StringComparer.Ordinal);
 public HashSet<string> Ids=new HashSet<string>(StringComparer.Ordinal);
 public long MissingNames;
 public void Add(string name,string id) { Rows++; var key=Key(name);if(key.Length>0)Names.Add(key);else MissingNames++;if(!String.IsNullOrWhiteSpace(id))Ids.Add(id.Trim()); }
 public static string Key(string s) { return Regex.Replace((s??"").Normalize(NormalizationForm.FormKC).Trim(),@"\s+"," ").ToUpperInvariant(); }
}
public class CatalogM3uScan {
 public CatalogTitleSet Movies=new CatalogTitleSet(),Series=new CatalogTitleSet(),Live=new CatalogTitleSet();
 public long Episodes,Entries,Unclassified,Lines,FourDigitEpisodes;
 static string Attr(string s,string key) { var m=Regex.Match(s,@"(?:^|\s)"+Regex.Escape(key)+"\\s*=\\s*\"([^\"]*)\"",RegexOptions.IgnoreCase);return m.Success?m.Groups[1].Value:null; }
 static string Title(string s) { bool q=false;for(int i=0;i<s.Length;i++){if(s[i]=='"')q=!q;if(s[i]==','&&!q)return s.Substring(i+1).Trim();}return ""; }
 static string Fold(string s) { var b=new StringBuilder();foreach(char c in (s??"").Normalize(NormalizationForm.FormD))if(System.Globalization.CharUnicodeInfo.GetUnicodeCategory(c)!=System.Globalization.UnicodeCategory.NonSpacingMark)b.Append(c);return b.ToString().ToLowerInvariant(); }
 public static CatalogM3uScan Read(TextReader reader) {
  var result=new CatalogM3uScan();string ext=null,group=null,line;
  var ep=new Regex(@"(?i)\bS\d{1,3}\s*E\d{1,6}\b");
  while((line=reader.ReadLine())!=null) {
   result.Lines++;if((result.Lines%8192)==0 && result.Lines>12000000)throw new InvalidDataException("line_limit");
   line=line.Trim().TrimStart('\uFEFF');if(line.Length==0)continue;
   if(line.StartsWith("#EXTINF:",StringComparison.OrdinalIgnoreCase)){ext=line;group=Attr(line,"group-title");continue;}
   if(line.StartsWith("#EXTGRP:",StringComparison.OrdinalIgnoreCase)){group=line.Substring(8).Trim();continue;}
   if(line.StartsWith("#"))continue;
   if(ext==null)continue;
   string name=Attr(ext,"tvg-name");if(String.IsNullOrWhiteSpace(name))name=Title(ext);
   string xui=Attr(ext,"xui-id"),tvg=Attr(ext,"tvg-id"),g=Fold(group);
   var match=ep.Match(name??"");string path="";Uri u;if(Uri.TryCreate(line,UriKind.Absolute,out u))path=u.AbsolutePath.ToLowerInvariant();
   int kind=0; // 1 live, 2 movie, 3 episode/series; this is a comparison hint, not an app importer.
   if(Regex.IsMatch(path,@"/(series|serie)/"))kind=3;
   else if(Regex.IsMatch(path,@"/(movie|movies|vod)/"))kind=2;
   else if(path.Contains("/live/"))kind=1;
   else if(Regex.IsMatch(g,@"\b(canais|canal|channels|channel|live|radio|radios)\b|24\s*(h|horas)\b|ao vivo"))kind=1;
   else if(match.Success)kind=3;
   else {
    bool series=Regex.IsMatch(g,@"\b(series|serie|seriados|seriado|minisserie|temporada|season)\b");
    bool movies=Regex.IsMatch(g,@"\b(filmes|filme|movies|movie|cinema|vod|lancamentos|lancamento)\b");
    if(series&&!movies)kind=3;else if(movies&&!series)kind=2;
    else if(xui!=null&&tvg!=null)kind=1;
    else if(Regex.IsMatch(path,@"\.(mp4|mkv|avi|mov|m4v|webm|wmv|mpg|mpeg|flv|divx|3gp|vob)$"))kind=2;
    else if(xui!=null)kind=2;
   }
   result.Entries++;
   if(kind==1)result.Live.Add(name,xui);
   else if(kind==2)result.Movies.Add(name,xui);
   else if(kind==3) {
    result.Episodes++;
    if(match.Success && Regex.IsMatch(match.Value,@"(?i)E\d{4,}"))result.FourDigitEpisodes++;
    string show=match.Success?name.Substring(0,match.Index).Trim().TrimEnd('-','|',':').Trim():name;
    result.Series.Add(show,null);
   }else result.Unclassified++;
   ext=null;group=null;
  }
  return result;
 }
}
'@

function Open-Reader([string]$Address) {
 $req=[Net.HttpWebRequest]::Create($Address);$req.UserAgent='Mozilla/5.0';$req.Timeout=45000;$req.ReadWriteTimeout=45000
 $req.AutomaticDecompression=[Net.DecompressionMethods]::GZip -bor [Net.DecompressionMethods]::Deflate
 $resp=$req.GetResponse()
 return [pscustomobject]@{request=$req;response=$resp;reader=[IO.StreamReader]::new($resp.GetResponseStream(),[Text.Encoding]::UTF8,$true)}
}
function Close-Reader($r){if($r){$r.reader.Dispose();$r.response.Dispose();$r.request.Abort()}}
function Read-Api([string]$Action,[string]$IdKey,[CatalogTitleSet]$Set){
 $r=$null
 try {
  $r=Open-Reader ($api+'?'+$query+'&action='+$Action)
  $body=[Text.StringBuilder]::new();$buffer=New-Object char[] 8192
  while(($read=$r.reader.Read($buffer,0,$buffer.Length))-gt 0){
   if($body.Length+$read-gt 64000000){throw [IO.InvalidDataException]::new('response_limit')}
   [void]$body.Append($buffer,0,$read)
  }
  $text=$body.ToString();$body=$null
  $trimmed=$text.Trim();if(!$trimmed.StartsWith('[')-or !$trimmed.EndsWith(']')){throw [IO.InvalidDataException]::new('incomplete_array')}
  $data=ConvertFrom-Json -InputObject $text -ErrorAction Stop
  foreach($item in $data){$Set.Add([string]$item.name,[string]$item.$IdKey)}
 }finally{Close-Reader $r;$text=$null;$data=$null}
}
function Safe-Sample([string]$Name){
 $value=[regex]::Replace($Name,'(?i)https?://\S+','[LINK OMITIDO]')
 foreach($key in @('username','password')){if($p[$key]){$value=$value.Replace(([string]$p[$key]).ToUpperInvariant(),'[OCULTO]')}}
 return $value
}
function Compare-Sets([CatalogTitleSet]$ApiSet,[CatalogTitleSet]$M3uSet){
 $common=0;foreach($name in $ApiSet.Names){if($M3uSet.Names.Contains($name)){$common++}}
 $onlyApiSamples=@();foreach($name in $ApiSet.Names){if(!$M3uSet.Names.Contains($name)){$onlyApiSamples+= (Safe-Sample $name);if($onlyApiSamples.Count-ge 12){break}}}
 $onlyM3uSamples=@();foreach($name in $M3uSet.Names){if(!$ApiSet.Names.Contains($name)){$onlyM3uSamples+= (Safe-Sample $name);if($onlyM3uSamples.Count-ge 12){break}}}
 $idCommon=0;foreach($id in $ApiSet.Ids){if($M3uSet.Ids.Contains($id)){$idCommon++}}
 return [ordered]@{apiRows=$ApiSet.Rows;m3uRows=$M3uSet.Rows;apiUniqueTitles=$ApiSet.Names.Count;m3uUniqueTitles=$M3uSet.Names.Count;commonTitles=$common;onlyApiTitles=($ApiSet.Names.Count-$common);onlyM3uTitles=($M3uSet.Names.Count-$common);apiMissingTitles=$ApiSet.MissingNames;m3uMissingTitles=$M3uSet.MissingNames;apiUniqueIds=$ApiSet.Ids.Count;m3uXuiIds=$M3uSet.Ids.Count;commonNumericIdentifiers=$idCommon;onlyApiSamples=$onlyApiSamples;onlyM3uSamples=$onlyM3uSamples}
}

Write-Host 'Comparacao R2: reconhece episodios com quatro ou mais digitos e inclui ate 12 titulos exclusivos de cada lado. Nao altera o aplicativo.'
$secret=Read-Host -AsSecureString 'Cole o link get.php (entrada oculta)'
$plain=[Net.NetworkCredential]::new('', $secret).Password
try{
 $uri=$null;$m3u=$plain.Trim().Replace('&amp;','&').Replace('\&','&')
 if(![Uri]::TryCreate($m3u,[UriKind]::Absolute,[ref]$uri)-or $uri.Scheme -notin @('http','https')-or $uri.UserInfo-or $uri.AbsolutePath -notmatch '(?i)/get\.php/?$'){throw 'Link get.php nao reconhecido.'}
 $p=[Web.HttpUtility]::ParseQueryString($uri.Query)
 if(!$p['username']-or !$p['password']){throw 'Parametros de conta ausentes.'}
 $api=$uri.GetLeftPart([UriPartial]::Authority)+($uri.AbsolutePath -replace '(?i)/get\.php/?$','')+'/player_api.php'
 $query='username='+[Uri]::EscapeDataString($p['username'])+'&password='+[Uri]::EscapeDataString($p['password'])
}finally{$plain=$null;$secret.Dispose()}
$started=Get-Date;$report=[ordered]@{version='coverage-r2';startedAt=$started.ToString('o');complete=$false;appChanged=$false;coverageVerified=$false;errorType=$null;httpStatus=0}
try{
 $movies=[CatalogTitleSet]::new();$series=[CatalogTitleSet]::new();$live=[CatalogTitleSet]::new()
 Write-Host 'Lendo filmes da API...';Read-Api 'get_vod_streams' 'stream_id' $movies
 Write-Host 'Lendo series da API...';Read-Api 'get_series' 'series_id' $series
 Write-Host 'Lendo canais da API...';Read-Api 'get_live_streams' 'stream_id' $live
 Write-Host 'Lendo a M3U em fluxo. Esta etapa depende da resposta do provedor; aguarde...'
 $r=$null
 try{$r=Open-Reader $m3u;$scan=[CatalogM3uScan]::Read($r.reader)}finally{Close-Reader $r}
 $report.movies=Compare-Sets $movies $scan.Movies;$report.series=Compare-Sets $series $scan.Series;$report.live=Compare-Sets $live $scan.Live
 $report.m3uEntries=$scan.Entries;$report.m3uEpisodeEntries=$scan.Episodes;$report.m3uFourDigitEpisodeEntries=$scan.FourDigitEpisodes;$report.unclassified=$scan.Unclassified
 $report.complete=($scan.Entries -gt 0 -and $movies.Rows -gt 0 -and $series.Rows -gt 0 -and $live.Rows -gt 0)
 $report.note='Comparacao de titulos normalizados, nao prova equivalencia de URLs ou IDs. IDs xui-id podem nao pertencer ao mesmo namespace da API. Series M3U agrupadas pelo marcador S com ate 3 digitos e E com ate 6 digitos; nomes ambiguos e entradas nao classificadas exigem analise. O catalogo pode mudar entre as consultas. Nenhuma troca de transporte foi feita.'
}catch{
 $e=$_.Exception
 for($d=0;$d -lt 6;$d++){if($e.Response){try{$report.httpStatus=[int]$e.Response.StatusCode}catch{}};if(!$e.InnerException){break};$e=$e.InnerException}
 $report.errorType=$e.GetType().Name
 Write-Host 'Nao foi possivel concluir; o relatorio parcial sera salvo sem mensagens sensiveis.'
}finally{$query=$null;$api=$null;$m3u=$null;$uri=$null;$p=$null}
$report.finishedAt=(Get-Date).ToString('o');$report.elapsedSeconds=[math]::Round(((Get-Date)-$started).TotalSeconds,1)
$parent=Split-Path -Parent $Arquivo;if($parent){New-Item -ItemType Directory -Force -Path $parent|Out-Null}
$report|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $Arquivo -Encoding UTF8
Write-Host ('Envie o relatorio: '+$Arquivo)
