package com.hhp227.concafe.data.source

import com.hhp227.concafe.data.model.NativeAdHandle

interface NativeAdDataSource {
    suspend fun loadAd(slot: Int): NativeAdHandle?
}
