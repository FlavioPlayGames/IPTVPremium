from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/series-artes-rapidas';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium/data/parser/DefaultXtreamParser.kt');cp=(r/'tests/xtream.classpath').read_text().strip();jar=t/'import.jar';java=str(r/'jdk-25.0.4.1/bin/java');classes='/workspace/storage-work/app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes'
p=subprocess.run([java,'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-Xfriend-paths='+classes,'-classpath',cp,'-d',str(jar),str(t/'ImportArtworkTest.kt')],capture_output=True,text=True)
if p.returncode:print(p.stderr);raise SystemExit(p.returncode)
p=subprocess.run([java,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.parser.ImportArtworkTestKt'],capture_output=True,text=True);(t/'import-results.txt').write_text(p.stdout+p.stderr);print(p.stdout,p.stderr);raise SystemExit(p.returncode)
