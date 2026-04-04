package com.hhp227.concafe.data.source

import com.hhp227.concafe.data.model.IosNativeAdHandle
import com.hhp227.concafe.data.model.NativeAdHandle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import platform.Foundation.NSObject
import platform.Foundation.NSError

import platform.GoogleMobileAds.GADAdLoader
import platform.GoogleMobileAds.GADAdLoaderDelegateProtocol
import platform.GoogleMobileAds.GADNativeAd
import platform.GoogleMobileAds.GADNativeAdLoaderDelegateProtocol
import platform.GoogleMobileAds.GADAdLoaderAdTypeNative
import platform.GoogleMobileAds.GADRequest

@OptIn(ExperimentalForeignApi::class)
class IosNativeAdDataSource : NativeAdDataSource {
    override suspend fun loadAd(): NativeAdHandle? =
        suspendCancellableCoroutine { cont ->
            val adUnitId = RANKING_NATIVE_TEST_AD_UNIT_ID // TODO: release 시 변경
            val adLoader = GADAdLoader(
                adUnitID = adUnitId,
                rootViewController = null,
                adTypes = listOf(GADAdLoaderAdTypeNative),
                options = null
            )
            val delegate = object : NSObject(), GADNativeAdLoaderDelegateProtocol,
                GADAdLoaderDelegateProtocol {
                override fun adLoader(
                    adLoader: GADAdLoader,
                    didReceiveNativeAd: GADNativeAd
                ) {
                    cont.resume(IosNativeAdHandle(didReceiveNativeAd))
                }

                override fun adLoader(
                    adLoader: GADAdLoader,
                    didFailToReceiveAdWithError: NSError
                ) {
                    cont.resume(null)
                }
            }
            adLoader.delegate = delegate

            adLoader.loadRequest(GADRequest())
        }

    companion object {
        private const val RANKING_NATIVE_AD_UNIT_ID =
            "ca-app-pub-6216021268300256/5283160617"

        private const val RANKING_NATIVE_TEST_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/3986624511" // iOS test id
    }
}