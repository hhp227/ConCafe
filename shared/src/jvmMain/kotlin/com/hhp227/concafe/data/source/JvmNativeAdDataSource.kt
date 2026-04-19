package com.hhp227.concafe.data.source

import com.hhp227.concafe.data.model.NativeAdHandle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class JvmNativeAdDataSource : NativeAdDataSource {
    override suspend fun loadAd(slot: Int): NativeAdHandle? = suspendCancellableCoroutine { cont ->
        cont.resume(null)
    }
}
