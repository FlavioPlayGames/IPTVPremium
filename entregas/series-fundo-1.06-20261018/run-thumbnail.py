from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/series-fundo';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium');cp=str(r/'tests/json.jar')+':'+str(next((r/'gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm').rglob('*.jar')))+':'+(r/'tests/xtream.classpath').read_text().strip()
(r/'tests/series-rapidas/Resolver.kt').write_text('package br.com.flaviotorres.iptvpremium.data.resolver\nimport br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource\nobject M3uXtreamResolver { fun resolve(source:PlaylistSource.M3u):PlaylistSource.XtreamCodes?=null }')
sources=[*[src/p for p in ['data/repository/SeriesEpisodesRepository.kt','data/repository/SeriesJsonRecovery.kt','data/repository/SeriesEpisodeDiskCache.kt','data/repository/SeriesEpisodesCodec.kt','data/repository/EpisodeThumbnailLoader.kt','data/remote/PlaylistHttpClient.kt','domain/model/PlaylistSource.kt']],*[r/'tests/temporadas'/p for p in ['Clock.kt','Log.kt','Movie.kt']],r/'tests/series-rapidas/Resolver.kt',t/'ThumbnailTest.kt'];jar=t/'foco-tests.jar'
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(p)for p in sources]],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
for name in ['br.com.flaviotorres.iptvpremium.data.repository.ThumbnailTestKt']:
 x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-Xmx192m','-cp',str(jar)+':'+cp,name],capture_output=True,text=True);print(x.stdout,x.stderr);(t/'thumbnail-results.txt').write_text(x.stdout+x.stderr);x.check_returncode()
