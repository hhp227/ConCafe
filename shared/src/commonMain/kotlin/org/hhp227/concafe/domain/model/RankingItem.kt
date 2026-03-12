package com.hhp227.concafe.domain.model

data class RankingItem(
    val id: String,
    val name: String,
    val score: Int,
    val rank: Int,
    val imageUrl: String?
)
