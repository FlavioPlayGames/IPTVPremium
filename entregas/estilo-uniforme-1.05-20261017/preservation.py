from pathlib import Path
import hashlib,json,re
from PIL import Image
r=Path('/workspace/storage-work');b=Path('/workspace/estilo-uniforme-before/java');j=r/'app/src/main/java';lines=[]
def passed(s):lines.append('PASS '+s)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
for p in b.rglob('*.kt'):
 rel=p.relative_to(b).as_posix()
 if any('/'+x+'/' in '/'+rel for x in ['data','domain','repository']) or p.name in ['AppUpdateManager.kt','DevicePortalSync.kt']:
  assert p.read_bytes()==(j/rel).read_bytes(),rel
passed('data/import/storage/favorites/history/update policy and sync logic byte-identical')
for n in ['SeriesLoadingOverlay.kt','SeriesOpeningGeometry.kt']:
 p=next(j.rglob(n));assert p.read_bytes()==(b/p.relative_to(j)).read_bytes()
passed('series loading animation and TV geometry byte-identical')
for n in ['AppNavigation.kt','AppKeyboard.kt','HomeMenuCard.kt']:
 p=next(j.rglob(n));a=p.read_text();o=(b/p.relative_to(j)).read_text()
 pattern=r'(?:tween|animateFloat|animateFloatAsState|animateDpAsState|infiniteRepeatable)\([^)]*\)'
 assert re.findall(pattern,a)==re.findall(pattern,o),n
passed('movie loading, Home focus movement and keyboard animation timing parameters preserved')
res=r/'app/src/main/res';hashes=json.loads(Path('/workspace/refinamento-tv-before/res-hashes.json').read_text());changed=[n for n,h in hashes.items() if sha(res/n)!=h]
assert len(changed)==6 and all(re.fullmatch(r'drawable-nodpi/theme_(blue|purple|red|pink|emerald|cyan)_logo.webp',n) for n in changed)
for n in changed:
 im=Image.open(res/n);assert im.size==(1536,1024) and im.mode=='RGBA';assert im.getchannel('A').getextrema()[0]==0;assert all(im.getpixel(x)[3]==0 for x in [(0,0),(1535,0),(0,1023),(1535,1023)])
passed('only six additional-theme logos replaced; Gold, icons, movie art and loading resources unchanged; native size/alpha retained')
meta=json.loads((r/'app/build/outputs/apk/debug/output-metadata.json').read_text());assert len(meta['elements'])==5 and all(e['versionCode']==7 and e['versionName']=='1.05' for e in meta['elements'])
passed('all five APK architectures carry 1.05/code7')
print('\n'.join(lines));Path('/workspace/storage-toolchain/tests/uniforme/preservation-results.txt').write_text('\n'.join(lines)+'\n')
