from pathlib import Path
import tempfile,json,subprocess,os
r=Path('/workspace/storage-toolchain');out=Path('/workspace/outputs');pkg=out/'series-vinculos-1.06';manifest=json.loads((pkg/'manifest.json').read_text());script=out/'Preparar-Series-Vinculos-1.06.ps1'
with tempfile.TemporaryDirectory() as tmp:
 root=Path(tmp);project=root/'project';project.mkdir();sdk=root/'sdk';(sdk/'platform-tools').mkdir(parents=True);jbr=root/'jbr';(jbr/'bin').mkdir(parents=True);(jbr/'bin/java.exe').write_text('fixture')
 for e in manifest['files']:
  if e.get('delete'):continue
  dest=project/e['file'];dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes((pkg/'arquivos'/e['file']).read_bytes())
 (project/'settings.gradle.kts').write_text('//fixture');(project/'local.properties').write_text('APP_VERSION_CODE=8\nAPP_VERSION_NAME=1.06\n')
 (project/'userdata-do-not-touch').write_text('favoritos-progresso')
 folder=project/'app/build/outputs/apk/debug';folder.mkdir(parents=True)
 elements=[]
 for name in ['universal','arm64-v8a','armeabi-v7a','x86','x86_64']:
  file='app-'+name+'-debug.apk';(folder/file).write_text('fixture');elements.append(dict(versionCode=8,versionName='1.06',outputFile=file))
 (folder/'output-metadata.json').write_text(json.dumps(dict(applicationId='br.com.flaviotorres.iptvpremium',elements=elements)))
 gradle=project/'gradlew.bat';gradle.write_text('#!/bin/sh\nprintf "%s\\n" "$*" >> "$DIAG_CALLS"\n');gradle.chmod(0o755)
 adb=sdk/'platform-tools/adb.exe';adb.write_text('''#!/bin/sh
printf '%s\n' "$*" >> "$DIAG_CALLS"
case "$*" in
 *get-state*) echo device;;
 *date*) echo '10-18 16:00:00.000';;
 *logcat*) echo 'D/SharedLibrary: fixture logcat';;
esac
''');adb.chmod(0o755)
 calls=root/'calls.txt';env=os.environ.copy();env.update(JAVA_HOME=str(jbr),ANDROID_HOME=str(sdk),LOCALAPPDATA=str(root),DIAG_CALLS=str(calls),XDG_DATA_HOME=str(r/'xdg-data'),XDG_CONFIG_HOME=str(r/'xdg-config'),XDG_CACHE_HOME=str(r/'xdg-cache'))
 for mode,value in [('Pausados','true'),('Ativos','false')]:
  calls.write_text('')
  result=subprocess.run([str(r/'powershell/pwsh'),'-NoProfile','-File',str(script),'-Projeto',str(project),'-CompararVinculos','-ModoVinculos',mode],env=env,capture_output=True,text=True)
  assert result.returncode==0,result.stdout+result.stderr
  trace=calls.read_text();assert f'-PiptvDebugPauseSharedLibrary={value}' in trace,trace
  assert trace.count(' install -r ')==1 and 'app-universal-debug.apk' in trace
  assert 'force-stop' in trace and trace.index('force-stop')<trace.index('am start')<trace.index(' logcat ')
  assert ' clear' not in trace and 'uninstall' not in trace and 'instrument' not in trace
  assert (project/f'diagnostico-series-vinculos-{mode.lower()}-logcat.txt').exists()
  assert (project/'userdata-do-not-touch').read_text()=='favoritos-progresso'
  assert not (project/'gradle.properties').exists()
  print('PASS orquestracao '+mode+': propriedade correta, instala uma vez, reinicia processo, captura separada, sem apagar dados')
