package com.hhp227.concafe.data.source

interface ScreenCaptureProtectionDataSource {
    fun enable()
    fun disable()
}

expect fun createPlatformScreenCaptureProtectionDataSource(): ScreenCaptureProtectionDataSource
