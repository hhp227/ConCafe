package com.hhp227.concafe.data.source.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface RemoteMemoryCache {
    suspend fun get(key: String): Any?
    suspend fun put(key: String, value: Any)
    suspend fun remove(key: String)
    suspend fun removeByPrefix(prefix: String)
    suspend fun clear()
}

class DefaultRemoteMemoryCache : RemoteMemoryCache {
    private val mutex = Mutex()

    private val values = mutableMapOf<String, Any>()

    override suspend fun get(key: String): Any? = mutex.withLock {
        values[key]
    }

    override suspend fun put(key: String, value: Any) {
        mutex.withLock {
            values[key] = value
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            values.remove(key)
        }
    }

    override suspend fun removeByPrefix(prefix: String) {
        mutex.withLock {
            val targetKeys = values.keys.filter { it.startsWith(prefix) }
            targetKeys.forEach { values.remove(it) }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            values.clear()
        }
    }
}

internal object CachedRemoteNull

suspend fun <T : Any> RemoteMemoryCache.cacheFirst(
    key: String,
    loadRemote: suspend () -> T
): T {
    @Suppress("UNCHECKED_CAST")
    val cachedValue = get(key) as? T
    if (cachedValue != null) return cachedValue

    val remoteValue = loadRemote()
    put(key, remoteValue)
    return remoteValue
}

fun cacheKey(domain: String, operation: String, vararg parameters: Pair<String, Any?>): String {
    val suffix = parameters.joinToString(separator = "|") { (name, value) ->
        "$name=${value.toCacheKeyValue()}"
    }
    return if (suffix.isEmpty()) "$domain.$operation" else "$domain.$operation|$suffix"
}

private fun Any?.toCacheKeyValue(): String = when (this) {
    null -> "<null>"
    is Iterable<*> -> joinToString(prefix = "[", postfix = "]") { it.toCacheKeyValue() }
    else -> toString()
}
