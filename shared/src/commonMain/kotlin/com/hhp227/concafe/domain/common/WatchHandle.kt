package com.hhp227.concafe.domain.common

class WatchHandle(
    private val onCancel: () -> Unit
) {
    fun cancel() {
        onCancel()
    }
}
