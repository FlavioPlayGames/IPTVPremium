from pathlib import Path
import tempfile,subprocess,json,hashlib,os,zipfile
r=Path('/workspace/storage-toolchain');out=Path('/workspace/outputs');pkg=out/'historico-experiencia-1.06';manifest=json.loads((pkg/'manifest.json').read_text());script=out/'Preparar-Historico-Experiencia-1.06.ps1';before=Path('/workspace/historico-experiencia-before');env=os.environ.copy();env.update(XDG_DATA_HOME=str(r/'xdg-data'),XDG_CONFIG_HOME=str(r/'xdg-config'),XDG_CACHE_HOME=str(r/'xdg-cache'))
def content(p):return p.read_bytes() if p.is_file() else None
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest() if p.is_file() else None
def snapshot(root):return {e['file']:content(root/e['file']) for e in manifest['files']}
def fixture(root,code=7):
 (root/'settings.gradle.kts').write_text('//fixture');(root/'gradlew.bat').write_text('@echo off')
 (root/'local.properties').write_bytes(b'# preserve\r\nsdk.dir=C\\:\\\\Android\\\\Sdk\r\nAPP_VERSION_CODE='+str(code).encode()+b'\r\nAPP_VERSION_NAME=1.05\r\ncustom.key=\xff\\unchanged\r\n')
 (root/'userdata-do-not-touch').write_bytes(b'favorites-history-fixture')
 for e in manifest['files']:
  p=before/e['file']
  if p.is_file():q=root/e['file'];q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(p.read_bytes())
def run(root,*args):return subprocess.run([str(r/'powershell/pwsh'),'-NoProfile','-File',str(script),'-Projeto',str(root),*args],env=env,capture_output=True,text=True)
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);old=(root/'local.properties').read_bytes();snap=snapshot(root)
 x=run(root,'-Verificar');assert x.returncode==0,x.stdout+x.stderr;assert snapshot(root)==snap and content(root/'local.properties')==old and not (root/'.iptv-backups').exists();print('PASS verificar nao altera fontes exclusoes versao ou configuracao')
 x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr
 assert all(sha(root/e['file'])==e['after'] for e in manifest['files']);print('PASS aplica fontes e remove nove arquivos conhecidos de extracao')
 props=content(root/'local.properties');assert props==old.replace(b'APP_VERSION_CODE=7',b'APP_VERSION_CODE=8').replace(b'APP_VERSION_NAME=1.05',b'APP_VERSION_NAME=1.06');assert content(root/'userdata-do-not-touch')==b'favorites-history-fixture';assert any(content(p)==old for p in (root/'.iptv-backups').rglob('local.properties'));print('PASS preserva dados assinatura configuracao privada e bytes nao UTF8')
 deleted=[e for e in manifest['files'] if e.get('delete')];assert len(deleted)==9
 for e in deleted:
  if snap[e['file']] is not None:assert any(content(p)==snap[e['file']] for p in (root/'.iptv-backups').rglob(Path(e['file']).name))
 print('PASS arquivos removidos mantem backup integral')
 backups=list((root/'.iptv-backups').iterdir());x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr;assert list((root/'.iptv-backups').iterdir())==backups;print('PASS reaplicacao idempotente inclusive exclusoes')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);target=root/next(e['file'] for e in manifest['files'] if e.get('delete'));target.write_text('usuario alterou este extrator');snap=snapshot(root);props=content(root/'local.properties');x=run(root,'-NaoCompilar');assert x.returncode!=0;assert snapshot(root)==snap and content(root/'local.properties')==props and not (root/'.iptv-backups').exists();print('PASS extrator com alteracoes desconhecidas impede toda a aplicacao antes de mutacoes')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root,9);snap=snapshot(root);props=content(root/'local.properties');x=run(root,'-NaoCompilar');assert x.returncode!=0;assert snapshot(root)==snap and content(root/'local.properties')==props;print('PASS downgrade interno bloqueado antes de alterar fontes')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root)
 for e in manifest['files']:
  p=out/'series-causa-1.06/arquivos'/e['file']
  if p.is_file():q=root/e['file'];q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(p.read_bytes())
 x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr;assert all(sha(root/e['file'])==e['after'] for e in manifest['files']);print('PASS base r2 instalada aceita sem exigir diagnostico intermediario')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root)
 for e in manifest['files']:
  p=Path('/workspace/series-rapidas-uploaded')/e['file'];q=root/e['file']
  if p.is_file():q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(p.read_bytes())
 x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr;assert all(sha(root/e['file'])==e['after'] for e in manifest['files']);print('PASS src.zip original aceito para aplicacao cumulativa')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);snap=snapshot(root);props=content(root/'local.properties')
 for flag in ['-TestarSeries','-TestarApresentacao','-TestarLista','-CapturarLog']:
  x=run(root,'-NaoCompilar',flag);assert x.returncode!=0
  x=run(root,flag,'-Emulador','phone-id');assert x.returncode!=0
 assert snapshot(root)==snap and content(root/'local.properties')==props and not (root/'.iptv-backups').exists();print('PASS todos testes e captura exigem build e alvo emulador antes de mutacoes')
 x=run(root,'-Verificar','-CapturarLog');assert x.returncode==0,x.stdout+x.stderr;assert snapshot(root)==snap and not (root/'.iptv-backups').exists();print('PASS verificar com captura nao inicia testes nem modifica projeto')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);p=root/'app/build.gradle.kts';s=p.read_text(encoding='utf-8-sig').replace('\r\n','\n');p.write_bytes(b'\xef\xbb\xbf'+s.replace('\n','\r\n').encode());x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr;print('PASS Gradle BOM e finais de linha Windows conhecidos aceitos')
with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root);snap=snapshot(root)
 wrapper=root/'rollback.ps1';wrapper.write_text('''param([string]$Script,[string]$Project)
$ErrorActionPreference='Stop'
function Copy-Item {
 param([string]$LiteralPath,[string]$Destination,[switch]$Force)
 if ($Destination.EndsWith('SharedLibraryStore.kt') -and -not $Destination.Contains('.iptv-backups') -and -not $script:injected) { $script:injected = $true; throw 'falha de copia injetada' }
 Microsoft.PowerShell.Management\\Copy-Item -LiteralPath $LiteralPath -Destination $Destination -Force:$Force
}
& $Script -Projeto $Project
''')
 x=subprocess.run([str(r/'powershell/pwsh'),'-NoProfile','-File',str(wrapper),'-Script',str(pkg/'Aplicar.ps1'),'-Project',str(root)],env=env,capture_output=True,text=True)
 assert x.returncode!=0,x.stdout+x.stderr;assert 'anteriores restaurados' in x.stderr,x.stdout+x.stderr;assert snapshot(root)==snap;print('PASS rollback restaura arquivos copiados e todos arquivos excluidos apos falha injetada')
with zipfile.ZipFile(out/'IPTVPremium-historico-experiencia-1.06.zip') as z:
 assert all(not n.endswith(('.apk','.jks','.keystore')) and not n.endswith('local.properties') for n in z.namelist())
 secrets=json.loads(Path('/workspace/.private-provider/access.json').read_text())
 for n in z.namelist():
  data=z.read(n)
  assert all(secrets[k].encode() not in data for k in ['username','password'])
 print('PASS pacote sem credenciais fornecidas APKs ou configuracao privada de assinatura')

with tempfile.TemporaryDirectory() as d:
 root=Path(d);fixture(root)
 old=snapshot(root)
 x=run(root,'-NaoCompilar');assert x.returncode==0,x.stdout+x.stderr
 changed=[key for key in old if old[key]!=content(root/key)]
 assert len(changed)==5,changed
 print('PASS base anterior altera somente os cinco arquivos previstos para historico e experiencia')
