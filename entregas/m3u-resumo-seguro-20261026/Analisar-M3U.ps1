param([string]$Arquivo=(Join-Path $env:USERPROFILE ('Downloads/resumo-m3u-{0}.csv' -f (Get-Date -Format 'yyyyMMdd-HHmmss'))))
$ErrorActionPreference='Stop'
Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Text.RegularExpressions;
using System.Collections.Generic;
using System.Diagnostics;
public class M3uSafeRow {
 public string Categoria {get;set;}
 public string Caminho {get;set;}
 public string Extensao {get;set;}
 public string TipoDeclarado {get;set;}
 public long Entradas {get;set;}
 public string Exemplo1 {get;set;}
 public string Exemplo2 {get;set;}
 public string Atributos {get;set;}
}
public class M3uSafeResult {
 public long Total {get;set;}
 public bool Completo {get;set;}
 public List<M3uSafeRow> Grupos {get;set;}
}
public static class M3uSafeScanner {
 static readonly Regex Attr=new Regex("([a-zA-Z0-9_-]+)\\s*=\\s*\"([^\"]*)\"",RegexOptions.Compiled);
 static readonly Regex Url=new Regex(@"https?://[^\s]+",RegexOptions.IgnoreCase|RegexOptions.Compiled);
 static string Safe(string value,string[] secrets) {
  value=Url.Replace(value,"[URL]");
  foreach(string secret in secrets)if(!String.IsNullOrEmpty(secret))value=value.Replace(secret,"[DADO REMOVIDO]");
  return value.Length>180?value.Substring(0,180):value;
 }
 static string Title(string value) {
  bool quoted=false;
  for(int i=0;i<value.Length;i++){if(value[i]=='"')quoted=!quoted;else if(value[i]==','&&!quoted)return value.Substring(i+1).Trim();}
  return "";
 }
 public static M3uSafeResult Read(StreamReader reader,string[] secrets) {
  Dictionary<string,M3uSafeRow> groups=new Dictionary<string,M3uSafeRow>();
  long total=0;string pending=null;string separateGroup=null;string line;bool complete=true;
  Stopwatch timer=Stopwatch.StartNew();
  while((line=reader.ReadLine())!=null) {
   line=line.Trim().TrimStart('\uFEFF');
   if(line.StartsWith("#EXTINF",StringComparison.OrdinalIgnoreCase)){pending=line;separateGroup=null;continue;}
   if(line.StartsWith("#EXTGRP:",StringComparison.OrdinalIgnoreCase)){separateGroup=line.Substring(8).Trim();continue;}
   if(line.Length==0||line.StartsWith("#")||pending==null)continue;
   Dictionary<string,string> attrs=new Dictionary<string,string>(StringComparer.OrdinalIgnoreCase);
   foreach(Match match in Attr.Matches(pending))attrs[match.Groups[1].Value]=match.Groups[2].Value;
   string group; if(!attrs.TryGetValue("group-title",out group)||String.IsNullOrWhiteSpace(group))group=separateGroup??"Sem categoria";
   group=Safe(group,secrets);string title=Safe(Title(pending),secrets);pending=null;total++;
   Uri address;string path=Uri.TryCreate(line,UriKind.Absolute,out address)?address.AbsolutePath.ToLowerInvariant():"";
   string marker="SEM_MARCADOR";
   foreach(string part in path.Split('/'))if(part=="live"||part=="movie"||part=="movies"||part=="vod"||part=="series"||part=="serie"){marker=part;break;}
   string file=path.Substring(path.LastIndexOf('/')+1);int dot=file.LastIndexOf('.');
   string suffix=dot<0?"SEM_EXTENSAO":file.Substring(dot+1);
   if(Array.IndexOf(new string[]{"ts","m3u8","mp4","mkv","avi","mov","m4v","webm","wmv","mpg","mpeg","flv","SEM_EXTENSAO"},suffix)<0)suffix="OUTRA";
   string declared="NAO_INFORMADO";string value;
   foreach(string key in new string[]{"tvg-type","media-type","type","content-type"})if(attrs.TryGetValue(key,out value)){
    value=value.ToLowerInvariant();declared=Array.IndexOf(new string[]{"movie","movies","series","live","vod","channel","filme","serie"},value)>=0?value:"OUTRO";break;
   }
   string bucket=group+"\t"+marker+"\t"+suffix+"\t"+declared;M3uSafeRow row;
   if(!groups.TryGetValue(bucket,out row)) {
    if(groups.Count>=20000){complete=false;break;}
    List<string> keys=new List<string>(attrs.Keys);keys.Sort(StringComparer.OrdinalIgnoreCase);
    row=new M3uSafeRow{Categoria=group,Caminho=marker,Extensao=suffix,TipoDeclarado=declared,Exemplo1=title,Exemplo2="",Atributos=String.Join(";",keys.ToArray())};groups[bucket]=row;
   } else if(row.Exemplo2==""&&row.Exemplo1!=title)row.Exemplo2=title;
   row.Entradas++;
   if(timer.Elapsed.TotalMinutes>=10){complete=false;break;}
  }
  return new M3uSafeResult{Total=total,Completo=complete,Grupos=new List<M3uSafeRow>(groups.Values)};
 }
}
'@
$segura=Read-Host 'Cole a URL completa da M3U (a digitacao fica oculta)' -AsSecureString
$urlEmMemoria=$null;$response=$null;$reader=$null
try {
 $urlEmMemoria=[System.Net.NetworkCredential]::new('',$segura).Password
 $uri=[Uri]$urlEmMemoria
 if(!$uri.IsAbsoluteUri -or $uri.Scheme -notin @('http','https')){throw 'Formato de URL invalido.'}
 $segredos=[Collections.Generic.List[string]]::new()
 foreach($par in $uri.Query.TrimStart('?').Split('&')){
  $item=$par.Split('=',2)
  if($item.Count -eq 2 -and $item[0] -match '^(username|password|token|auth|key)$'){
   $segredos.Add($item[1]);$segredos.Add([Uri]::UnescapeDataString($item[1].Replace('+',' ')))
  }
 }
 $request=[Net.HttpWebRequest]::Create($uri)
 $request.Method='GET';$request.UserAgent='IPTVPremium/1.0';$request.Timeout=45000;$request.ReadWriteTimeout=30000
 $request.AutomaticDecompression=[Net.DecompressionMethods]::GZip -bor [Net.DecompressionMethods]::Deflate
 Write-Host 'Lendo e resumindo a lista. Nenhum arquivo M3U original sera salvo.'
 $response=$request.GetResponse()
 $reader=[IO.StreamReader]::new($response.GetResponseStream(),[Text.Encoding]::UTF8,$true,65536)
 $resumo=[M3uSafeScanner]::Read($reader,$segredos.ToArray())
 if($resumo.Total -eq 0){throw 'A resposta nao contem entradas M3U.'}
 $parent=Split-Path -Parent $Arquivo
 if($parent){New-Item -ItemType Directory -Force -Path $parent | Out-Null}
 $resumo.Grupos | Sort-Object Categoria,Caminho,Extensao | Export-Csv -LiteralPath $Arquivo -NoTypeInformation -Encoding UTF8
 Write-Host "Entradas lidas: $($resumo.Total). Leitura completa: $($resumo.Completo)."
 Write-Host "Resumo salvo: $Arquivo"
 Write-Host 'Envie somente esse CSV. A URL e as credenciais nao sao incluidas no resumo.'
} catch {
 Write-Host 'Nao foi possivel analisar a lista. Confira a URL e o acesso no computador. Nao envie suas credenciais novamente.'
 exit 1
} finally {
 if($reader){$reader.Dispose()};if($response){$response.Dispose()}
 $urlEmMemoria=$null;$segura=$null
}
