from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/historico-experiencia';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium/data/history');cp=str(r/'tests/json.jar')+':'+(r/'tests/xtream.classpath').read_text().strip();jar=t/'shared-tests.jar'
sources=list((t/'stubs').glob('*.kt'))+list(src.glob('*.kt'))+[t/'SharedLibraryTest.kt']
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(p)for p in sources]],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-Xmx192m','-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.history.SharedLibraryTestKt'],capture_output=True,text=True);print(x.stdout,x.stderr);(t/'results.txt').write_text(x.stdout+x.stderr);x.check_returncode()
