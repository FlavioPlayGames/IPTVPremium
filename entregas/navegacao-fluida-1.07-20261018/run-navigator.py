from pathlib import Path
import subprocess
r=Path('/workspace/storage-toolchain');t=r/'tests/navegacao-fluida-107';b=Path('/workspace/storage-work/app/src/main/java/br/com/flaviotorres/iptvpremium')
s=(b/'ui/components/CatalogGridNavigation.kt').read_text();a=s.index('internal class CatalogGridNavigator');z=s.index('/** Parent remains composed',a)
(t/'NavigatorUnderTest.kt').write_text('package br.com.flaviotorres.iptvpremium.ui.components\nimport androidx.compose.foundation.lazy.grid.LazyGridState\nimport androidx.compose.runtime.*\nimport androidx.compose.ui.focus.FocusRequester\nimport androidx.compose.ui.input.key.*\nimport br.com.flaviotorres.iptvpremium.data.cache.CatalogVerticalIntent\nimport kotlinx.coroutines.*\nimport kotlinx.coroutines.flow.*\n'+s[a:z])
cp=(r/'tests/xtream.classpath').read_text().strip();jar=t/'navigator.jar';files=list(t.glob('*.kt'))+[b/'data/cache/CatalogInteractionPolicy.kt']
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-cp',str(r/'gradle-9.6.0/lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*[str(p)for p in files]],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([str(r/'jdk-25.0.4.1/bin/java'),'-Xmx192m','-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.ui.components.NavigatorTestKt'],capture_output=True,text=True);(t/'navigator-results.txt').write_text(x.stdout+x.stderr);print(x.stdout,x.stderr);raise SystemExit(x.returncode)
