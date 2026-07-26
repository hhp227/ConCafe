package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.model.NativeAdHandle
import com.hhp227.concafe.data.source.NativeAdDataSource
import com.hhp227.concafe.domain.repository.NativeAdRepository

class NativeAdRepositoryImpl(
    private val dataSource: NativeAdDataSource
) : NativeAdRepository {
    private val cachedAds = mutableMapOf<Int, NativeAdHandle?>()

    override suspend fun loadAd(slot: Int): NativeAdHandle? {
        cachedAds[slot]?.let { return it }
        val loaded = dataSource.loadAd(slot)
        cachedAds[slot] = loaded
        return loaded
    }

    override fun clear() {
        cachedAds.values.forEach { it?.destroy() }
        cachedAds.clear()
    }
}
