from pathlib import Path
import subprocess,json,os,uuid,shutil,hashlib
w=Path('/workspace/legendas-duas-etapas');pkg=Path('/workspace/outputs/legendas-duas-etapas');base=w/'site-installer-tests'/uuid.uuid4().hex;manifest=json.loads((pkg/'site/alteracoes.json').read_text());env=dict(os.environ,XDG_DATA_HOME='/workspace/storage-toolchain/xdg-data',XDG_CONFIG_HOME='/workspace/storage-toolchain/xdg-config',XDG_CACHE_HOME='/workspace/storage-toolchain/xdg-cache');pw='/workspace/storage-toolchain/powershell/pwsh';results=[]
def run(script,root):return subprocess.run([pw,'-NoProfile','-File',str(pkg/script),'-Site',str(root)],capture_output=True,text=True,env=env)
for case in ['normal','custom-unrelated','unknown-fragment','corrupt','undo-conflict']:
 root=base/case;root.mkdir(parents=True)
 for e in manifest['patches']:
  p=root/e['path'];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((w/'site-before'/e['path']).read_bytes())
 (root/'wrangler.jsonc').write_text('{"main":"worker-atualizacao-abi.js","private-test":"keep-this-configuration"}')
 if case=='custom-unrelated':
  p=root/'worker-atualizacao-abi.js';p.write_text(p.read_text()+'\n// CUSTOM BUSINESS LOGIC OUTSIDE PATCH\n')
 if case=='unknown-fragment':
  p=root/'worker-atualizacao-abi.js';p.write_text(p.read_text().replace('async function queue(env, id, kind, payload, requestId) {','async function customQueue(env, id, kind, payload, requestId) {'))
 original={str(p.relative_to(root)):p.read_bytes()for p in root.rglob('*')if p.is_file()};asset=pkg/'site/referencia/opensubtitles.js';saved=asset.read_bytes()
 if case=='corrupt':asset.write_bytes(saved+b'broken')
 try:r=run('Aplicar-Site.ps1',root)
 finally:asset.write_bytes(saved)
 assert (r.returncode==0)==(case not in ['unknown-fragment','corrupt']),(case,r.stdout,r.stderr)
 if case in ['unknown-fragment','corrupt']:
  assert all((root/k).read_bytes()==v for k,v in original.items());assert not(root/'.legendas-duas-etapas-site-backups').exists()
 else:
  assert (root/'wrangler.jsonc').read_bytes()==original['wrangler.jsonc']
  if case=='custom-unrelated':assert 'CUSTOM BUSINESS LOGIC OUTSIDE PATCH' in(root/'worker-atualizacao-abi.js').read_text()
  applied={str(p.relative_to(root)):p.read_bytes()for p in root.rglob('*')if p.is_file()and '.legendas-duas-etapas-site-backups' not in p.parts}
  r=run('Aplicar-Site.ps1',root);assert r.returncode==0,(case,r.stdout,r.stderr)
  assert all((root/k).read_bytes()==v for k,v in applied.items())
  assert len(list((root/'.legendas-duas-etapas-site-backups').iterdir()))==1
  if case=='undo-conflict':
   p=root/'public/subtitles.js';current=p.read_bytes();p.write_bytes(current+b'\n// local change\n');before=p.read_bytes()
   assert run('Desfazer-Site.ps1',root).returncode!=0;assert p.read_bytes()==before;p.write_bytes(current)
  r=run('Desfazer-Site.ps1',root);assert r.returncode==0,(case,r.stdout,r.stderr)
  assert all((root/k).read_bytes()==v for k,v in original.items())
  assert all(not(root/e['path']).exists()for e in manifest['newFiles'])
 results.append('PASS site installer '+case)
(w/'site-installer-results.txt').write_text('\n'.join(results)+'\n');print('\n'.join(results))
