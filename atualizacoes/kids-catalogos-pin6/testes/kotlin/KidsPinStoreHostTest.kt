package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35])
class KidsPinStoreHostTest {
    @Test fun legacyPinStillAuthenticatesAndNewPinRequiresExactlySix() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val salt=ByteArray(16){it.toByte()}
        fun hex(b:ByteArray)=b.joinToString(""){"%02x".format(it.toInt()and 255)}
        val hash=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec("1234".toCharArray(),salt,120000,256)).encoded
        File(context.noBackupFilesDir,"parental_pin_v1.json").apply {parentFile!!.mkdirs();writeText(JSONObject().put("salt",hex(salt)).put("hash",hex(hash)).toString())}
        val store=ParentalPinStore(context)
        assertTrue(store.verify("1234"))
        for(pin in listOf("12345","1234567")) {
            try {store.set(pin);fail("Invalid length accepted")} catch(_:IllegalArgumentException){}
        }
        store.set("654321");assertTrue(store.verify("654321"));assertFalse(store.verify("1234"))
    }
    @Test fun kidsRulePersistsAndCanBeDisabledInsideAuthorizedControls() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        ParentalPolicy.initialize(context)
        ParentalPolicy.update {it.copy(kidsOnly=true)}
        ParentalPolicy.initialize(context);assertTrue(ParentalPolicy.rules.value.kidsOnly)
        ParentalPolicy.update {it.copy(kidsOnly=false)}
        ParentalPolicy.initialize(context);assertFalse(ParentalPolicy.rules.value.kidsOnly)
    }
}
