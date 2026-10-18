package br.com.flaviotorres.iptvpremium.data.repository
import java.nio.ByteBuffer
fun main() {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 fun plane(bytes:ByteArray,row:Int,pixel:Int=1)=YuvPreviewPixels.Plane(ByteBuffer.wrap(bytes),row,pixel)
 val neutral=plane(byteArrayOf(128.toByte()),1)
 val black=YuvPreviewPixels.argb(plane(ByteArray(4){16},2),neutral,neutral,0,0,2,2,2,2)
 pass("YUV preto gera RGB preto opaco",black.all{it==0xff000000.toInt()})
 val white=YuvPreviewPixels.argb(plane(ByteArray(4){235.toByte()},2),neutral,neutral,0,0,2,2,1,1)
 pass("YUV branco gera RGB branco",white.single()==0xffffffff.toInt())
 val red=YuvPreviewPixels.argb(plane(ByteArray(4){81},2),plane(byteArrayOf(90),1),plane(byteArrayOf(240.toByte()),1),0,0,2,2,1,1).single()
 pass("canais U V nao estao trocados",((red shr 16)and 255)>250 && (red and 255)<5)
 val y=ByteArray(24){16};y[10]=235.toByte();y[11]=235.toByte();y[16]=235.toByte();y[17]=235.toByte()
 val uv=plane(ByteArray(12){128.toByte()},6,2)
 val cropped=YuvPreviewPixels.argb(plane(y,6),uv,uv,4,1,2,2,2,2)
 pass("respeita crop e padding de linha",cropped.all{it==0xffffffff.toInt()})
 val pos=ByteBuffer.wrap(byteArrayOf(0,235.toByte(),235.toByte(),235.toByte(),235.toByte()));pos.position(1)
 pass("respeita posicao inicial do buffer",YuvPreviewPixels.argb(YuvPreviewPixels.Plane(pos,2,1),neutral,neutral,0,0,2,2,1,1).single()==0xffffffff.toInt())
 pass("rejeita bitmap maior que limite de miniatura",runCatching{YuvPreviewPixels.argb(plane(y,6),uv,uv,0,0,2,2,641,360)}.isFailure)
}
