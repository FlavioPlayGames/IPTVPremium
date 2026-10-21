package br.com.flaviotorres.iptvpremium.data.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class CatalogReuseStorageDeviceTest {
    private val source=PlaylistSource.XtreamCodes("fixture","Fixture",true,"https://example.invalid","fixture","fixture")
    private fun fixture(test:suspend (PlaylistStorage)->Unit)=runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val file=File(context.cacheDir,"reuse-${UUID.randomUUID()}.preferences_pb")
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val data=PreferenceDataStoreFactory.create(scope=scope,produceFile={file})
        try { test(PlaylistStorage(data)) } finally { scope.coroutineContext[Job]!!.cancelAndJoin();file.delete() }
    }
    private suspend fun installed(storage:PlaylistStorage, value:PlaylistSource=source) {
        storage.applyPortalOperation(value.id,value)
        val request=storage.portalLoadRequest.first()!!
        storage.markPortalCatalogReady(request);storage.finishPortalLoad(request)
    }
    @Test fun repeatedDeliveryAfterInstallDoesNotRequestImport()=fixture { storage->
        installed(storage);storage.applyPortalOperation(source.id,source)
        assertNull(storage.portalLoadRequest.first());assertEquals(source.id,storage.activePlaylistId.first())
    }
    @Test fun renameAndReenableReuseExistingCatalog()=fixture { storage->
        installed(storage)
        storage.applyPortalOperation(source.id,source.copy(name="Renamed",isEnabled=false))
        storage.applyPortalOperation(source.id,source.copy(name="Renamed",isEnabled=true))
        assertNull(storage.portalLoadRequest.first());assertEquals("Renamed",storage.playlists.first().single().name)
    }
    @Test fun changedCredentialsRequestImport()=fixture { storage->
        installed(storage);storage.applyPortalOperation(source.id,source.copy(password="changed"))
        assertNotNull(storage.portalLoadRequest.first())
    }
    @Test fun repeatedPendingDeliveryPreservesRequestAndCompletedMarker()=fixture { storage->
        storage.applyPortalOperation(source.id,source);val request=storage.portalLoadRequest.first()!!
        storage.markPortalCatalogReady(request)
        storage.applyPortalOperation(source.id,source.copy(name="Renamed"))
        assertEquals(request,storage.portalLoadRequest.first());assertTrue(storage.isPortalCatalogReady(request))
    }
    @Test fun changingActiveListToExistingM3uReusesCatalog()=fixture { storage->
        installed(storage)
        val m3u=PlaylistSource.M3u("other","M3U",true,"https://example.invalid/list.m3u")
        installed(storage,m3u);storage.saveActivePlaylistId(source.id)
        storage.applyPortalOperation(m3u.id,m3u.copy(name="Renamed"))
        assertEquals(m3u.id,storage.activePlaylistId.first());assertNull(storage.portalLoadRequest.first())
        storage.applyPortalOperation(m3u.id,m3u.copy(playlistUrl="https://other.invalid/list.m3u"))
        assertNotNull(storage.portalLoadRequest.first())
    }
    @Test fun connectionEditedWhileDisabledImportsWhenReenabled()=fixture { storage->
        installed(storage)
        val changed=source.copy(isEnabled=false,password="changed")
        storage.applyPortalOperation(source.id,changed)
        assertNull(storage.portalLoadRequest.first())
        storage.applyPortalOperation(source.id,changed.copy(isEnabled=true))
        assertNotNull(storage.portalLoadRequest.first())
    }

}
