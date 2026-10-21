package br.com.flaviotorres.iptvpremium.data.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class PosterResolutionHostTest {
    private fun image(width: Int, height: Int): File = File.createTempFile("poster-test", ".img").also { file ->
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
    @Test fun decodedLargeCachedPosterFitsExactlyWithoutCropping() {
        val file = image(1000, 1500)
        try { val bitmap = PosterResolution.decode(file)!!
            assertEquals(300, bitmap.width); assertEquals(450, bitmap.height); bitmap.recycle()
        } finally { file.delete() }
    }
    @Test fun newlyDownloadedPosterIsStoredAt300By450(): Unit = runBlocking {
        val file = image(780, 1170)
        try {
            assertTrue(PosterResolution.prepareDownloaded(file, 1_000_000))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            assertEquals(300, bounds.outWidth); assertEquals(450, bounds.outHeight)
        } finally { file.delete() }
    }
    @Test fun smallPosterRemainsUnchangedAndDoesNotUpscale(): Unit = runBlocking {
        val file = image(200, 300); val original = file.readBytes()
        try {
            assertTrue(PosterResolution.prepareDownloaded(file, 1_000_000))
            assertArrayEquals(original, file.readBytes())
            val bitmap = PosterResolution.decode(file)!!
            assertEquals(200, bitmap.width); assertEquals(300, bitmap.height); bitmap.recycle()
        } finally { file.delete() }
    }
    @Test fun nonStandardRatioIsPreserved() {
        val file = image(800, 800)
        try { val bitmap = PosterResolution.decode(file)!!
            assertEquals(300, bitmap.width); assertEquals(300, bitmap.height); bitmap.recycle()
        } finally { file.delete() }
    }
    @Test fun invalidImageIsRejected(): Unit = runBlocking {
        val file = File.createTempFile("poster-invalid", ".img").apply { writeText("not an image") }
        try { assertNull(PosterResolution.decode(file)); assertFalse(PosterResolution.prepareDownloaded(file, 1_000_000)) }
        finally { file.delete() }
    }
}
