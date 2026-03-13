package com.hhp227.concafe.domain.model

data class CafeNoticeCreate(
    val cafeId: String,
    val title: String,
    val content: String,
    val isPinned: Boolean,
    val reservedAt: String?
)
