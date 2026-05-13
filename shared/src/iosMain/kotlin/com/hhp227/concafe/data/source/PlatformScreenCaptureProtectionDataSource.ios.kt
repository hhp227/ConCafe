package com.hhp227.concafe.data.source

actual fun createPlatformScreenCaptureProtectionDataSource(): ScreenCaptureProtectionDataSource {
    return IosScreenCaptureProtectionDataSource()
}

private class IosScreenCaptureProtectionDataSource : ScreenCaptureProtectionDataSource {
    override fun enable() = Unit

    override fun disable() = Unit
}
