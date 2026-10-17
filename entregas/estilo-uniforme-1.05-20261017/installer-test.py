from pathlib import Path
import subprocess,os,hashlib,json,shutil,zipfile
out=Path('/workspace/outputs');t=Path('/workspace/storage-toolchain/tests/uniforme/fixtures');t.mkdir(exist_ok=True)
pkg=out/'estilo-uniforme-1.05';entries=json.loads((pkg/'manifest.json').read_text())['files'];before=Path('/workspace/estilo-uniforme-before/java');exe='/workspace/storage-toolchain/powershell/pwsh'
env=dict(os.environ,XDG_DATA_HOME='/workspace/storage-toolchain/xdg-data',XDG_CONFIG_HOME='/workspace/storage-toolchain/xdg-config',XDG_CACHE_HOME='/workspace/storage-toolchain/xdg-cache')
keep=b'sdk.dir=C\\:\\\\Android\\\\Sdk\r\nDUMMY_KEY=bytes-\xc3\xa7-\xff-$-keep\r\n';props=keep+b'APP_VERSION_CODE=6\r\nAPP_VERSION_NAME=1.04\r\n';lines=[]
def ok(s):lines.append('PASS '+s);print(lines[-1])
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest() if p.exists() else None
def fixture(name,variant=0):
 p=t/name
 if p.exists():shutil.rmtree(p)
 p.mkdir();(p/'settings.gradle.kts').write_text('// fixture');(p/'gradlew.bat').write_text('');(p/'local.properties').write_bytes(props);(p/'data-sentinel').write_text('favorite-history')
 for e in entries:
  rel=Path(e['file']);src=before/rel.relative_to('app/src/main/java') if '/java/' in str(rel) else Path('/workspace/estilo-uniforme-before/logos')/rel.name
  if rel.name=='PlaylistListScreen.kt' and variant:src=Path('/workspace/estilo-uniforme-before/PlaylistListScreen-2colunas.kt') if variant==1 else out/'refinamento-tv-1.04/arquivos/variantes/PlaylistListScreen-sem-lixeira.kt'
  assert src.exists(),src
  dest=p/rel;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest)
 return p

def run(p,verify=False):return subprocess.run([exe,'-NoProfile','-File',str(pkg/'Aplicar.ps1' if verify else out/'Preparar-estilo-uniforme-1.05.ps1'),'-Projeto',str(p),'-Verificar' if verify else '-NaoCompilar'],env=env,capture_output=True,text=True)
for v in [0,1,2]:
 p=fixture('variant-'+str(v),v);r=run(p);assert r.returncode==0,r.stderr
 for e in entries:
  choice=e if e['file'].split('/')[-1]!='PlaylistListScreen.kt' or not v else e['alternatives'][v-1]
  assert sha(p/e['file'])==choice['after'],e['file']
 assert (p/'local.properties').read_bytes()==keep+b'APP_VERSION_CODE=7\r\nAPP_VERSION_NAME=1.05\r\n';assert (p/'data-sentinel').read_text()=='favorite-history'
 saved=list((p/'.iptv-backups').glob('versao-app-*'));assert len(saved)==1 and (saved[0]/'local.properties').read_bytes()==props
 r=run(p);assert r.returncode==0,r.stderr;assert len(list((p/'.iptv-backups').glob('versao-app-*')))==1
 ok('playlist variant '+str(v)+' preserved; all targets/hashes checked; code7; byte-preserving backup; idempotence')
p=fixture('pre-104');old=Path('/workspace/refinamento-tv-before/java');ext=Path('/workspace/refinamento-tv-before/minhas-listas-externa');prior=json.loads((out/'refinamento-tv-1.04/manifest.json').read_text())['files']
for e in prior:
 rel=Path(e['file']);src=old/rel.relative_to('app/src/main/java')
 if (ext/rel).exists():src=ext/rel
 dest=p/rel
 if src.exists():shutil.copy2(src,dest)
 else:dest.unlink(missing_ok=True)
r=run(p);assert r.returncode==0,r.stderr;assert all(sha(p/e['file'])==e['after'] for e in entries);ok('known pre-1.04 sources receive approved refinements and uniform style together')
for kind in ['higher','invalid','mismatch','verify']:
 p=fixture(kind)
 if kind=='higher':(p/'local.properties').write_bytes(keep+b'APP_VERSION_CODE=8\n')
 if kind=='invalid':(p/'local.properties').write_bytes(keep+b'APP_VERSION_CODE=invalid\n')
 if kind=='mismatch':(p/entries[-1]['file']).write_text('local-edit-do-not-overwrite')
 hashes={e['file']:sha(p/e['file']) for e in entries};prop=(p/'local.properties').read_bytes();r=run(p,kind=='verify')
 assert (r.returncode==0)==(kind=='verify'),(kind,r.stdout,r.stderr)
 assert all(sha(p/f)==h for f,h in hashes.items()) and (p/'local.properties').read_bytes()==prop
 ok(kind+' does not modify sources or local properties')
for wrong in [False,True]:
 p=fixture('compile-'+str(wrong));fake='''#!/usr/bin/env python3
import pathlib,json
p=pathlib.Path(__file__).parent/'app/build/outputs/apk/debug';p.mkdir(parents=True,exist_ok=True)
elements=[]
for name in ['universal','armeabi-v7a','arm64-v8a','x86','x86_64']:
 f='app-'+name+'-debug.apk';(p/f).write_bytes(b'fixture');elements.append({'versionCode':CODE,'versionName':'1.05','outputFile':f})
(p/'output-metadata.json').write_text(json.dumps({'applicationId':'br.com.flaviotorres.iptvpremium','elements':elements}))
'''.replace('CODE','6' if wrong else '7');(p/'gradlew.bat').write_text(fake);(p/'gradlew.bat').chmod(0o755)
 (p/'run.ps1').write_text("$ErrorActionPreference='Stop'\nfunction Invoke-Item {param($Path)}\n& '/workspace/outputs/Preparar-estilo-uniforme-1.05.ps1' -Projeto $PSScriptRoot\n")
 r=subprocess.run([exe,'-NoProfile','-File',str(p/'run.ps1')],env=env,capture_output=True,text=True);assert (r.returncode!=0)==wrong,(r.stdout,r.stderr)
 ok('rejects unexpected APK version' if wrong else 'build workflow verifies all five APKs at 1.05/code7')
with zipfile.ZipFile(out/'IPTVPremium-estilo-uniforme-1.05.zip') as z:assert not any(n.endswith(('.apk','local.properties','google-services.json','.jks','.keystore')) for n in z.namelist())
ok('no APK/private configuration/signing keys in package')
Path('/workspace/storage-toolchain/tests/uniforme/installer-results.txt').write_text('\n'.join(lines)+'\n')
