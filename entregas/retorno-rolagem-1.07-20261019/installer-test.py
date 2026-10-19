from pathlib import Path
import tempfile,subprocess,os,json
r=Path('/workspace/movie-loading-107/IPTVPremium');b=Path('/workspace/retorno-rolagem-107-before');o=Path('/workspace/outputs/retorno-rolagem-1.07');entries=json.loads((o/'manifest.json').read_text());script=o/'Preparar-Retorno-Rolagem-1.07.ps1';env=dict(os.environ,HOME='/workspace/storage-toolchain',XDG_CACHE_HOME='/workspace/storage-toolchain/cache',XDG_CONFIG_HOME='/workspace/storage-toolchain/config',XDG_DATA_HOME='/workspace/storage-toolchain/data');results=[]
def run(root,flag,ok=True,ps=script):
 x=subprocess.run(['/workspace/storage-toolchain/powershell/pwsh','-NoProfile','-File',str(ps),'-Projeto',str(root),flag],capture_output=True,text=True,env=env);assert (x.returncode==0)==ok,x.stdout+x.stderr
with tempfile.TemporaryDirectory(dir='/workspace')as d:
 root=Path(d);(root/'gradlew.bat').touch();(root/'local.properties').write_text('preserve');(root/'history.json').write_text('preserve history')
 def before():
  for e in entries:
   p=root/e['path'];p.parent.mkdir(parents=True,exist_ok=True)
   if e['allowMissing']:
    if p.exists():p.unlink()
   else:p.write_bytes((b/e['path']).read_bytes())
 before();run(root,'-Verificar');assert all((root/e['path']).exists()!=e['allowMissing']for e in entries);results.append('PASS verification accepts two absent new files without creating them')
 run(root,'-SomenteAplicar');assert all((root/e['path']).read_bytes()==(r/e['path']).read_bytes()for e in entries);results.append('PASS applies exact five files')
 assert (root/'local.properties').read_text()=='preserve'and(root/'history.json').read_text()=='preserve history';results.append('PASS preserves configuration and history')
 saved=list((root/'.navigation-backups').glob('*'))[0];assert all((saved/e['path']).exists()!=e['allowMissing']for e in entries);results.append('PASS backup records originally present files')
 run(root,'-SomenteAplicar');results.append('PASS idempotent reapplication')
 bad=root/entries[-1]['path'];bad.write_text('user changes');original={e['path']:(root/e['path']).read_bytes()for e in entries};run(root,'-SomenteAplicar',ok=False);assert all((root/e['path']).read_bytes()==original[e['path']]for e in entries);results.append('PASS divergent source rejected before any mutation')
 before();s=script.read_text(encoding='utf-8-sig');needle='[IO.File]::WriteAllBytes($target, [Convert]::FromBase64String($entry.data))';assert needle in s;s=s.replace(needle,needle+"\n        if ($entry.allowMissing) { throw 'injected failure after new file' }");broken=root/'broken.ps1';broken.write_text(s,encoding='utf-8-sig');run(root,'-SomenteAplicar',ok=False,ps=broken)
 assert all((not(root/e['path']).exists())if e['allowMissing']else(root/e['path']).read_bytes()==(b/e['path']).read_bytes()for e in entries);results.append('PASS rollback restores old files and removes only newly created targets')
(o/'testes-instalador.txt').write_text('\n'.join(results)+'\n');print('\n'.join(results))
