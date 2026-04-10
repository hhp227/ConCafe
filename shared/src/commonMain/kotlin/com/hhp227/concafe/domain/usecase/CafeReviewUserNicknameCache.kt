package com.hhp227.concafe.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CafeReviewUserNicknameCache {
    private val mutex = Mutex()
    private val nicknameByCafeId = linkedMapOf<String, LinkedHashMap<String, String>>()

    suspend fun getNicknames(
        cafeId: String,
        userIds: Collection<String>
    ): Map<String, String> = mutex.withLock {
        if (userIds.isEmpty()) {
            return@withLock emptyMap()
        }
        val cache = nicknameByCafeId[cafeId] ?: return@withLock emptyMap()
        userIds.mapNotNull { userId ->
            val nickname = cache[userId] ?: return@mapNotNull null
            userId to nickname
        }.toMap()
    }

    suspend fun putAll(
        cafeId: String,
        nicknameByUserId: Map<String, String>
    ) = mutex.withLock {
        if (nicknameByUserId.isEmpty()) {
            return@withLock
        }
        val cache = nicknameByCafeId.getOrPut(cafeId) { linkedMapOf() }

        nicknameByUserId.forEach { (userId, nickname) ->
            val normalizedUserId = userId.trim()
            val normalizedNickname = nickname.trim()

            if (normalizedUserId.isEmpty() || normalizedNickname.isEmpty()) {
                return@forEach
            }
            cache[normalizedUserId] = normalizedNickname
        }
        trimCache(cache)
        trimCafeCache()
    }

    private fun trimCache(cache: LinkedHashMap<String, String>) {
        while (cache.size > MAX_USER_CACHE_PER_CAFE) {
            val oldestKey = cache.keys.firstOrNull() ?: break
            cache.remove(oldestKey)
        }
    }

    private fun trimCafeCache() {
        while (nicknameByCafeId.size > MAX_CAFE_CACHE_SIZE) {
            val oldestCafeId = nicknameByCafeId.keys.firstOrNull() ?: break
            nicknameByCafeId.remove(oldestCafeId)
        }
    }

    companion object {
        private const val MAX_USER_CACHE_PER_CAFE = 300
        private const val MAX_CAFE_CACHE_SIZE = 30
    }
}
