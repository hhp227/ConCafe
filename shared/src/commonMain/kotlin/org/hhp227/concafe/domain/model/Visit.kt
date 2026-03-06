package org.hhp227.concafe.domain.model

data class Visit(
    val id: String,
    val userId: String,
    val cafeId: String,
    val visitedAt: String,
    val memo: String?,
    val verified: Boolean
)
