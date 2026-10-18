from pathlib import Path
import subprocess,os
r=Path('/workspace/storage-toolchain');t=r/'tests/espanhol-abertura';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium');cp=(r/'tests/xtream.classpath').read_text().strip();jar=t/'identity.jar';java=str(r/'jdk-25.0.4.1/bin/java')
sources=[src/'data/repository/SeriesArtworkIdentityResolver.kt',src/'data/repository/SeriesArtworkFallback.kt',src/'data/remote/TmdbHttpClient.kt',r/'tests/temporadas/Log.kt',t/'IdentityTest.kt',t/'LiveIdentityTest.kt',src/'data/repository/MovieFanartArtwork.kt',t/'MovieIdentityTest.kt',t/'LiveMovieTest.kt',t/'SpanishTest.kt',src/'ui/movies/MovieOpeningPolicy.kt']
p=subprocess.run([java,'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(x)for x in sources]],capture_output=True,text=True)
if p.returncode:print(p.stderr);raise SystemExit(p.returncode)
for name,argv,net in [('spanish',['br.com.flaviotorres.iptvpremium.data.repository.SpanishTestKt'],False),('movie',['br.com.flaviotorres.iptvpremium.data.repository.MovieIdentityTestKt'],False),('movie-live',['br.com.flaviotorres.iptvpremium.data.repository.LiveMovieTestKt'],True),('identity',['br.com.flaviotorres.iptvpremium.data.repository.IdentityTestKt',str(t)],False),('live',['br.com.flaviotorres.iptvpremium.data.repository.LiveIdentityTestKt'],True)]:
 args=[java]
 if net:args+=['-Dhttps.proxyHost=proxy','-Dhttps.proxyPort=8080','-Djavax.net.ssl.trustStore=/workspace/storage-toolchain/cacerts','-Djavax.net.ssl.trustStorePassword=changeit']
 p=subprocess.run([*args,'-cp',str(jar)+':'+cp,*argv],capture_output=True,text=True);(t/(name+'-results.txt')).write_text(p.stdout+p.stderr);print(p.stdout)
 if p.returncode:print(p.stderr[:2000]);raise SystemExit(p.returncode)
