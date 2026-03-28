package com.hhp227.concafe.data.source.firestore

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CachedFirestoreRestApi(
    private val delegate: FirestoreRestApi,
    private val cacheStore: FirestoreRestCacheStore
) : FirestoreRestApi {
    private fun resolvePolicy(path: String): CachePolicy {
        val isQueryRequest = path.contains(":runQuery")
        return if (isQueryRequest) QUERY_POLICY else DOCUMENT_POLICY
    }

    private fun readCacheEntry(key: String): CacheEntry? {
        val rawPayload = cacheStore.load(key)
        val cachedAtRaw = cacheStore.load(resolveTimestampKey(key))
        val decodedLegacyEnvelope = if (rawPayload == null) {
            null
        } else {
            runCatching {
                Json.decodeFromString<CacheEnvelope>(rawPayload)
            }.getOrNull()
        }
        return if (decodedLegacyEnvelope != null) {
            val normalizedEntry = CacheEntry(
                payload = decodedLegacyEnvelope.payload,
                cachedAtEpochMillis = decodedLegacyEnvelope.cachedAtEpochMillis,
                isLegacyPayload = false
            )
            writeCacheEntry(
                key = key,
                payload = normalizedEntry.payload,
                cachedAtEpochMillis = normalizedEntry.cachedAtEpochMillis
            )
            normalizedEntry
        } else if (rawPayload != null && cachedAtRaw != null) {
            CacheEntry(
                payload = rawPayload,
                cachedAtEpochMillis = cachedAtRaw.toLongOrNull() ?: 0L,
                isLegacyPayload = false
            )
        } else if (rawPayload != null) {
            CacheEntry(
                payload = rawPayload,
                cachedAtEpochMillis = 0L,
                isLegacyPayload = true
            )
        } else {
            null
        }
    }

    private fun writeCacheEntry(
        key: String,
        payload: String,
        cachedAtEpochMillis: Long
    ) {
        cacheStore.save(key, payload)
        cacheStore.save(resolveTimestampKey(key), cachedAtEpochMillis.toString())
    }

    private fun resolveTimestampKey(key: String): String {
        return "$key|meta|cachedAt"
    }

    private fun isFresh(entry: CacheEntry, policy: CachePolicy, nowEpochMillis: Long): Boolean {
        val ageMillis = nowEpochMillis - entry.cachedAtEpochMillis
        val normalizedAgeMillis = if (ageMillis < 0L) 0L else ageMillis
        return normalizedAgeMillis <= policy.softTtlMillis
    }

    private fun canUseStale(entry: CacheEntry, policy: CachePolicy, nowEpochMillis: Long): Boolean {
        val ageMillis = nowEpochMillis - entry.cachedAtEpochMillis
        val normalizedAgeMillis = if (ageMillis < 0L) 0L else ageMillis
        return if (entry.isLegacyPayload) {
            true
        } else {
            normalizedAgeMillis <= policy.maxStaleMillis
        }
    }

    private suspend fun executeGetWithPolicy(path: String, idToken: String?): String {
        val cacheKey = buildGetCacheKey(path)
        val policy = resolvePolicy(path)
        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
        val cachedEntry = readCacheEntry(cacheKey)
        return if (cachedEntry != null && isFresh(cachedEntry, policy, nowEpochMillis)) {
            cachedEntry.payload
        } else {
            val networkResult = runCatching {
                delegate.get(path, idToken)
            }
            val networkPayload = networkResult.getOrNull()

            if (networkPayload != null) {
                writeCacheEntry(
                    key = cacheKey,
                    payload = networkPayload,
                    cachedAtEpochMillis = nowEpochMillis
                )
                networkPayload
            } else if (cachedEntry != null && canUseStale(cachedEntry, policy, nowEpochMillis)) {
                cachedEntry.payload
            } else {
                throw networkResult.exceptionOrNull()
                    ?: IllegalStateException("Firestore GET request failed without error: $path")
            }
        }
    }

    private suspend fun executePostWithPolicy(path: String, body: String, idToken: String?): String {
        val cacheKey = buildPostCacheKey(path, body)
        val policy = resolvePolicy(path)
        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
        val cachedEntry = readCacheEntry(cacheKey)
        return if (cachedEntry != null && isFresh(cachedEntry, policy, nowEpochMillis)) {
            cachedEntry.payload
        } else {
            val networkResult = runCatching {
                delegate.post(path, body, idToken)
            }
            val networkPayload = networkResult.getOrNull()

            if (networkPayload != null) {
                writeCacheEntry(
                    key = cacheKey,
                    payload = networkPayload,
                    cachedAtEpochMillis = nowEpochMillis
                )
                networkPayload
            } else if (cachedEntry != null && canUseStale(cachedEntry, policy, nowEpochMillis)) {
                cachedEntry.payload
            } else {
                throw networkResult.exceptionOrNull()
                    ?: IllegalStateException("Firestore POST request failed without error: $path")
            }
        }
    }

    private fun buildGetCacheKey(path: String): String {
        return "GET|$path"
    }

    private fun buildPostCacheKey(path: String, body: String): String {
        return "POST|$path|${body.hashCode()}"
    }

    override suspend fun get(path: String, idToken: String?): String {
        return executeGetWithPolicy(path = path, idToken = idToken)
    }

    override suspend fun post(path: String, body: String, idToken: String?): String {
        return executePostWithPolicy(path = path, body = body, idToken = idToken)
    }

    override suspend fun patch(path: String, body: String, idToken: String?): String {
        val response = delegate.patch(path, body, idToken)

        cacheStore.clear()
        return response
    }

    override suspend fun delete(path: String, idToken: String?) {
        delegate.delete(path, idToken)
        cacheStore.clear()
    }

    @Serializable
    private data class CacheEnvelope(
        val payload: String,
        val cachedAtEpochMillis: Long
    )

    private data class CacheEntry(
        val payload: String,
        val cachedAtEpochMillis: Long,
        val isLegacyPayload: Boolean
    )

    private data class CachePolicy(
        val softTtlMillis: Long,
        val maxStaleMillis: Long
    )

    private companion object {
        private val QUERY_POLICY = CachePolicy(
            softTtlMillis = 2 * 60 * 1000L,
            maxStaleMillis = 24 * 60 * 60 * 1000L
        )

        private val DOCUMENT_POLICY = CachePolicy(
            softTtlMillis = 30 * 1000L,
            maxStaleMillis = 10 * 60 * 1000L
        )
    }
}
