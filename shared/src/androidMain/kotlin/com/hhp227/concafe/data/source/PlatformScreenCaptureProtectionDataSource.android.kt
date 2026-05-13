package com.hhp227.concafe.data.source

import android.app.Activity
import android.view.WindowManager
import java.lang.ref.WeakReference

object AndroidScreenCaptureProtectionActivityHolder {
    private var currentActivityRef: WeakReference<Activity>? = null

    fun update(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    fun clear(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    internal fun currentActivity(): Activity? {
        return currentActivityRef?.get()
    }
}

actual fun createPlatformScreenCaptureProtectionDataSource(): ScreenCaptureProtectionDataSource {
    return AndroidScreenCaptureProtectionDataSource()
}

private class AndroidScreenCaptureProtectionDataSource : ScreenCaptureProtectionDataSource {
    override fun enable() {
        currentWindow()?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    override fun disable() {
        currentWindow()?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun currentWindow() = AndroidScreenCaptureProtectionActivityHolder
        .currentActivity()
        ?.window
}
