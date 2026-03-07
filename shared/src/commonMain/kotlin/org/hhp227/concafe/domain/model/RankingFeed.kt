package org.hhp227.concafe.domain.model

data class RankingFeed(
    val ads: List<RankingPromoAd>,
    val castRankings: List<RankingFeedEntry>,
    val cafeRankings: List<RankingFeedEntry>
)

data class RankingPromoAd(
    val id: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val detailText: String,
    val startColorHex: String,
    val endColorHex: String,
    val symbol: String
)

data class RankingFeedEntry(
    val id: String,
    val rank: Int,
    val name: String,
    val subtitle: String,
    val score: Int,
    val change: String,
    val startColorHex: String,
    val endColorHex: String,
    val symbol: String
)
