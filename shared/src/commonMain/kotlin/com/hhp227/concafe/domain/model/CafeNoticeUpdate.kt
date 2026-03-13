package com.hhp227.concafe.domain.model

data class CafeNoticeUpdate(
    val cafeId: String,
    val noticeId: String,
    val title: String,
    val content: String,
    val isPinned: Boolean,
    val reservedAt: String? = null
)
