from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/historico-experiencia';s=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium');cp=(r/'tests/xtream.classpath').read_text().strip();java=str(r/'jdk-25.0.4.1/bin/java');jar=t/'live.jar'
sources=[s/'data/history/ContentIdentity.kt',s/'data/repository/TitleAliasResolver.kt',s/'data/repository/SeriesArtworkIdentityResolver.kt',s/'data/repository/SeriesArtworkFallback.kt',s/'data/remote/TmdbHttpClient.kt',r/'tests/temporadas/Log.kt',t/'LiveSharedIdentity.kt']
x=subprocess.run([java,'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(p)for p in sources]],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([java,'-Dhttps.proxyHost=proxy','-Dhttps.proxyPort=8080','-Djavax.net.ssl.trustStore=/workspace/storage-toolchain/cacerts','-Djavax.net.ssl.trustStorePassword=changeit','-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.repository.LiveSharedIdentityKt'],capture_output=True,text=True);print(x.stdout,x.stderr);(t/'live-results.txt').write_text(x.stdout+x.stderr);raise SystemExit(x.returncode)
