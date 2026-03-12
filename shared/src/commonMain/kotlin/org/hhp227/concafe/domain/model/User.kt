package org.hhp227.concafe.domain.model

data class User(
    val id: String,
    val email: String,
    val nickname: String,
    val profileImage: String?,
    val role: UserRole,
    val banned: Boolean,
    val createdAt: String
)
