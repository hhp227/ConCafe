package com.hhp227.concafe.data.source.cache

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteMemoryCacheTest {
    @Test
    fun cacheFirstMemoizesRemoteValue() = runBlocking {
        val cache = DefaultRemoteMemoryCache()
        var loadCount = 0

        val first = cache.cacheFirst("favorites") { loadCount++; listOf("cafe-1") }
        val second = cache.cacheFirst("favorites") { loadCount++; listOf("cafe-2") }

        assertEquals(listOf("cafe-1"), first)
        assertEquals(listOf("cafe-1"), second)
        assertEquals(1, loadCount)
    }

    @Test
    fun cacheFirstDoesNotMemoizeFallbackProducedByCancelledLoad() = runBlocking {
        val cache = DefaultRemoteMemoryCache()
        val load = launch(start = CoroutineStart.UNDISPATCHED) {
            cache.cacheFirst("favorites") {
                // Mirrors the remote data sources: runCatching swallows the CancellationException
                // raised mid-request and the loader degrades to an empty result.
                runCatching { awaitCancellation() }
                emptyList<String>()
            }
        }

        load.cancel()
        load.join()

        assertNull(cache.get("favorites"))
    }
}
