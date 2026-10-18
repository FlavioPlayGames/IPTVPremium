from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/series-artes-fallback';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium/data/remote/TmdbHttpClient.kt');cp=(r/'tests/xtream.classpath').read_text().strip();jar=t/'transport.jar';java=str(r/'jdk-25.0.4.1/bin/java')
p=subprocess.run([java,'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),str(src),str(r/'tests/temporadas/Log.kt'),str(t/'TransportTest.kt')],capture_output=True,text=True)
if p.returncode:print(p.stderr);p.check_returncode()
p=subprocess.run([java,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.remote.TransportTestKt'],capture_output=True,text=True);(t/'transport-results.txt').write_text(p.stdout+p.stderr);print(p.stdout,p.stderr);p.check_returncode()
