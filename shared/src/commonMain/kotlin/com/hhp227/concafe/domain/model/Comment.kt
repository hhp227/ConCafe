package com.hhp227.concafe.domain.model

data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val userNickname: String,
    val content: String,
    val createdAt: String,
    val displayDate: String
)
