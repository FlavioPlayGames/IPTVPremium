from pathlib import Path
import subprocess
w=Path('/workspace/continuar-notas-sinopse');r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player');tc=Path('/workspace/storage-toolchain')
fixture=w/'PressVerify.kt';fixture.write_text('''package br.com.flaviotorres.iptvpremium.core.player
internal const val PLAYER_FAVORITE_HOLD_MS=2_000L
fun main(){
 val p=PlayerCatalogPress()
 check(p.down(0));check(!p.down(100));check(p.up(200,false)==PlayerPressResult.CLICK);check(p.up(201,false)==PlayerPressResult.NONE)
 p.down(1000);check(p.up(4000,false)==PlayerPressResult.CLICK)
 p.down(5000);p.cancel();check(p.up(5100,false)==PlayerPressResult.NONE)
 p.down(6000);check(p.up(6100,false,true)==PlayerPressResult.NONE)
 println("PASS synopsis confirmation: one action on key-up, repeats ignored, long press without favorite, focus cancellation and canceled key")
}
''')
cp=':'.join(str(p)for p in(tc/'gradle-9.6.0/lib').glob('*.jar'));java=str(tc/'jdk-25.0.4.1/bin/java');jar=w/'press.jar'
subprocess.run([java,'-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),str(fixture),str(r/'PlayerCatalogPress.kt')],capture_output=True,text=True,check=True)
x=subprocess.run([java,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.core.player.PressVerifyKt'],capture_output=True,text=True,check=True);(w/'press-tests.txt').write_text(x.stdout);print(x.stdout)
