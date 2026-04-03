package com.hhp227.concafe

import android.app.Application
import com.hhp227.concafe.di.androidPlatformModules
import com.hhp227.concafe.di.doInitConCafeAppKoin
import com.google.android.gms.ads.MobileAds
import com.kakao.sdk.common.KakaoSdk

class ConCafeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        KakaoSdk.init(this, getString(R.string.kakao_native_app_key))
        doInitConCafeAppKoin(androidPlatformModules(this))
    }
}
