package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Passwords never reach disk. Only the session is encrypted, outside Android backup. */
internal class OpenSubtitlesAccountStore(context:Context):OpenSubtitleAccounts {
    private val file=AtomicFile(File(context.applicationContext.noBackupFilesDir,"iptvpremium_open_session_v1.bin"))
    private val alias="iptvpremium_open_session_v1"
    companion object { private val lock=Any(); private var generation=0L }
    override fun generation():Long=synchronized(lock){generation}
    override fun read():OpenSubtitleSession?=synchronized(lock){runCatching {
        val pair=DataInputStream(file.openRead()).use { input->
            check(input.readInt()==1)
            val iv=ByteArray(12); input.readFully(iv)
            val length=input.readInt();check(length in 17..32768)
            val bytes=ByteArray(length);input.readFully(bytes);iv to bytes
        }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,pair.first))
        val json=JSONObject(String(cipher.doFinal(pair.second),Charsets.UTF_8))
        val token=json.getString("token");check(token.length in 8..16000 && token.none{it.isWhitespace()||it.isISOControl()})
        val base=openSubtitleApiBase(json.getString("host"));check(base!=null)
        OpenSubtitleSession(token,base,json.getString("scope"),json.getLong("expires"),json.getString("app"))
    }.getOrNull()}
    override fun save(token:String,base:String,expiresAt:Long,appFingerprint:String,expectedGeneration:Long,accountScope:String):Boolean=synchronized(lock){
        if(generation!=expectedGeneration)return@synchronized false
        runCatching {
            check(token.length in 8..16000 && token.none{it.isWhitespace()||it.isISOControl()})
            val host=java.net.URI(base).host;check(openSubtitleApiBase(host)==base)
            val json=JSONObject().put("token",token).put("host",host)
                .put("scope",accountScope).put("expires",expiresAt).put("app",appFingerprint)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
            val encrypted=cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
            val stream=file.startWrite()
            try {
                val output=DataOutputStream(stream);output.writeInt(1);output.write(cipher.iv)
                output.writeInt(encrypted.size);output.write(encrypted);output.flush();file.finishWrite(stream)
            }catch(error:Exception){file.failWrite(stream);throw error}
            generation++;SubtitleProviderUpdates.changed();true
        }.getOrDefault(false)
    }
    override fun disconnect(expectedToken:String?):Boolean=synchronized(lock){
        if(expectedToken!=null && read()?.token!=expectedToken)return@synchronized false
        generation++;file.delete();SubtitleProviderUpdates.changed();true
    }
    private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let{return it}
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setUserAuthenticationRequired(false).build())
        return generator.generateKey()
    }
}

/** Application registration is configured by the maintainer, never requested from viewers. */
internal fun openSubtitlesClient(context:Context):OpenSubtitlesClient {
    val app=context.applicationContext
    return OpenSubtitlesClient({OpenSubtitleAppConfig(
        app.getString(br.com.flaviotorres.iptvpremium.R.string.opensubtitles_api_key).trim(),
        app.getString(br.com.flaviotorres.iptvpremium.R.string.opensubtitles_user_agent).trim()
            .ifBlank{"IPTVPremium v1.13"})},OpenSubtitlesAccountStore(app))
}
