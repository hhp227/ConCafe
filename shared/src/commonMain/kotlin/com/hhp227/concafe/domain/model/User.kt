package com.hhp227.concafe.domain.model

data class User(
    val id: String,
    val email: String,
    val nickname: String,
    val profileImage: String?,
    val authProvider: AuthProvider = AuthProvider.UNKNOWN,
    val role: UserRole,
    val banned: Boolean,
    val createdAt: String,
    val phoneNumber: String? = null,
    val signupCompleted: Boolean = true
)
