from pathlib import Path
import tempfile,subprocess,json,hashlib,os
r=Path('/workspace/storage-toolchain');out=Path('/workspace/outputs');pkg=out/'series-causa-1.06';manifest=json.loads((pkg/'manifest.json').read_text());env=os.environ.copy();env.update(XDG_DATA_HOME=str(r/'xdg-data'),XDG_CONFIG_HOME=str(r/'xdg-config'),XDG_CACHE_HOME=str(r/'xdg-cache'));script=out/'Preparar-Series-Causa-1.06.ps1'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest() if p.exists() else None
def fixture(root,code=7):
 (root/'settings.gradle.kts').write_text('//fixture');(root/'gradlew.bat').write_text('@echo off')
 (root/'local.properties').write_bytes(b'# keep bytes\r\nsdk.dir=C\\:\\\\Android\\\\Sdk\r\nAPP_VERSION_CODE='+str(code).encode()+b'\r\nAPP_VERSION_NAME=1.05\r\ncustom.key=\xff\\unchanged\r\n')
 (root/'userdata-do-not-touch').write_text('favorites-history-fixture')
 for e in manifest['files']:
  source=Path('/workspace/series-causa-before')/e['file']
  if source.exists():p=root/e['file'];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(source.read_bytes())
def run(root,*args):return subprocess.run([str(r/'powershell/pwsh'),'-NoProfile','-File',str(script),'-Projeto',str(root),*args],env=env,capture_output=True,text=True)
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);old=(root/'local.properties').read_bytes();old_data=(root/'userdata-do-not-touch').read_bytes()
 x=run(root,'-Verificar');assert x.returncode==0,x.stdout+x.stderr
 assert (root/'local.properties').read_bytes()==old and not (root/'.iptv-backups').exists();print('PASS bootstrap verificar nao altera versao nem fontes')
 x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr
 for e in manifest['files']:assert sha(root/e['file'])==e['after']
 props=(root/'local.properties').read_bytes();assert props==old.replace(b'APP_VERSION_CODE=7',b'APP_VERSION_CODE=8').replace(b'APP_VERSION_NAME=1.05',b'APP_VERSION_NAME=1.06')
 assert (root/'userdata-do-not-touch').read_bytes()==old_data
 assert any(p.read_bytes()==old for p in (root/'.iptv-backups').rglob('local.properties'))
 print('PASS aplica dezoito arquivos e prepara 1.06 codigo8 preservando bytes privados e dados')
 backups=list((root/'.iptv-backups').iterdir());x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr;assert list((root/'.iptv-backups').iterdir())==backups;print('PASS repeticao idempotente sem backups adicionais')
 first=root/manifest['files'][0]['file'];first.write_text('usuario alterou este fonte');snapshot={e['file']:(root/e['file']).read_bytes()for e in manifest['files']}
 x=run(root,'-NaoCompilar');assert x.returncode!=0;assert all((root/k).read_bytes()==v for k,v in snapshot.items());assert (root/'local.properties').read_bytes()==props;print('PASS fonte desconhecido interrompe antes de alterar arquivos e versao')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root,9);old=(root/'local.properties').read_bytes();x=run(root,'-NaoCompilar');assert x.returncode!=0;assert (root/'local.properties').read_bytes()==old;assert all(sha(root/e['file']) == sha(Path('/workspace/series-causa-before')/e['file']) for e in manifest['files']);print('PASS recusa downgrade de numero interno')
# No runtime passwords, APKs or local build/signing configuration in the distribution.
import zipfile
with zipfile.ZipFile(out/'IPTVPremium-series-causa-1.06.zip') as z:
 assert all(not n.endswith(('.apk','.jks','.keystore')) and not n.endswith('local.properties') for n in z.namelist())
 print('PASS pacote nao inclui APKs ou configuracao de assinatura local')

# The uploaded src.zip has the older remote-only extractor; accept it without discarding other files.
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root)
 for e in manifest['files']:
  p=Path('/workspace/series-rapidas-uploaded')/e['file']
  if p.exists():target=root/e['file'];target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(p.read_bytes())
 x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr
 assert all(sha(root/e['file'])==e['after'] for e in manifest['files'])
 print('PASS base do src.zip aceita e atualizada sem exigir patch intermediario')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);props=(root/'local.properties').read_bytes()
 x=run(root,'-NaoCompilar','-TestarFrames');assert x.returncode!=0
 assert (root/'local.properties').read_bytes()==props and not (root/'.iptv-backups').exists()
 print('PASS teste nativo sem compilacao rejeitado antes de mutacoes')
 x=run(root,'-TestarFrames','-Emulador','phone-id');assert x.returncode!=0
 assert (root/'local.properties').read_bytes()==props and not (root/'.iptv-backups').exists()
 print('PASS instalacao de testes restrita explicitamente a emulador')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root)
 p=root/'app/build.gradle.kts';s=p.read_text(encoding='utf-8-sig').replace('\r\n','\n');p.write_bytes(b'\xef\xbb\xbf'+s.replace('\n','\r\n').encode())
 x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr
 print('PASS Gradle com BOM e finais de linha Windows aceito sem liberar alteracoes desconhecidas')

with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);props=(root/'local.properties').read_bytes()
 x=run(root,'-NaoCompilar','-TestarSeries');assert x.returncode!=0
 assert (root/'local.properties').read_bytes()==props and not (root/'.iptv-backups').exists()
 x=run(root,'-TestarSeries','-Emulador','phone-id');assert x.returncode!=0
 assert (root/'local.properties').read_bytes()==props and not (root/'.iptv-backups').exists()
 print('PASS novos testes series rejeitam uso sem build e alvo fora do emulador')
