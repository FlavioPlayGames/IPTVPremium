from pathlib import Path
import subprocess,shutil,json,os,uuid
w=Path('/workspace/xtream-on-demand-fix');pkg=Path('/workspace/outputs/xtream-sob-demanda-tv');manifest=json.loads((pkg/'manifest.json').read_text());rel=Path('app/src/main/java/br/com/flaviotorres/iptvpremium')
base=w/'installer-test'/uuid.uuid4().hex
env=dict(os.environ,XDG_DATA_HOME='/workspace/storage-toolchain/xdg-data',XDG_CONFIG_HOME='/workspace/storage-toolchain/xdg-config',XDG_CACHE_HOME='/workspace/storage-toolchain/xdg-cache');pw='/workspace/storage-toolchain/powershell/pwsh'
def run(script,root):return subprocess.run([pw,'-NoProfile','-File',str(pkg/script),'-Projeto',str(root)],capture_output=True,text=True,env=env)
for changed in [False,True]:
 root=base/str(changed);root.mkdir(parents=True,exist_ok=True);(root/'gradlew.bat').write_text('fixture')
 original={}
 for e in manifest:
  p=root/e['path'];src=w/'before'/Path(e['path']).relative_to(rel)
  if src.exists():p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(src.read_bytes());original[e['path']]=p.read_bytes()
 if changed:
  p=root/next(e['path'] for e in manifest if not e['allowMissing']);p.write_text(p.read_text()+'// user change\n');original[str(p.relative_to(root))]=p.read_bytes()
 r=run('Aplicar.ps1',root);assert (r.returncode==0)!=changed,(r.stdout,r.stderr)
 if changed:
  assert all((root/k).read_bytes()==v for k,v in original.items());print('PASS installer rejects modified source before mutation');continue
 assert run('Aplicar.ps1',root).returncode==0
 assert len(list((root/'.xtream-sob-demanda-backups').iterdir()))==1
 # Corrupt bridge must abort rollback without restoring any files.
 bridge=pkg/'rollback/PlaylistCacheDatabase.kt';saved=bridge.read_bytes();bridge.write_bytes(saved+b'// corruption')
 try:
  r=run('Desfazer.ps1',root);assert r.returncode!=0
  assert all((root/e['path']).read_bytes()==(pkg/e['path']).read_bytes() for e in manifest)
 finally:bridge.write_bytes(saved)
 r=run('Desfazer.ps1',root);assert r.returncode==0,(r.stdout,r.stderr)
 for e in manifest:
  p=root/e['path']
  if e['allowMissing']:assert not p.exists()
  else:assert p.read_bytes()==(saved if p.name=='PlaylistCacheDatabase.kt' else original[e['path']])
 assert run('Aplicar.ps1',root).returncode==0
 print('PASS install/idempotency, corrupted rollback blocked, safe rollback, removal of helper, reapply')
