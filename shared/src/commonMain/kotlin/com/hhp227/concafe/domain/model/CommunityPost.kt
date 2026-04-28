package com.hhp227.concafe.domain.model

data class CommunityPost(
    val id: String,
    val userId: String,
    val userNickname: String,
    val title: String,
    val content: String,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: String,
    val displayDate: String
)
