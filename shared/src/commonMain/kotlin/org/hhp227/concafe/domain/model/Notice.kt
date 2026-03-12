package com.hhp227.concafe.domain.model

data class Notice(
    val id: String,
    val cafeId: String,
    val cafeName: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val relativeTime: String
)
