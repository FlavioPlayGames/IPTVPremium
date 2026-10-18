from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/fluidez-layout';cp=(r/'tests/xtream.classpath').read_text().strip();java=str(r/'jdk-25.0.4.1/bin/java');jar=t/'paging.jar'
x=subprocess.run([java,'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),str(t/'PagingBudgetTest.kt')],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([java,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.repository.PagingBudgetTestKt'],capture_output=True,text=True);print(x.stdout,x.stderr);(t/'paging-results.txt').write_text(x.stdout+x.stderr);raise SystemExit(x.returncode)
