package com.hhp227.concafe.domain.model

data class CafeEventCreate(
    val cafeId: String,
    val title: String,
    val content: String,
    val imageUrl: String,
    val periodText: String?
)
