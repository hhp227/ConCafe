package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.model.NativeAdHandle
import com.hhp227.concafe.data.source.NativeAdDataSource
import com.hhp227.concafe.domain.repository.NativeAdRepository

class NativeAdRepositoryImpl(
    private val dataSource: NativeAdDataSource
) : NativeAdRepository {
    private var cachedAd: NativeAdHandle? = null

    override suspend fun loadAd(): NativeAdHandle? {
        if (cachedAd != null) return cachedAd
        cachedAd = dataSource.loadAd()
        return cachedAd
    }

    override fun clear() {
        cachedAd?.destroy()
        cachedAd = null
    }
}