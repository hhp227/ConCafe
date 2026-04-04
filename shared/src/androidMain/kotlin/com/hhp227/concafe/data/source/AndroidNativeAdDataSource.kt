package com.hhp227.concafe.data.source

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.hhp227.concafe.data.model.AndroidNativeAdHandle
import com.hhp227.concafe.data.model.NativeAdHandle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidNativeAdDataSource(
    private val context: Context
) : NativeAdDataSource {
    override suspend fun loadAd(): NativeAdHandle? =
        suspendCancellableCoroutine { cont ->
            val loader = AdLoader.Builder(context, if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                RANKING_NATIVE_TEST_AD_UNIT_ID
            } else {
                RANKING_NATIVE_AD_UNIT_ID
            })
                .forNativeAd { ad ->
                    cont.resume(AndroidNativeAdHandle(ad))
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        cont.resume(null)
                    }
                })
                .build()

            loader.loadAd(AdRequest.Builder().build())
        }

    companion object {
        private const val RANKING_NATIVE_AD_UNIT_ID = "ca-app-pub-6216021268300256/6596242282"

        private const val RANKING_NATIVE_TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    }
}