package br.com.flaviotorres.iptvpremium.core.di

import android.content.Context
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import br.com.flaviotorres.iptvpremium.core.network.AppNetwork
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.catalog.NativeXtreamCatalog
import br.com.flaviotorres.iptvpremium.data.remote.*

/** Process-owned graph; survives activity recreation without duplicate importers or HTTP pools. */
internal object AppServices {
    @Volatile private var graph:KoinApplication?=null
    fun install(context:Context):org.koin.core.Koin=graph?.koin?:synchronized(this) {
        graph?.koin?:run {
            val app=context.applicationContext
            AppNetwork.initialize(app)
            koinApplication {
                androidContext(app)
                modules(module {
                    single<PlaylistHttpClient>{DefaultPlaylistHttpClient()}
                    single{PlaylistCacheDatabase.getInstance(app)}
                    single{get<PlaylistCacheDatabase>().playlistCacheDao()}
                    single{NativeXtreamCatalog.getInstance(get(),get())}
                    single{br.com.flaviotorres.iptvpremium.core.images.AppImages(app)}
                })
            }.also{graph=it}.koin
        }
    }
    inline fun <reified T:Any> get(context:Context):T=install(context).get()
}
