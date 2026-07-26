package com.hhp227.concafe.domain.model

data class CafeDetailReview(
    val id: String,
    val userId: String,
    val userNickname: String,
    val rating: Float,
    val content: String,
    val taggedCastNames: List<String>,
    val likeCount: Int,
    val createdDate: String,
    val verified: Boolean,
    val imageUrls: List<String> = emptyList()
)
