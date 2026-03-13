package com.hhp227.concafe.domain.model

data class CafeNoticeManagementItem(
    val id: String,
    val cafeId: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val displayDate: String,
    val isPinned: Boolean,
    val statusLabel: String,
    val statusAccent: NoticeStatusAccent
)

enum class NoticeStatusAccent {
    PUBLISHED,
    DRAFT,
    ENDED
}
