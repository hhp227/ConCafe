package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ScreenCaptureProtectionDataSource
import com.hhp227.concafe.domain.repository.ScreenCaptureProtectionRepository

class ScreenCaptureProtectionRepositoryImpl(
    private val dataSource: ScreenCaptureProtectionDataSource
) : ScreenCaptureProtectionRepository {
    override fun enable() {
        dataSource.enable()
    }

    override fun disable() {
        dataSource.disable()
    }
}
