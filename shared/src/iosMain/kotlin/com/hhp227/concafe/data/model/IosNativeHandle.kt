package com.hhp227.concafe.data.model

import platform.GoogleMobileAds.GADNativeAd
import platform.GoogleMobileAds.GADAdLoader
import platform.GoogleMobileAds.GADRequest

class IosNativeAdHandle(
    val nativeAd: GADNativeAd
) : NativeAdHandle {
    override fun destroy() {
        nativeAd.unregisterAdView()
    }
}