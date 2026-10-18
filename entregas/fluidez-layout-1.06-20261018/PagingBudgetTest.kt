package br.com.flaviotorres.iptvpremium.data.repository
import androidx.paging.PagingConfig
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import java.lang.reflect.Proxy
fun main() {
 val dao=Proxy.newProxyInstance(PlaylistCacheDao::class.java.classLoader,arrayOf(PlaylistCacheDao::class.java)){_,_,_->error("A configuracao nao deve executar consultas") } as PlaylistCacheDao
 val repository=PlaylistCatalogRepository(dao)
 val method=PlaylistCatalogRepository::class.java.getDeclaredMethod("pagingConfig",Boolean::class.javaPrimitiveType).apply{isAccessible=true}
 val grid=method.invoke(repository,true) as PagingConfig
 check(grid.maxSize in 60..240 && grid.initialLoadSize in 15..30 && grid.prefetchDistance in 5..20)
 println("PASS configuracao de producao limita janela e carga inicial da grade")
 check(grid.enablePlaceholders)
 println("PASS descarte de paginas preserva indices globais com placeholders")
 val channels=method.invoke(repository,false) as PagingConfig
 check(!channels.enablePlaceholders)
 println("PASS alteracao de grade nao introduz placeholders sem tamanho nos canais")
}
