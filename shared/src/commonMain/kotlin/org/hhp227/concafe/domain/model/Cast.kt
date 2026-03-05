package org.hhp227.concafe.domain.model

data class Cast(
    val id: String,
    val cafeId: String,
    val name: String,
    val profileImage: String?,
    val description: String,
    val birthday: String?,
    val conceptRole: String,
    val followerCount: Int
)
