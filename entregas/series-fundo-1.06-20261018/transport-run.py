from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/series-causa';src=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium');basecp=(r/'tests/xtream.classpath').read_text().strip()
def run(label,sources,cp,main):
 jar=t/(label+'.jar')
 x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(p) for p in sources]],capture_output=True,text=True)
 if x.returncode: print(x.stderr);x.check_returncode()
 x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(jar)+':'+cp,main],capture_output=True,text=True)
 print(x.stdout,x.stderr);(r/'tests/series-fundo'/(label+'-results.txt')).write_text(x.stdout+x.stderr);x.check_returncode()
run('transport-http',[src/'data/remote/PlaylistHttpClient.kt',src/'data/remote/DefaultPlaylistHttpClient.kt',r/'tests/series-rapidas/HttpLog.kt',t/'RegressionHttp.kt'],basecp,'br.com.flaviotorres.iptvpremium.data.remote.RegressionHttpKt')
cp=str(r/'tests/json.jar')+':'+str(next((r/'gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm').rglob('*.jar')))+':'+basecp
sources=[*[src/p for p in ['data/repository/SeriesEpisodesRepository.kt','data/repository/SeriesJsonRecovery.kt','data/repository/SeriesEpisodeDiskCache.kt','data/repository/SeriesEpisodesCodec.kt','data/cache/CatalogDetailsWarmupCache.kt','data/cache/SeriesFocusPreloader.kt','data/remote/PlaylistHttpClient.kt','domain/model/PlaylistSource.kt']],r/'tests/temporadas/Clock.kt',r/'tests/temporadas/Movie.kt',r/'tests/series-rapidas/HttpLog.kt',r/'tests/series-rapidas/Resolver.kt',t/'RegressionCache.kt',t/'TransportFocusTest.kt',t/'RetryPolicyTest.kt']
jar=t/'transport-policy.jar'
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(p)for p in sources]],capture_output=True,text=True)
if x.returncode: print(x.stderr);x.check_returncode()
for label,main in [('transport-cache','br.com.flaviotorres.iptvpremium.data.cache.RegressionCacheKt'),('transport-focus','br.com.flaviotorres.iptvpremium.data.cache.TransportFocusTestKt'),('retry-policy','br.com.flaviotorres.iptvpremium.data.repository.RetryPolicyTestKt')]:
 x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(jar)+':'+cp,main],capture_output=True,text=True);print(x.stdout,x.stderr);(r/'tests/series-fundo'/(label+'-results.txt')).write_text(x.stdout+x.stderr);x.check_returncode()
