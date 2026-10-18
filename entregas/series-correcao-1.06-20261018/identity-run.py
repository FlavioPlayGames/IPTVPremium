from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/series-causa';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium')
cp=str(r/'tests/json.jar')+':'+(r/'tests/xtream.classpath').read_text().strip()
sources=[*[src/p for p in ['data/repository/SeriesEpisodesRepository.kt','data/repository/SeriesEpisodeDiskCache.kt','data/repository/SeriesEpisodesCodec.kt','data/remote/PlaylistHttpClient.kt','domain/model/PlaylistSource.kt']],r/'tests/series-rapidas/Resolver.kt',t/'identity-parser-shapes.kt'];jar=t/'identity-parser.jar'
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(p)for p in sources]],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-Xmx192m','-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.data.repository.Identity_parser_shapesKt'],capture_output=True,text=True);print(x.stdout,x.stderr);(t/'identity-results.txt').write_text(x.stdout+x.stderr);x.check_returncode()
