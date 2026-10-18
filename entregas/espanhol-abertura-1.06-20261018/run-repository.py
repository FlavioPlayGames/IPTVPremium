from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/espanhol-abertura';classes='/workspace/storage-work/app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes';cp='/workspace/storage-work/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:'+(r/'tests/xtream.classpath').read_text().strip();jar=t/'repository.jar';java=str(r/'jdk-25.0.4.1/bin/java')
p=subprocess.run([java,'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-Xfriend-paths='+classes,'-classpath',cp,'-d',str(jar),str(t/'RepositoryLiveTest.kt')],capture_output=True,text=True)
if p.returncode:print(p.stderr);raise SystemExit(p.returncode)
p=subprocess.run([java,'-Dhttps.proxyHost=proxy','-Dhttps.proxyPort=8080','-Djavax.net.ssl.trustStore=/workspace/storage-toolchain/cacerts','-Djavax.net.ssl.trustStorePassword=changeit','-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.repository.RepositoryLiveTestKt'],capture_output=True,text=True);(t/'repository-results.txt').write_text(p.stdout+p.stderr);print(p.stdout,p.stderr[:2500]);raise SystemExit(p.returncode)
