from pathlib import Path
import subprocess,json,os,uuid,hashlib
w=Path('/workspace/kids-historico-isolado');base=w/'installer-tests'/uuid.uuid4().hex;pkg=Path('/workspace/outputs/kids-historico-1.13');entries=json.loads((pkg/'manifest.json').read_text())
env=dict(os.environ,XDG_DATA_HOME='/workspace/storage-toolchain/xdg-data',XDG_CONFIG_HOME='/workspace/storage-toolchain/xdg-config',XDG_CACHE_HOME='/workspace/storage-toolchain/xdg-cache');pw='/workspace/storage-toolchain/powershell/pwsh';output=[]
def run(name,root):return subprocess.run([pw,'-NoProfile','-File',str(pkg/name),'-Projeto',str(root)],capture_output=True,text=True,env=env)
def sha(p):return hashlib.sha256(p.read_text(encoding='utf-8-sig').replace('\r\n','\n').encode()).hexdigest()
failures=['changed','corrupt','unknown-version','newer-version']
for case in ['normal','high-version-code',*failures,'undo-conflict','undo-gradle-conflict']:
 root=base/case;root.mkdir(parents=True);(root/'gradlew.bat').write_text('fixture');original={}
 for e in entries:
  before=w/('before-'+Path(e['path']).name)
  if before.exists():
   p=root/e['path'];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(before.read_bytes());original[e['path']]=p.read_bytes()
 gradle=root/'app/build.gradle.kts';gradle.parent.mkdir(parents=True,exist_ok=True)
 gradle.write_text('plugins { /* fixture */ }\n        versionCode = maxOf(14, localProperties.getProperty("APP_VERSION_CODE", "14").toInt()).also { require(it >= 2) }\n        versionName = "1.12"\nval privateSetting = "keep-this-local-value"\n')
 if case=='unknown-version':gradle.write_text(gradle.read_text().replace('maxOf(14,','unsupported(14,'))
 if case=='newer-version':gradle.write_text(gradle.read_text().replace('"1.12"','"1.14"'))
 original['app/build.gradle.kts']=gradle.read_bytes()
 if case=='high-version-code':
  props=root/'local.properties';props.write_text(' APP_VERSION_CODE = 120\n');original['local.properties']=props.read_bytes()
 if case=='changed':
  key=next(e['path']for e in entries if e['path']in original);p=root/key;p.write_bytes(p.read_bytes()+b'// local edit\n');original[key]=p.read_bytes()
 asset=pkg/entries[0]['path'];saved=asset.read_bytes()
 if case=='corrupt':asset.write_bytes(saved+b'broken')
 try:result=run('Aplicar.ps1',root)
 finally:asset.write_bytes(saved)
 assert (result.returncode==0)==(case not in failures),(case,result.stdout,result.stderr)
 assert 'keep-this-local-value' not in result.stdout+result.stderr
 if case in failures:
  assert all((root/k).read_bytes()==value for k,value in original.items())
  assert not(root/'.kids-historico-1.13-backups').exists()
  assert all(not(root/e['path']).exists()for e in entries if e['path']not in original)
 else:
  assert all(sha(root/e['path'])==e['sha']for e in entries)
  version=gradle.read_text();assert 'versionName = "1.13"'in version and 'keep-this-local-value'in version
  assert ('maxOf(121,'if case=='high-version-code'else'maxOf(15,')in version
  stable=gradle.read_bytes();result=run('Aplicar.ps1',root);assert result.returncode==0,(case,result.stdout,result.stderr)
  assert gradle.read_bytes()==stable
  assert len(list((root/'.kids-historico-1.13-backups').iterdir()))==1
  if case in ['undo-conflict','undo-gradle-conflict']:
   p=gradle if case=='undo-gradle-conflict'else root/entries[0]['path'];installed=p.read_bytes();p.write_text('new work')
   snapshot={str(p.relative_to(root)):p.read_bytes()for p in root.rglob('*')if p.is_file()}
   assert run('Desfazer.ps1',root).returncode!=0
   assert all((root/k).read_bytes()==v for k,v in snapshot.items());p.write_bytes(installed)
  result=run('Desfazer.ps1',root);assert result.returncode==0,(case,result.stdout,result.stderr)
  assert all((root/k).read_bytes()==v for k,v in original.items())
  assert all(not(root/e['path']).exists()for e in entries if e['path']not in original)
 output.append('PASS installer '+case)
for script in pkg.glob('*.ps1'):
 check=base/'parse.ps1';check.write_text("$tokens=$null; $errors=$null; [void][System.Management.Automation.Language.Parser]::ParseFile($args[0],[ref]$tokens,[ref]$errors); if($errors.Count){$errors | Out-String | Write-Host; exit 1}")
 result=subprocess.run([pw,'-NoProfile','-File',str(check),str(script)],capture_output=True,text=True,env=env);assert result.returncode==0,(script,result.stdout,result.stderr)
output.append('PASS PowerShell syntax, including version patch and TV installer')
(w/'installer-results.txt').write_text('\n'.join(output)+'\n');print('\n'.join(output))
