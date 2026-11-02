from pathlib import Path
import subprocess
w=Path('/workspace/historico-unificado');tc=Path('/workspace/storage-toolchain');r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium')
cp=':'.join(str(p)for p in(tc/'gradle-9.6.0/lib').glob('*.jar'))+':'+str(tc/'tests/json.jar');j=str(tc/'jdk-25.0.4.1/bin/java');jar=w/'tests.jar'
files=list((w/'tests').glob('*.kt'))+list((r/'data/history').glob('*.kt'))+[r/'domain/model/FavoriteScope.kt']
x=subprocess.run([j,'-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*map(str,files)],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([j,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.history.VerifyKt'],capture_output=True,text=True);print(x.stdout+x.stderr);(w/'test-results.txt').write_text(x.stdout+x.stderr);raise SystemExit(x.returncode)
