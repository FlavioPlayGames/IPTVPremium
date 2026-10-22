package br.com.flaviotorres.iptvpremium.data.repository

import br.com.flaviotorres.iptvpremium.data.remote.PlaylistAccessDeniedException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class XtreamAuthenticationRecoveryDeviceTest {
    private val active = """{"user_info":{"auth":1,"status":"Active"}}"""
    private suspend fun confirm(load: suspend () -> String) = XtreamAuthenticationRecovery.confirm(load, XtreamAuthenticationRecovery::validateResponse)
    @Test fun validResponseIsNotRequestedTwice() = runBlocking {
        var calls = 0; confirm { calls++; active }; assertEquals(1, calls)
    }
    @Test fun bomAndWhitespaceAreAccepted() = runBlocking {
        confirm { " \n\uFEFF$active" }
        confirm { """{"user_info":{"auth":null,"status":"Active"}}""" }
    }
    @Test fun malformedFirstResponseRecoversAutomatically() = runBlocking {
        var calls = 0; confirm { if (++calls == 1) "<html>temporary</html>" else active }; assertEquals(2, calls)
    }
    @Test fun repeatedMalformedResponseStopsAtTwoAttempts() = runBlocking {
        var calls = 0
        val error = runCatching { confirm { calls++; "{" } }.exceptionOrNull()
        assertTrue(error is XtreamAuthenticationResponseException); assertEquals(2, calls)
    }
    @Test fun explicitDenialDoesNotRetryEvenIfStatusSaysActive() = runBlocking {
        var calls = 0
        val error = runCatching { confirm { calls++; """{"user_info":{"auth":0,"status":"Active"}}""" } }.exceptionOrNull()
        assertTrue(error is PlaylistAccessDeniedException); assertEquals(1, calls)
    }
    @Test fun expiredAccountDoesNotRetry() = runBlocking {
        var calls = 0
        val error = runCatching { confirm { calls++; """{"user_info":{"auth":1,"status":"Expired"}}""" } }.exceptionOrNull()
        assertTrue(error is PlaylistAccessDeniedException); assertEquals(1, calls)
    }
    @Test fun unrelatedNetworkFailureIsNotRetried() = runBlocking {
        var calls = 0
        val error = runCatching { confirm { calls++; throw java.io.IOException("fixture") } }.exceptionOrNull()
        assertTrue(error is java.io.IOException); assertEquals(1, calls)
    }
    @Test fun cancellationDuringRetryDelayPreventsAnotherRequest() = runBlocking {
        var calls = 0
        val task = launch(start = CoroutineStart.UNDISPATCHED) { confirm { calls++; "{}" } }
        task.cancelAndJoin(); assertEquals(1, calls)
    }
    @Test fun emptyAccountResponseIsRetriedAsUnconfirmed() = runBlocking {
        var calls = 0; confirm { if (++calls == 1) """{"user_info":{}}""" else active }; assertEquals(2, calls)
    }
}
