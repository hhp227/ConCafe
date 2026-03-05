package org.hhp227.concafe.domain.model

data class HomePopularCast(
    val id: String,
    val name: String,
    val cafeName: String,
    val followers: Int,
    val imageUrl: String?
)
