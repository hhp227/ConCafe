package com.hhp227.concafe.presentation.settings.notification

enum class NotificationQuietHoursOption(
    val title: String,
    val description: String
) {
    OFF("즉시 받기", "중요 알림을 포함해 들어오는 즉시 알려드려요."),
    NIGHT("밤 시간만 조용히", "밤 11시부터 오전 8시까지는 조용히 보관해요."),
    ALL_DAY("요약만 받기", "하루 동안 모아 저녁 시간에 한 번 정리해드려요.")
}
