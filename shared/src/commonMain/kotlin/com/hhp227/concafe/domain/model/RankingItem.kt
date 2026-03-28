package com.hhp227.concafe.domain.model

data class RankingItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val score: Int,
    val rank: Int,
    val change: String,
    val imageUrl: String?
)
