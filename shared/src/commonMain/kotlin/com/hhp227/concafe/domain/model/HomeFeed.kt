package com.hhp227.concafe.domain.model

data class HomePopularCastPage(
    val casts: List<Cast>,
    val cafeNames: Map<String, String>,
    val nextCursor: String?,
    val hasNext: Boolean
)

data class HomeCafeEvent(
    val id: String,
    val cafeId: String,
    val cafeName: String,
    val title: String,
    val content: String,
    val imageUrl: String,
    val periodText: String,
    val statusLabel: String
)
