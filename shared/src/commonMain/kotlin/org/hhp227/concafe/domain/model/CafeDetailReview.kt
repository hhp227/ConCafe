package com.hhp227.concafe.domain.model

data class CafeDetailReview(
    val id: String,
    val userNickname: String,
    val rating: Float,
    val content: String,
    val likeCount: Int,
    val createdDate: String,
    val verified: Boolean
)
