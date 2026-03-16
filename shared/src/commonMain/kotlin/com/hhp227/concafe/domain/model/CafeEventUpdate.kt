package com.hhp227.concafe.domain.model

data class CafeEventUpdate(
    val cafeId: String,
    val eventId: String,
    val title: String,
    val content: String,
    val imageUrl: String,
    val periodText: String? = null
)
