from pathlib import Path
import subprocess,json,os,uuid,hashlib
w=Path('/workspace/retomada-video');base=w/'installer-tests'/uuid.uuid4().hex;pkg=Path('/workspace/outputs/retomada-video');manifest=json.loads((pkg/'manifest.json').read_text());source=Path('/workspace/historico-regras-20261102/java/br/com/flaviotorres/iptvpremium');prefix=Path('app/src/main/java/br/com/flaviotorres/iptvpremium')
env=dict(os.environ,XDG_DATA_HOME='/workspace/storage-toolchain/xdg-data',XDG_CONFIG_HOME='/workspace/storage-toolchain/xdg-config',XDG_CACHE_HOME='/workspace/storage-toolchain/xdg-cache');pw='/workspace/storage-toolchain/powershell/pwsh';output=[]
def run(script,root):return subprocess.run([pw,'-NoProfile','-File',str(pkg/script),'-Projeto',str(root)],capture_output=True,text=True,env=env)
def sha(p):return hashlib.sha256(p.read_text(encoding='utf-8-sig').replace('\r\n','\n').encode()).hexdigest()
for case in ['normal','changed','corrupt','undo-conflict']:
 root=base/case;root.mkdir(parents=True);(root/'gradlew.bat').write_text('fixture');original={}
 for e in manifest:
  src=w/'before-PlaybackScreen.kt'
  if not src.exists() or (case=='already-cleaned' and e['remove']):continue
  p=root/e['path'];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(src.read_bytes());original[e['path']]=p.read_bytes()
 if case in ['changed','changed-manifest']:
  key='app/src/main/AndroidManifest.xml' if case=='changed-manifest' else next(e['path'] for e in manifest if not e['remove'] and e['path'] in original);p=root/key;p.write_bytes(p.read_bytes()+b'// local change\n');original[key]=p.read_bytes()
 asset=pkg/next(e['path'] for e in manifest if not e['remove']);saved=asset.read_bytes()
 if case=='corrupt':asset.write_bytes(saved+b'bad')
 try:result=run('Aplicar.ps1',root)
 finally:asset.write_bytes(saved)
 assert (result.returncode==0)==(case not in ['changed','changed-manifest','corrupt']),(case,result.stdout,result.stderr)
 if case in ['changed','changed-manifest','corrupt']:
  assert all((root/p).read_bytes()==data for p,data in original.items());assert not(root/'.retomada-video-backups').exists()
 else:
  assert all(not(root/e['path']).exists() if e['remove'] else sha(root/e['path'])==e['sha'] for e in manifest)
  assert run('Aplicar.ps1',root).returncode==0
  assert len(list((root/'.retomada-video-backups').iterdir()))==1
  if case=='undo-conflict':
   e=next(e for e in manifest if not e['remove']);p=root/e['path'];installed=p.read_bytes();p.write_text('new work')
   before={str(p.relative_to(root)):p.read_bytes() for p in root.rglob('*') if p.is_file()}
   assert run('Desfazer.ps1',root).returncode!=0
   assert all((root/p).read_bytes()==data for p,data in before.items());p.write_bytes(installed)
  result=run('Desfazer.ps1',root);assert result.returncode==0,(result.stdout,result.stderr)
  assert all((root/p).read_bytes()==data for p,data in original.items())
  assert all(not(root/e['path']).exists() for e in manifest if e['path'] not in original)
 output.append('PASS installer '+case)
for script in pkg.glob('*.ps1'):
 check=base/'parse.ps1';check.write_text("$tokens=$null; $errors=$null; [void][System.Management.Automation.Language.Parser]::ParseFile($args[0],[ref]$tokens,[ref]$errors); if($errors.Count){$errors | Out-String | Write-Host; exit 1}")
 result=subprocess.run([pw,'-NoProfile','-File',str(check),str(script)],capture_output=True,text=True,env=env);assert result.returncode==0,(script,result.stdout,result.stderr)
output.append('PASS PowerShell syntax, including TV installer')
(w/'installer-results.txt').write_text('\n'.join(output)+'\n');print('\n'.join(output))
