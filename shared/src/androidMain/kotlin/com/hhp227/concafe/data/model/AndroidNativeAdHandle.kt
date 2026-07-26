package com.hhp227.concafe.data.model

import com.google.android.gms.ads.nativead.NativeAd

class AndroidNativeAdHandle(
    val nativeAd: NativeAd
) : NativeAdHandle {
    override fun destroy() {
        nativeAd.destroy()
    }
}