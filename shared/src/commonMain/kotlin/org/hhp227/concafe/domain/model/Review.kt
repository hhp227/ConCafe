package org.hhp227.concafe.domain.model

data class Review(
    val id: String,
    val userId: String,
    val cafeId: String,
    val rating: Float,
    val content: String,
    val imageUrls: List<String>,
    val likeCount: Int,
    val createdAt: String
)
