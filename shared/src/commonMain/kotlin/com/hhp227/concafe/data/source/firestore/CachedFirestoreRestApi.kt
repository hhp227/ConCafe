package com.hhp227.concafe.data.source.firestore

class CachedFirestoreRestApi(
    private val delegate: FirestoreRestApi,
    private val cacheStore: FirestoreRestCacheStore
) : FirestoreRestApi {
    override suspend fun get(path: String, idToken: String?): String {
        val cacheKey = buildGetCacheKey(path)
        val cachedPayload = cacheStore.load(cacheKey)

        if (!cachedPayload.isNullOrEmpty()) {
            return cachedPayload
        }
        val response = delegate.get(path, idToken)

        cacheStore.save(cacheKey, response)
        return response
    }

    override suspend fun post(path: String, body: String, idToken: String?): String {
        val cacheKey = buildPostCacheKey(path, body)
        val cachedPayload = cacheStore.load(cacheKey)

        if (!cachedPayload.isNullOrEmpty()) {
            return cachedPayload
        }
        val response = delegate.post(path, body, idToken)

        cacheStore.save(cacheKey, response)
        return response
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

    private fun buildGetCacheKey(path: String): String {
        return "GET|$path"
    }

    private fun buildPostCacheKey(path: String, body: String): String {
        return "POST|$path|${body.hashCode()}"
    }
}
