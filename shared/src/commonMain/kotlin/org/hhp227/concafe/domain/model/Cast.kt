package org.hhp227.concafe.domain.model

data class Cast(
    val id: String,
    val cafeId: String,
    val name: String,
    val profileImage: String?,
    val desc: String,
    val birthday: String?,
    val conceptRole: String,
    val followerCount: Int
)
