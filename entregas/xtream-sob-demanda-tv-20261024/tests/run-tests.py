from pathlib import Path
import subprocess
w=Path('/workspace/xtream-on-demand-fix');tc=Path('/workspace/storage-toolchain');classes='/workspace/restored-navigation-build/app/build/intermediates/built_in_kotlinc/desempenho/compileDesempenhoKotlin/classes'
cp=(tc/'tests/xtream.classpath').read_text().strip().replace('/workspace/storage-work/app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes',classes)
cp='/workspace/xtream-direct-fix/tests/android-classes:'+str(next((tc/'gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm/1.10.2').rglob('*.jar')))+':'+cp
java=str(tc/'jdk-25.0.4.1/bin/java');results=[]
for name,src in [
 ('data.sync.OnDemandTestKt',[w/'OnDemandTest.kt']),
 ('data.sync.ViewModelTestKt',[w/'ViewModelTest.kt',w/'RollbackDatabase.kt']),
 ('data.sync.FullIntegrationKt',[w/'FullIntegration.kt',w/'FullImportTestHarness.kt']),
 ('data.sync.PipelineTestKt',[Path('/workspace/xtream-read-ahead-fix/PipelineTest.kt')]),
 ('data.parser.RepositoryTestKt',[Path('/workspace/xtream-read-ahead-fix/RepositoryTest.kt')])]:
 jar=w/(name.split('.')[-1]+'.jar')
 r=subprocess.run([java,'-cp',str(tc/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-Xfriend-paths='+classes,'-classpath',cp,'-d',str(jar),*map(str,src)],capture_output=True,text=True)
 if r.returncode:print(r.stderr);raise SystemExit(r.returncode)
 r=subprocess.run([java,'-Xmx256m','-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.'+name],capture_output=True,text=True,timeout=120)
 print(r.stdout,r.stderr,flush=True);results.append(r.stdout+r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
(w/'test-results.txt').write_text('\n'.join(results))
