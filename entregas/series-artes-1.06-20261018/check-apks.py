from pathlib import Path
import json,subprocess,os,xml.etree.ElementTree as ET
r=Path('/workspace/storage-toolchain');sdk=r/'android-sdk';folder=Path('/workspace/storage-work/app/build/outputs/apk/debug');m=json.loads((folder/'output-metadata.json').read_text());assert len(m['elements'])==5
env=os.environ.copy();env['JAVA_HOME']=str(r/'jdk-25.0.4.1');lines=[]
for e in m['elements']:
 assert e['versionCode']==8 and e['versionName']=='1.06'
 p=folder/e['outputFile'];x=subprocess.run([str(sdk/'build-tools/37.0.0/aapt'),'dump','badging',str(p)],capture_output=True,text=True);x.check_returncode();assert "sdkVersion:'28'" in x.stdout and "targetSdkVersion:'37'" in x.stdout
 x=subprocess.run([str(sdk/'build-tools/37.0.0/apksigner'),'verify','--verbose','--min-sdk-version','28',str(p)],env=env,capture_output=True,text=True);assert x.returncode==0,x.stdout+x.stderr
 lines.append('PASS '+p.name+' minSdk28 targetSdk37 1.06/code8 assinatura valida para Android9')
report=Path('/workspace/storage-work/app/build/reports/lint-results-debug.xml');issues=ET.parse(report).getroot().findall('issue');errors=[i for i in issues if i.attrib.get('severity')in('Error','Fatal')];apis=[i for i in issues if i.attrib.get('id')in('NewApi','InlinedApi')];assert not errors,[(i.attrib['id'],i.attrib.get('message'))for i in errors];assert not apis
lines.append('PASS lint sem erros e sem ocorrencias NewApi/InlinedApi com minSdk28')
p=Path('/workspace/storage-work/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
assert p.exists()
x=subprocess.run([str(sdk/'build-tools/37.0.0/apksigner'),'verify','--verbose','--min-sdk-version','28',str(p)],env=env,capture_output=True,text=True);assert x.returncode==0,x.stdout+x.stderr
lines.append('PASS APK de testes nativos compilado e assinado; execucao em dispositivo nao realizada aqui')
print('\n'.join(lines));(r/'tests/series-artes-fallback/apk-results.txt').write_text('\n'.join(lines)+'\n')

import zipfile
for path in [*folder.glob('*.apk'),Path('/workspace/storage-work/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')]:
 with zipfile.ZipFile(path) as archive:
  assert not any(n.endswith(('frame-test.mp4','frame-test.mkv')) for n in archive.namelist())
  for name in archive.namelist():
   if name.endswith('.dex'):
    dex=archive.read(name)
    assert all(token not in dex for token in [b'EpisodeFrameLoader',b'Media3EpisodeFrameDecoder',b'BoundedVideoReader',b'YuvPreviewPixels',b'EpisodeFrameDecoderDeviceTest',b'SeriesArtworkPolicyKt'])
print('PASS extrator antigo e fixtures de video ausentes dos cinco APKs e APK de testes')
with (r/'tests/series-artes-fallback/apk-results.txt').open('a') as f:f.write('PASS extrator antigo e fixtures removidos de todos os APKs\n')
