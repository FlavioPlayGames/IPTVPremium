package br.com.flaviotorres.iptvpremium.core.session

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.AtomicFile
import br.com.flaviotorres.iptvpremium.BuildConfig
import java.io.File

/** App-private, excluded from backups; only Android saved UI state, not a second catalog. */
internal class ActivitySessionStore(context:Context) {
    private val file=AtomicFile(File(context.noBackupFilesDir,"activity-session-v1.bin"))
    private val loader=context.classLoader
    fun clear()=file.delete()
    fun read():Bundle? {
        val parcel=Parcel.obtain()
        return try {
            val bytes=file.openRead().use{input->
                if(input.channel.size()>SessionSnapshotCodec.MAX_BYTES+20L)return null
                input.readBytes()
            }
            val payload=SessionSnapshotCodec.decode(bytes,BuildConfig.VERSION_CODE)?:return null
            parcel.unmarshall(payload,0,payload.size);parcel.setDataPosition(0)
            parcel.readBundle(loader)?.apply{classLoader=loader}
        } catch(_:Exception){clear();null} finally{parcel.recycle()}
    }
    fun save(state:Bundle) {
        val parcel=Parcel.obtain()
        try {
            parcel.writeBundle(state)
            val payload=SessionSnapshotCodec.encode(parcel.marshall(),BuildConfig.VERSION_CODE)
            val output=file.startWrite()
            try{output.write(payload);file.finishWrite(output)}catch(error:Exception){file.failWrite(output);throw error}
        } catch(_:Exception){android.util.Log.w("ActivitySession","Sessão não pôde ser gravada")} finally{parcel.recycle()}
    }
}
