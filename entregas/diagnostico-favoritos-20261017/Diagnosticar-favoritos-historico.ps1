param([string]$Serial, [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe", [string]$PastaDestino = (Get-Location).Path)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) { throw 'adb.exe nao encontrado. Informe -AdbPath.' }
if ([string]::IsNullOrWhiteSpace($Serial)) {
    $devices = @(& $AdbPath devices | ForEach-Object { if ($_ -match '^([^\s]+)\s+device$') { $matches[1] } })
    if ($devices.Count -ne 1) { throw 'Informe -Serial com o codigo do aparelho que deseja diagnosticar.' }
    $Serial = $devices[0]
}
if ($Serial -notmatch '^[A-Za-z0-9._:-]+$') { throw 'Serial invalido.' }
$package = 'br.com.flaviotorres.iptvpremium'
function Read-DeviceBytes([string]$relative) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $AdbPath
    $info.Arguments = '-s ' + $Serial + ' exec-out run-as ' + $package + ' cat ' + $relative
    $info.UseShellExecute = $false; $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true; $info.CreateNoWindow = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    $memory = New-Object System.IO.MemoryStream
    try {
        $null = $process.Start()
        $errors = $process.StandardError.ReadToEndAsync()
        $buffer = New-Object byte[] 8192
        while (($read = $process.StandardOutput.BaseStream.Read($buffer, 0, $buffer.Length)) -gt 0) {
            if ($memory.Length + $read -gt 8388608) { $process.Kill(); throw 'Arquivo de preferencias maior que o limite de diagnostico.' }
            $memory.Write($buffer, 0, $read)
        }
        $process.WaitForExit(); $null = $errors.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { return $null }
        return ,$memory.ToArray()
    } finally { $memory.Dispose(); $process.Dispose() }
}
function Read-PrivateXml([string]$name) {
    $bytes = Read-DeviceBytes ('shared_prefs/' + $name + '.xml')
    if ($null -eq $bytes) { return $null }
    $settings = New-Object System.Xml.XmlReaderSettings
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Prohibit
    $settings.XmlResolver = $null
    $stringReader = New-Object System.IO.StringReader ([System.Text.Encoding]::UTF8.GetString($bytes))
    $reader = [System.Xml.XmlReader]::Create($stringReader, $settings)
    try { $doc = New-Object System.Xml.XmlDocument; $doc.XmlResolver = $null; $doc.Load($reader); return ,$doc }
    finally { $reader.Dispose(); $stringReader.Dispose() }
}
Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Text;
public static class IPTVPreferenceMapReader {
    class Field { public int Tag; public byte[] Value; }
    static ulong Var(byte[] b, ref int p) {
        ulong v=0; for(int s=0;s<64;s+=7) { if(p>=b.Length)throw new FormatException("Preferencias truncadas"); byte c=b[p++];v|=(ulong)(c&127)<<s;if((c&128)==0)return v; }
        throw new FormatException("Varint invalido");
    }
    static List<Field> Fields(byte[] b) {
        var fields=new List<Field>();int p=0;
        while(p<b.Length){ulong key=Var(b,ref p);int tag=(int)(key>>3),wire=(int)(key&7);if(tag==0)throw new FormatException();
            if(wire==0){Var(b,ref p);continue;}
            if(wire==1){p+=8;}else if(wire==5){p+=4;}else if(wire==2){ulong n=Var(b,ref p);if(n>(ulong)(b.Length-p))throw new FormatException();byte[] v=new byte[(int)n];Buffer.BlockCopy(b,p,v,0,(int)n);p+=(int)n;fields.Add(new Field{Tag=tag,Value=v});}else throw new FormatException();
            if(p>b.Length)throw new FormatException();
        }return fields;
    }
    public static Dictionary<string,string> Read(byte[] b) {
        var result=new Dictionary<string,string>();
        foreach(var f in Fields(b))if(f.Tag==1){string key=null,value=null;foreach(var e in Fields(f.Value)){if(e.Tag==1)key=Encoding.UTF8.GetString(e.Value);if(e.Tag==2)foreach(var v in Fields(e.Value))if(v.Tag==5)value=Encoding.UTF8.GetString(v.Value);}if(key!=null&&value!=null)result[key]=value;}
        return result;
    }
}
'@
function Private-Id([string]$id) {
    if ([string]::IsNullOrEmpty($id)) { return 'empty' }
    $hash = [System.Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($hash.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($id))).Replace('-','').ToLowerInvariant()).Substring(0,12) }
    finally { $hash.Dispose() }
}
$report = New-Object 'System.Collections.Generic.List[string]'
$report.Add('DIAGNOSTICO SOMENTE LEITURA: favoritos e historico. IDs anonimizados; sem nomes, titulos, URLs ou credenciais.')
$registered = New-Object 'System.Collections.Generic.HashSet[string]'
$active = $null
$bytes = Read-DeviceBytes 'files/datastore/playlist_storage.preferences_pb'
if ($null -ne $bytes) {
    try {
        $map = [IPTVPreferenceMapReader]::Read($bytes)
        $lists = @(ConvertFrom-Json -InputObject $map['playlists'])
        foreach ($list in $lists) { if ($list.id) { $null = $registered.Add([string]$list.id) } }
        $active = $map['active_playlist_id']
        $report.Add(('registered_lists=' + $registered.Count + ' active_list_hash=' + (Private-Id $active)))
    } catch { $report.Add('registry=unreadable') }
} else { $report.Add('registry=unavailable_run_as_or_missing') }
$doc = Read-PrivateXml 'iptvpremium_favorites_v1'
if ($null -eq $doc) { $report.Add('favorites_file=missing_or_unavailable') }
else {
    $groups = @($doc.SelectNodes('/map/set'))
    $report.Add('favorites_groups=' + $groups.Count)
    foreach ($group in $groups) {
        $key = $group.GetAttribute('name')
        if ($key -match '^(movies|series)_(.*)$') {
            $type = $matches[1]; $id = $matches[2]
            $report.Add(('favorites_type=' + $type + ' list_hash=' + (Private-Id $id) + ' registered=' + $registered.Contains($id) + ' active=' + ($id -ceq $active) + ' count=' + @($group.SelectNodes('string')).Count))
        }
    }
}
$doc = Read-PrivateXml 'iptvpremium_watch_history_v1'
if ($null -eq $doc) { $report.Add('history_file=missing_or_unavailable') }
else {
    $node = $doc.SelectSingleNode('/map/string[@name="items"]')
    if ($null -eq $node) { $report.Add('history_items=0') }
    else {
        try {
            $items = @(ConvertFrom-Json -InputObject $node.InnerText)
            $report.Add('history_items=' + $items.Count)
            $groups = $items | Group-Object -Property playlistId, type
            foreach ($group in $groups) {
                $entry = $group.Group[0]; $id = [string]$entry.playlistId; $type = [string]$entry.type
                if ($type -notin @('MOVIE','EPISODE','LIVE_CHANNEL')) { $type='unknown' }
                $report.Add(('history_type=' + $type + ' list_hash=' + (Private-Id $id) + ' registered=' + $registered.Contains($id) + ' active=' + ($id -ceq $active) + ' count=' + $group.Count))
            }
        } catch { $report.Add('history_items=unreadable') }
    }
    foreach ($type in @('MOVIE','EPISODE')) {
        $node = $doc.SelectSingleNode('/map/boolean[@name="legacy_cleared_' + $type + '"]')
        if ($null -ne $node) { $value=$node.GetAttribute('value'); if($value -in @('true','false')){$report.Add('explicit_clear_' + $type + '=' + $value)} }
    }
}
foreach ($name in @('iptvpremium_movies_assistir_v1','iptvpremium_series_assistir_v1')) {
    $doc = Read-PrivateXml $name
    $label = if ($name -match 'movies') { 'movies' } else { 'series' }
    if ($null -eq $doc) { $report.Add('legacy_' + $label + '=missing_or_unavailable') }
    else { $report.Add('legacy_' + $label + '_keys=' + @($doc.SelectNodes('/map/*')).Count) }
}
$info = @(& $AdbPath -s $Serial shell dumpsys package $package)
foreach ($line in $info) {
    if ($line -match '^\s*(versionCode=\d+\b|versionName=|firstInstallTime=|lastUpdateTime=|primaryCpuAbi=|secondaryCpuAbi=)') { $report.Add($line.Trim()) }
}
$path = Join-Path $PastaDestino ('favoritos-historico-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.txt')
$report | Set-Content -LiteralPath $path -Encoding UTF8
$report | ForEach-Object { Write-Host $_ }
Write-Host ('Relatorio salvo: ' + $path)
