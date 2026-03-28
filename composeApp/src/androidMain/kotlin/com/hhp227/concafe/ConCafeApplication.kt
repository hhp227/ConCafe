package com.hhp227.concafe

import android.app.Application
import com.hhp227.concafe.di.androidPlatformModules
import com.hhp227.concafe.di.doInitConCafeAppKoin

class ConCafeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        doInitConCafeAppKoin(androidPlatformModules(this))
    }
}
