package com.hhp227.concafe.domain.model

data class ExternalLink(
    val id: String,
    val platform: String,
    val title: String,
    val url: String,
    val isVisible: Boolean,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String
)
