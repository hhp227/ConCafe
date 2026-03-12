package com.hhp227.concafe.domain.model

data class CastRecentReview(
    val id: String,
    val userNickname: String,
    val rating: Float,
    val content: String,
    val taggedCastNames: List<String>,
    val createdDateLabel: String
)
