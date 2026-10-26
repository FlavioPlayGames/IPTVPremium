package br.com.flaviotorres.iptvpremium.data.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidM3uTransportStore(context: Context) : M3uTransportStore {
    private val preferences = context.applicationContext.getSharedPreferences("m3u_transport_v1", Context.MODE_PRIVATE)
    override suspend fun read(key: String): String? = withContext(Dispatchers.IO) { preferences.getString(key, null) }
    override suspend fun write(key: String, value: String) = withContext(Dispatchers.IO) {
        check(preferences.edit().putString(key, value).commit()) { "Não foi possível salvar o transporte da lista." }
    }
}
