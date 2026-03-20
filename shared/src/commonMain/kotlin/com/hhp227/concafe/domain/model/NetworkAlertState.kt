package com.hhp227.concafe.domain.model

data class NetworkAlertState(
    val isVisible: Boolean,
    val isConnected: Boolean,
    val message: String
) {
    companion object {
        const val offlineMessage = "인터넷 연결이 원활하지 않습니다."
        const val onlineRecoveredMessage = "인터넷 연결이 복구되었습니다."

        val hidden = NetworkAlertState(
            isVisible = false,
            isConnected = true,
            message = ""
        )

        val offline = NetworkAlertState(
            isVisible = true,
            isConnected = false,
            message = offlineMessage
        )

        val recovered = NetworkAlertState(
            isVisible = true,
            isConnected = true,
            message = onlineRecoveredMessage
        )
    }
}
