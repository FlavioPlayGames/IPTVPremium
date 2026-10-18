from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/fluidez-layout';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium/data/cache/CatalogInteractionPolicy.kt');cp=str(next((r/'gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm').rglob('*.jar')))+':'+(r/'tests/xtream.classpath').read_text().strip();jar=t/'interaction.jar';java=str(r/'jdk-25.0.4.1/bin/java')
x=subprocess.run([java,'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),str(src),str(t/'InteractionTest.kt')],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([java,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.cache.InteractionTestKt'],capture_output=True,text=True);print(x.stdout,x.stderr);(t/'interaction-results.txt').write_text(x.stdout+x.stderr);raise SystemExit(x.returncode)
