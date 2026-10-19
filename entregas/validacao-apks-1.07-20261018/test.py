from pathlib import Path
import json,tempfile,subprocess,os,hashlib
r=Path('/workspace/storage-toolchain');t=r/'tests/apk-validation-107';out=Path('/workspace/outputs');env=os.environ.copy();env.update(XDG_DATA_HOME=str(r/'xdg-data'),XDG_CONFIG_HOME=str(r/'xdg-config'),XDG_CACHE_HOME=str(r/'xdg-cache'))
helper=t/'ApkOutputs.ps1';boot=out/'Preparar-Validacao-APKs-1.07.ps1';source=boot.read_text(encoding='utf-8-sig');assert helper.read_text().strip() in source
runner=t/'Runner.ps1';runner.write_text('''param([string]$Helper,[string]$Folder,[string]$Abis)
$ErrorActionPreference='Stop'
. $Helper
$o=Get-IptvDebugApks -Folder $Folder
$p=Select-IptvEmulatorApk -Outputs $o -Abis ($Abis -split ',')
[pscustomobject]@{count=$o.Entries.Count;full=$o.FullSet;file=[System.IO.Path]::GetFileName($p)} | ConvertTo-Json -Compress
''')
def entry(abi=None):return dict(outputFile='app-'+(abi or 'universal')+'-debug.apk',versionCode=9,versionName='1.07',filters=[] if abi is None else [dict(filterType='ABI',value=abi)])
def run(entries,abis='x86_64,x86',appid='br.com.flaviotorres.iptvpremium',variant='debug',missing=False):
 with tempfile.TemporaryDirectory() as d:
  p=Path(d);(p/'output-metadata.json').write_text(json.dumps(dict(applicationId=appid,variantName=variant,elements=entries)))
  for e in entries:
   name=e['outputFile']
   if not missing and '/' not in name and '\\' not in name:(p/name).write_bytes(b'fixture')
  x=subprocess.run([str(r/'powershell/pwsh'),'-NoProfile','-File',str(runner),'-Helper',str(helper),'-Folder',d,'-Abis',abis],env=env,capture_output=True,text=True)
  return x
cases=[]
def good(label,entries,filename,full=False,abis='x86_64,x86'):
 x=run(entries,abis);assert x.returncode==0,x.stdout+x.stderr;j=json.loads(x.stdout);assert j['file']==filename and j['full']==full;cases.append('PASS '+label)
def bad(label,entries,**args):
 x=run(entries,**args);assert x.returncode!=0,x.stdout;cases.append('PASS '+label)
good('APK x86 unico do usuario aceito para emulador com suporte x86',[entry('x86')],'app-x86-debug.apk')
good('conjunto completo conserva escolha do universal',[entry()]+[entry(a) for a in ['armeabi-v7a','arm64-v8a','x86','x86_64']],'app-universal-debug.apk',True)
good('somente universal aceito',[entry()],'app-universal-debug.apk')
good('arquitetura preferida do emulador vence a alternativa',[entry('x86'),entry('x86_64')],'app-x86_64-debug.apk')
good('APK unico arm64 aceito para emulador arm64',[entry('arm64-v8a')],'app-arm64-v8a-debug.apk',abis='arm64-v8a')
bad('identificador incorreto bloqueado',[entry('x86')],appid='br.outro.app')
bad('release nao tratado como debug',[entry('x86')],variant='release')
e=entry('x86');e['versionCode']=8;bad('numero interno antigo bloqueado',[e])
e=entry('x86');e['versionName']='1.06';bad('nome de versao antigo bloqueado',[e])
bad('APK ausente bloqueado',[entry('x86')],missing=True)
bad('arquitetura incompativel bloqueada',[entry('arm64-v8a')])
bad('arquiteturas duplicadas bloqueadas',[entry('x86'),entry('x86')])
e=entry('x86');e['outputFile']='../app-x86-debug.apk';bad('caminho externo bloqueado',[e])
e=entry('x86');e['filters'][0]['value']='x86_64';bad('arquivo diferente do filtro bloqueado',[e])
bad('metadados sem APK bloqueados',[])
a=boot.read_text(encoding='utf-8-sig');b=(out/'Preparar-Foco-Entrada-1.07.ps1').read_text(encoding='utf-8-sig')
def payload(s):return s.split("$payload = @'\n",1)[1].split("\n'@",1)[0]
assert payload(a)==payload(b);cases.append('PASS payload de fontes 1.07 identico sem alteracao no aplicativo')
assert a.count("Classe = 'CatalogNavigationDeviceTest'")==1 and 'Quantidade = 9' in a
cases.append('PASS nove testes nativos conservados sem duplicacao da suite')
(t/'results.txt').write_text('\n'.join(cases)+'\n');print('\n'.join(cases))
