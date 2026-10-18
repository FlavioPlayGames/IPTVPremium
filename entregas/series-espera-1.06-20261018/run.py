from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/series-espera';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium/ui/series/SeriesOpeningTiming.kt')
cp=str(next((r/'gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm').rglob('*.jar')))+':'+(r/'tests/xtream.classpath').read_text().strip();jar=t/'timing.jar'
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),str(src),str(t/'TimingTest.kt')],capture_output=True,text=True)
if x.returncode:print(x.stderr);x.check_returncode()
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.ui.series.TimingTestKt'],capture_output=True,text=True);(t/'timing-results.txt').write_text(x.stdout+x.stderr);print(x.stdout,x.stderr);x.check_returncode()
