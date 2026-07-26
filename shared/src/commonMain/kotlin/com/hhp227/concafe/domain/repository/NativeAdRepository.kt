package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.data.model.NativeAdHandle

interface NativeAdRepository {
    suspend fun loadAd(slot: Int): NativeAdHandle?
    fun clear()
}
